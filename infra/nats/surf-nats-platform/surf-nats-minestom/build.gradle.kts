import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.minestom")
}

minestomPluginFile {
    main = "dev.slne.surf.nats.NatsMinestomPlugin"
}

dependencies {
    api(projects.surfApiNats.surfApiNatsCore)
}

gradlePlugin {
    isAutomatedPublishing = false
}

val shadowComponent = components["shadow"] as AdhocComponentWithVariants
shadowComponent.addVariantsFromConfiguration(configurations["sourcesElements"]) {}
shadowComponent.addVariantsFromConfiguration(configurations["javadocElements"]) {}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["shadow"])
        }
    }

    repositories {
        slneReleases()
    }
}
