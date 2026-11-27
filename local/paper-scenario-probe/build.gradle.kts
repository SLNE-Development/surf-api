plugins {
    `core-convention`

    id("dev.slne.surf.api.gradle.paper-plugin")
}

description = "surf-api-paper-scenario-probe"

dependencies {
    compileOnlyApi(projects.surfApiPaperApi)
}

surfPaperPluginApi {
    authors.addAll("SLNE Development Team")
}

paper {
    name = "SurfScenarioProbe"
    description = "Answers the requests of surf-api's bot-driven Paper scenario tests"
}
