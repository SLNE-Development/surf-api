plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.nats.PaperMain")
    bootstrapper("dev.slne.surf.nats.PaperBootstrap")
    authors.addAll("twisti")
}

dependencies {
    api(projects.surfNatsCore)
    implementation(projects.surfNatsTypes.surfNatsTypesPaper)
}
