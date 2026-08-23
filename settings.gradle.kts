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
    }
}

rootProject.name = "Yonte"
include(":app")
include(":core:database")
include(":core:security")
include(":core:backup")
include(":core:navigation")
include(":core:designsystem")
include(":feature:notes")
include(":feature:settings")

include(":core:update")
