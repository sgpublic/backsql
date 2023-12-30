pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    // https://docs.gradle.org/current/userguide/platforms.html#sec:importing-catalog-from-file
    versionCatalogs {
        val backsql by creating {
            from(files(File(rootDir, "./gradle/backsql.versions.toml")))
        }
    }
}

rootProject.name = "backsql"

include(":backsql")
