plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
    id("dev.slne.surf.api.gradle.nats")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.nats.test.paper.PaperMain")
    authors.addAll("twisti")
}

dependencies {
    implementation(projects.surfApiNats.surfApiNatsTest.surfApiNatsTestCommon)
}
