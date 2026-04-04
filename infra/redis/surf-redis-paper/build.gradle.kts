plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.redis.PaperMain")
    bootstrapper("dev.slne.surf.redis.PaperBootstrap")
    foliaSupported(true)
    authors.addAll("twisti")
}

dependencies {
    api(projects.surfRedisCore)
}

