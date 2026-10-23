import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.core")
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

tasks.processResources {
    from("src/main/proto") {
        into("META-INF/surf-nats/proto")
    }
}

publishing {
    repositories {
        slneReleases()
    }
}
