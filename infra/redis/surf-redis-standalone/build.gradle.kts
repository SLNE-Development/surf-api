import dev.slne.surf.api.gradle.util.slneReleases
import org.gradle.api.component.AdhocComponentWithVariants

plugins {
    id("dev.slne.surf.api.gradle.core")
}

dependencies {
    api(projects.surfRedisCore)
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