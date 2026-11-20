plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
    id("dev.slne.surf.api.gradle.nats")
}

surfPaperPluginApi {
    authors.addAll("twisti")
}

dependencies {
    implementation(projects.surfApiNats.surfApiNatsTest.surfApiNatsTestCommon)
}
