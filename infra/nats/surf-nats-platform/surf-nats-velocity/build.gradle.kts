plugins {
    id("dev.slne.surf.api.gradle.velocity")
}

velocityPluginFile {
    main = "dev.slne.surf.nats.VelocityMain"
    authors = listOf("twisti")
}

dependencies {
    api(projects.surfNatsCore)
}
