plugins {
    id("dev.slne.surf.api.gradle.core")
    id("dev.slne.surf.api.gradle.nats")
}

surfNats {
    generatedVisibility = INTERNAL
}
