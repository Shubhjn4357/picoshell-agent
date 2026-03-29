package com.picoshell.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.picoshell.data.local.SeedCatalog
import com.picoshell.data.local.db.PicoShellDatabase
import com.picoshell.domain.model.InstallState
import com.picoshell.domain.model.ModelCard
import com.picoshell.domain.model.ModelCatalogItem
import com.picoshell.domain.model.ModelInstallation
import com.picoshell.domain.model.ModelOrigin
import com.picoshell.domain.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModelRepositoryImpl(
    database: PicoShellDatabase,
) : ModelRepository {
    private val queries = database.catalogStorageQueries
    private val seedModelsById = SeedCatalog.models.associateBy(ModelCatalogItem::id)

    override fun observeModels(): Flow<List<ModelCard>> {
        return queries
            .allStoredModels(::mapStoredModel)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { storedModels ->
                val storedById = storedModels.associateBy { it.catalog.id }
                val importedModels = storedModels.filterNot { it.catalog.id in seedModelsById }

                buildList {
                    addAll(
                        SeedCatalog.models.map { model ->
                            val stored = storedById[model.id]
                            ModelCard(
                                catalog = model,
                                installation = stored?.installation,
                            )
                        },
                    )
                    addAll(importedModels.sortedBy { it.catalog.name.lowercase() })
                }
            }
    }

    override suspend fun getModel(modelId: String): ModelCard? {
        val storedModel = queries
            .storedModelById(modelId, ::mapStoredModel)
            .executeAsOneOrNull()

        val seedModel = seedModelsById[modelId]

        return when {
            seedModel != null -> ModelCard(
                catalog = seedModel,
                installation = storedModel?.installation,
            )
            storedModel != null -> storedModel
            else -> null
        }
    }

    override suspend fun upsertModel(model: ModelCard) {
        val installation = model.installation ?: ModelInstallation(
            modelId = model.catalog.id,
            installState = InstallState.Missing,
        )

        queries.upsertModelInstallation(
            model_id = model.catalog.id,
            name = model.catalog.name,
            format = model.catalog.format,
            source_url = model.catalog.sourceUrl,
            category = model.catalog.category,
            status = installation.installState.name,
            progress = installation.progress.toDouble(),
            local_path = installation.localPath,
            notes = installation.notes,
        )
    }

    override suspend fun upsertInstallation(installation: ModelInstallation) {
        val catalog = seedModelsById[installation.modelId]
            ?: queries.storedModelById(installation.modelId, ::mapStoredModel)
                .executeAsOneOrNull()
                ?.catalog
            ?: return

        queries.upsertModelInstallation(
            model_id = installation.modelId,
            name = catalog.name,
            format = catalog.format,
            source_url = catalog.sourceUrl,
            category = catalog.category,
            status = installation.installState.name,
            progress = installation.progress.toDouble(),
            local_path = installation.localPath,
            notes = installation.notes,
        )
    }

    private fun mapStoredModel(
        model_id: String,
        name: String,
        format: String,
        source_url: String,
        category: String,
        status: String,
        progress: Double,
        local_path: String?,
        notes: String?,
    ): ModelCard {
        val isSeedModel = model_id in seedModelsById
        return ModelCard(
            catalog = seedModelsById[model_id] ?: ModelCatalogItem(
                id = model_id,
                name = name,
                format = format,
                category = category,
                sourceUrl = source_url,
                summary = "Imported from local storage.",
                origin = if (isSeedModel) ModelOrigin.Catalog else ModelOrigin.Imported,
            ),
            installation = ModelInstallation(
                modelId = model_id,
                installState = runCatching { InstallState.valueOf(status) }.getOrDefault(InstallState.Missing),
                progress = progress.toFloat(),
                localPath = local_path,
                notes = notes,
            ),
        )
    }
}
