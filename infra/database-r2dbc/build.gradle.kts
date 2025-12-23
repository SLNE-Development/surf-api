import dev.slne.surf.surfapi.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.surfapi.gradle.core") version "1.21.11+"
//    id("dev.slne.surf.surfapi.gradle.standalone") version "1.21.11+" /* Uncomment to use tests */
}

group = "dev.slne"
version = findProperty("version") as String

dependencies {

}

shadow {
    addShadowVariantIntoJavaComponent = false
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    shadowJar {
        relocationPrefix = "dev.slne.database.libs"
        enableAutoRelocation = true
    }

    test {
        useJUnitPlatform()
    }
}

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

afterEvaluate {
    tasks.named("publishPluginMavenPublicationToMaven-releasesRepository") {
        enabled = false
    }
    tasks.named("publishPluginMavenPublicationToMavenLocal") {
        enabled = false
    }
}