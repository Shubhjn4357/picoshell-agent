import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
    }
}

android {
    namespace = "com.picoshell.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("PicoShellDatabase") {
            packageName.set("com.picoshell.data.local.db")
        }
    }
}

