plugins {
    id("dev.slne.surf.surfapi.gradle.velocity")
}

velocityPluginFile {
    main = "dev.slne.surf.redis.VelocityMain"
    authors = listOf("twisti")
}

dependencies {
    api(project(":surf-redis-core"))
}