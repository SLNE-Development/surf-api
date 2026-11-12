plugins {
    id("dev.slne.surf.api.gradle.core")
    id("dev.slne.surf.nats")
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

surfNats {
    mapType(
        "surf.nats.test.v1.Tag",
        "dev.slne.surf.nats.test.fixtures.Tag",
        "dev.slne.surf.nats.test.fixtures.TagSerializer",
    )
}
