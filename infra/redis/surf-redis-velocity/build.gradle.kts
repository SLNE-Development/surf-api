plugins {
    id("dev.slne.surf.api.gradle.velocity")
}

velocityPluginFile {
    main = "dev.slne.surf.redis.VelocityMain"
    authors = listOf("twisti")
}

dependencies {
    api(projects.surfRedisCore)
}