import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.recetitas.shared"
        compileSdk = 37
        minSdk = 24
    }

    jvm("desktop")

    compilerOptions {
        optIn.addAll("kotlin.uuid.ExperimentalUuidApi", "kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.icons)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io)
            implementation(libs.filekit.dialogs)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.recetitas.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            // Sin jdk.accessibility la app no arranca en Windows con la accesibilidad de Java activada
            modules("jdk.accessibility", "jdk.unsupported")
            packageName = "Recetitas en familia"
            packageVersion = "1.0.0"
            description = "Recetario familiar"
            vendor = "peter1323"
            // Doble clic en un archivo .receta lo abre con la app
            fileAssociation("application/x-recetitas", "receta", "Receta de Recetitas en familia")
            windows {
                iconFile.set(project.file("icono.ico"))
                menu = true
                shortcut = true
                dirChooser = true
                upgradeUuid = "8f2d6a41-3c7e-4b19-a5d2-6e0f9c3b7a14"
            }
        }
    }
}
