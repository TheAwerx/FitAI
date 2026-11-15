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
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.testJunit)
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

android {
    namespace = "com.fitai.app.core.database"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("FitAiDatabase") {
            packageName.set("com.fitai.app.db")
            srcDirs("src/commonMain/sqldelight")
            // Schema snapshots (`<version>.db`). After adding `migrations/<n>.sqm`, run
            // generateCommonMainFitAiDatabaseSchema and commit the new snapshot. Migrations are verified by
            // DatabaseMigrationTest (old snapshot + migrate == fresh schema, rows kept); SQLDelight's own
            // verifyMigrations task diffs the whole catalog and runs for many minutes on this schema.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
