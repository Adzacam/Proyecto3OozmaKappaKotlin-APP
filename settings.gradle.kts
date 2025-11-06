pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Corregir el formato del repositorio Maven
        maven {
            setUrl("https://jitpack.io")
        }
    }
}

rootProject.name = "DevelArqApp"
include(":app")