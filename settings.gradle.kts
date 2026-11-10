rootProject.name = "RecetitasEnFamilia"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// composeApp: todo el código compartido + la app de Windows
// androidApp: solo la Activity que arranca la app en Android
include(":composeApp", ":androidApp")
