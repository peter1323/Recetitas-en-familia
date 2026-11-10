plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.recetitas"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.recetitas"
        minSdk = 24
        targetSdk = 37
        versionCode = providers.gradleProperty("recetitas.versionCode").get().toInt()
        versionName = providers.gradleProperty("recetitas.version").get()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Firmada con la clave de depuración para poder instalar el .apk directamente
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
}
