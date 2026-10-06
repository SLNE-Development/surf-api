plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.nats.PaperMain")
    authors.addAll("twisti")
}

dependencies {
    api(projects.surfNatsCore)
}
