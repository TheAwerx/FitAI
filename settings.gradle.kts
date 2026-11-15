rootProject.name = "FitAI"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":composeApp")

include(":core:domain")
include(":core:database")
include(":core:data")
include(":core:design-system")
include(":core:presentation")

include(":feature:auth:domain")
include(":feature:auth:data")
include(":feature:auth:presentation")

include(":feature:onboarding:domain")
include(":feature:onboarding:data")
include(":feature:onboarding:presentation")

include(":feature:home:domain")
include(":feature:home:data")
include(":feature:home:presentation")

include(":feature:workout:domain")
include(":feature:workout:data")
include(":feature:workout:presentation")

include(":feature:nutrition:domain")
include(":feature:nutrition:data")
include(":feature:nutrition:presentation")

include(":feature:profile:domain")
include(":feature:profile:data")
include(":feature:profile:presentation")

include(":feature:coach:domain")
include(":feature:coach:data")
include(":feature:coach:presentation")
