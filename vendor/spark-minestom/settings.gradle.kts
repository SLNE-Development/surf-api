pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.lucko.me/")
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            name = "minestom-snapshots"
            mavenContent {
                snapshotsOnly()
                includeGroup("net.minestom")
            }
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "spark-minestom"
