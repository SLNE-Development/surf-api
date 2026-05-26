import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.core")
}

dependencies {
    api(projects.surfRedisCore)
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["shadow"])
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
        }
    }

    repositories {
        slneReleases()
    }
}