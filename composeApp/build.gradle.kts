import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val localSigningPropertiesFile = rootProject.file("signing/release-signing.properties")
val localSigningProperties = Properties().apply {
    if (localSigningPropertiesFile.exists()) {
        localSigningPropertiesFile.inputStream().use(::load)
    }
}

fun configuredSigningProperty(name: String): String? {
    return providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: localSigningProperties.getProperty(name)?.takeIf { it.isNotBlank() }
}

fun resolveSigningFile(path: String): File {
    val candidate = File(path)
    return if (candidate.isAbsolute) candidate else rootProject.file(path)
}

val releaseStoreFilePath = configuredSigningProperty("RELEASE_STORE_FILE")
val releaseStorePassword = configuredSigningProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = configuredSigningProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = configuredSigningProperty("RELEASE_KEY_PASSWORD")

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":ui"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }

        androidMain.dependencies {
            implementation(project(":core"))
            implementation(project(":domain"))
            implementation(project(":data"))
            implementation(project(":services"))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
        }
    }
}

android {
    namespace = "com.picoshell.agent"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.picoshell.agent"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (releaseStoreFilePath != null) {
            create("release") {
                storeFile = resolveSigningFile(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
