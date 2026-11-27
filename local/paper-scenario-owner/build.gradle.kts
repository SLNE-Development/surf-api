plugins {
    `core-convention`

    id("dev.slne.surf.api.gradle.paper-plugin")
}

description = "surf-api-paper-scenario-owner"

dependencies {
    compileOnlyApi(projects.surfApiPaperApi)
}

surfPaperPluginApi {
    authors.addAll("SLNE Development Team")
    basePackage.set("dev.slne.surf.api.paper.scenario.owner")
}

paper {
    name = "SurfScenarioOwner"
    description = "Owns temporary inventory sessions in surf-api's scenario tests"
}
