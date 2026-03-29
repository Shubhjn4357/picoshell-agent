package com.picoshell.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.picoshell.core.toSlug
import com.picoshell.domain.model.InstallState
import com.picoshell.domain.model.ModelCard
import com.picoshell.domain.model.ModelCatalogItem
import com.picoshell.domain.model.ModelInstallation
import com.picoshell.domain.model.ModelOrigin
import com.picoshell.domain.repository.ModelRepository
import com.picoshell.services.download.ModelDownloadManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.math.ln
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AndroidModelDownloadManager(
    private val context: Context,
    private val client: HttpClient,
    private val modelRepository: ModelRepository,
) : ModelDownloadManager {
    override suspend fun stageModel(modelId: String): Result<ModelInstallation> = runCatching {
        val model = modelRepository.getModel(modelId)
            ?: error("Unknown model id: $modelId")

        modelRepository.upsertInstallation(
            ModelInstallation(
                modelId = modelId,
                installState = InstallState.Syncing,
                progress = 0.1f,
                notes = "Preparing model asset.",
            ),
        )

        val resolved = resolveRemoteSource(
            sourceUrl = model.catalog.sourceUrl,
            fallbackName = model.catalog.name,
        )

        val installation = when (resolved) {
            is ResolvedRemoteSource.DirectArtifact -> {
                downloadArtifact(
                    model = model,
                    artifactUrl = resolved.url,
                    requestedName = resolved.fileName,
                    notePrefix = resolved.note,
                )
            }
            is ResolvedRemoteSource.UnresolvedCatalog -> {
                stageCatalogReference(model, resolved.note)
            }
        }

        modelRepository.upsertInstallation(installation)
        installation
    }

    override suspend fun importLocalModel(uriString: String): Result<ModelCard> = runCatching {
        val uri = Uri.parse(uriString)
        takeReadPermission(uri)

        val metadata = resolveMetadata(uri)
        require(metadata.displayName.endsWith(".gguf", ignoreCase = true)) {
            "Only .gguf model files are supported for local import."
        }

        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.read()
            } ?: error("Unable to read the selected file.")
        }

        val modelId = "local-${metadata.displayName.toSlug()}-${System.currentTimeMillis().toString().takeLast(6)}"
        val model = ModelCard(
            catalog = ModelCatalogItem(
                id = modelId,
                name = metadata.displayName.removeSuffix(".gguf"),
                format = "GGUF",
                category = "Text",
                sourceUrl = uriString,
                summary = "Linked from Android document storage without duplicating the underlying model file.",
                origin = ModelOrigin.Imported,
            ),
            installation = ModelInstallation(
                modelId = modelId,
                installState = InstallState.Ready,
                progress = 1f,
                localPath = uriString,
                notes = buildString {
                    append("Linked local GGUF in place through Android document storage")
                    metadata.sizeBytes?.let {
                        append(" (")
                        append(it.toReadableSize())
                        append(")")
                    }
                    append(". No duplicate app-side copy was created.")
                },
            ),
        )

        modelRepository.upsertModel(model)
        model
    }

    override suspend fun downloadModelFromUrl(url: String): Result<ModelCard> = runCatching {
        val normalizedUrl = url.trim()
        require(normalizedUrl.isNotBlank()) {
            "Paste a model URL before starting the download."
        }

        val resolved = when (
            val source = resolveRemoteSource(
                sourceUrl = normalizedUrl,
                fallbackName = "downloaded-model",
            )
        ) {
            is ResolvedRemoteSource.DirectArtifact -> source
            is ResolvedRemoteSource.UnresolvedCatalog -> {
                error(source.note.ifBlank { "Unable to resolve the pasted link into a GGUF artifact." })
            }
        }

        val modelName = resolved.fileName.removeSuffix(".gguf")
        val modelId = "remote-${modelName.toSlug()}-${System.currentTimeMillis().toString().takeLast(6)}"

        val model = ModelCard(
            catalog = ModelCatalogItem(
                id = modelId,
                name = modelName,
                format = "GGUF",
                category = "Text",
                sourceUrl = resolved.url,
                summary = "Downloaded from a pasted model link.",
                origin = ModelOrigin.Imported,
            ),
            installation = ModelInstallation(
                modelId = modelId,
                installState = InstallState.Syncing,
                progress = 0.1f,
                notes = resolved.note,
            ),
        )

        modelRepository.upsertModel(model)

        val installation = downloadArtifact(
            model = model,
            artifactUrl = resolved.url,
            requestedName = resolved.fileName,
            notePrefix = resolved.note,
        )
        val completedModel = model.copy(installation = installation)
        modelRepository.upsertModel(completedModel)
        completedModel
    }

    private suspend fun resolveRemoteSource(
        sourceUrl: String,
        fallbackName: String,
    ): ResolvedRemoteSource {
        val normalizedUrl = sourceUrl.trim()

        if (normalizedUrl.isDirectArtifactUrl()) {
            return ResolvedRemoteSource.DirectArtifact(
                url = normalizedUrl.normalizeHuggingFaceFileUrl(),
                fileName = normalizedUrl.fileNameFromUrl().ifBlank { "${fallbackName.toSlug()}.gguf" },
                note = "Downloading GGUF artifact from direct URL.",
            )
        }

        val repoId = normalizedUrl.huggingFaceRepoIdOrNull()
            ?: return ResolvedRemoteSource.UnresolvedCatalog(
                "The link is not a direct GGUF URL and could not be resolved as a Hugging Face model repository.",
            )

        val siblings = runCatching { fetchHuggingFaceSiblings(repoId) }
            .getOrElse { throwable ->
                return ResolvedRemoteSource.UnresolvedCatalog(
                    throwable.message?.let {
                        "Unable to inspect the Hugging Face repository `$repoId`: $it"
                    } ?: "Unable to inspect the Hugging Face repository `$repoId`.",
                )
            }

        val ggufSiblings = siblings.filter { it.endsWith(".gguf", ignoreCase = true) }
        val ggufPath = ggufSiblings
            .maxByOrNull(::scoreGgufCandidate)
            ?: return ResolvedRemoteSource.UnresolvedCatalog(
                "No GGUF files were found in the Hugging Face repository `$repoId`.",
            )

        return ResolvedRemoteSource.DirectArtifact(
            url = "https://huggingface.co/$repoId/resolve/main/$ggufPath",
            fileName = ggufPath.substringAfterLast('/'),
            note = if (ggufSiblings.size > 1) {
                "Resolved repository link to the preferred GGUF artifact `$ggufPath`."
            } else {
                "Resolved repository link to the repository's GGUF artifact."
            },
        )
    }

    private suspend fun fetchHuggingFaceSiblings(repoId: String): List<String> {
        val body = client
            .get("https://huggingface.co/api/models/$repoId")
            .body<String>()

        val siblings = JSONObject(body).optJSONArray("siblings") ?: return emptyList()
        return buildList {
            for (index in 0 until siblings.length()) {
                val sibling = siblings.optJSONObject(index) ?: continue
                sibling.optString("rfilename")
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }

    private suspend fun downloadArtifact(
        model: ModelCard,
        artifactUrl: String,
        requestedName: String,
        notePrefix: String,
    ): ModelInstallation = withContext(Dispatchers.IO) {
        val targetFile = uniqueFile(
            directory = File(context.filesDir, "models/downloaded").apply { mkdirs() },
            requestedName = requestedName.ifBlank {
                "${model.catalog.name.toSlug()}.gguf"
            },
        )

        URL(artifactUrl).openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 120_000
            getInputStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        ModelInstallation(
            modelId = model.catalog.id,
            installState = InstallState.Ready,
            progress = 1f,
            localPath = targetFile.absolutePath,
            notes = "$notePrefix Stored in app-managed storage.",
        )
    }

    private suspend fun stageCatalogReference(
        model: ModelCard,
        note: String,
    ): ModelInstallation {
        val response = client.get(model.catalog.sourceUrl)
        val manifestFile = withContext(Dispatchers.IO) {
            val modelsDir = File(context.filesDir, "models/catalog").apply { mkdirs() }
            val manifest = JSONObject()
                .put("name", model.catalog.name)
                .put("format", model.catalog.format)
                .put("category", model.catalog.category)
                .put("sourceUrl", model.catalog.sourceUrl)
                .put("status", response.status.value)
                .put("note", note)

            File(modelsDir, "${model.catalog.name.toSlug()}.manifest.json").apply {
                writeText(manifest.toString(2))
            }
        }

        return ModelInstallation(
            modelId = model.catalog.id,
            installState = if (response.status.value in 200..399) InstallState.Staged else InstallState.Error,
            progress = if (response.status.value in 200..399) 1f else 0f,
            localPath = manifestFile.absolutePath,
            notes = if (response.status.value in 200..399) {
                note
            } else {
                "Catalog verification failed with status ${response.status.value}."
            },
        )
    }

    private fun resolveMetadata(uri: Uri): ImportedModelMetadata {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                val displayName = if (displayNameColumn >= 0) cursor.getString(displayNameColumn) else null
                val sizeBytes = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else null
                if (!displayName.isNullOrBlank()) {
                    return ImportedModelMetadata(displayName, sizeBytes)
                }
            }
        }

        val fallbackName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported-model.gguf"
        return ImportedModelMetadata(fallbackName, null)
    }

    private fun takeReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private fun uniqueFile(
        directory: File,
        requestedName: String,
    ): File {
        val safeName = requestedName.ifBlank { "model.gguf" }
        val baseName = safeName.substringBeforeLast('.', safeName)
        val extension = safeName.substringAfterLast('.', "")

        var candidate = File(directory, safeName)
        var suffix = 1
        while (candidate.exists()) {
            val numberedName = buildString {
                append(baseName)
                append("-")
                append(suffix)
                if (extension.isNotBlank()) {
                    append(".")
                    append(extension)
                }
            }
            candidate = File(directory, numberedName)
            suffix += 1
        }

        return candidate
    }
}

private sealed interface ResolvedRemoteSource {
    data class DirectArtifact(
        val url: String,
        val fileName: String,
        val note: String,
    ) : ResolvedRemoteSource

    data class UnresolvedCatalog(
        val note: String,
    ) : ResolvedRemoteSource
}

private data class ImportedModelMetadata(
    val displayName: String,
    val sizeBytes: Long?,
)

private fun String.isDirectArtifactUrl(): Boolean {
    val normalized = lowercase()
    return normalized.endsWith(".gguf") ||
        normalized.contains(".gguf?") ||
        ("/resolve/" in normalized && ".gguf" in normalized) ||
        ("/blob/" in normalized && ".gguf" in normalized)
}

private fun String.normalizeHuggingFaceFileUrl(): String {
    return replace("/blob/", "/resolve/")
}

private fun String.huggingFaceRepoIdOrNull(): String? {
    val normalized = trim().removeSuffix("/")
    val regex = Regex("""https?://huggingface\.co/(?:models/)?([^/\s]+)/([^/\s?#]+)""")
    val match = regex.find(normalized) ?: return null
    val namespace = match.groupValues[1]
    if (namespace in setOf("datasets", "spaces")) {
        return null
    }
    return "$namespace/${match.groupValues[2]}"
}

private fun String.fileNameFromUrl(): String {
    return substringAfterLast('/').substringBefore('?').ifBlank { "model.gguf" }
}

private fun scoreGgufCandidate(path: String): Int {
    val normalized = path.lowercase()
    return when {
        "q4_k_m" in normalized -> 100
        "q4_k_s" in normalized -> 95
        "q5_k_m" in normalized -> 90
        "q5_k_s" in normalized -> 85
        "q6_k" in normalized -> 80
        "q8_0" in normalized -> 70
        "f16" in normalized -> 60
        else -> 10
    }
}

private fun Long.toReadableSize(): String {
    if (this < 1024) return "$this B"
    val unitIndex = (ln(toDouble()) / ln(1024.0)).toInt().coerceAtMost(4)
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val size = this / 1024.0.pow(unitIndex.toDouble())
    return String.format("%.1f %s", size, units[unitIndex])
}
