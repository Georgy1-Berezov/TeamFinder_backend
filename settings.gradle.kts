rootProject.name = "TeamFinder"
include(":composeApp", ":shared")
include(":backend")
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