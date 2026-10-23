import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.paper-raw")
}

surfRawPaperApi {
    testing {
        mockBukkit.set(true)
        // The surf-api default (4.116.1) is not published for mockbukkit-v26.3.
        mockBukkitVersion.set("4.118.0")
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    api(projects.surfNatsTypes.surfNatsTypesCommon)
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
