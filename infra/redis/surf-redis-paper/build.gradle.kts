plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.redis.PaperMain")
    bootstrapper("dev.slne.surf.redis.PaperBootstrap")
    foliaSupported(true)
    authors.addAll("twisti")
}

dependencies {
    api(project(":surf-redis-core"))
}

