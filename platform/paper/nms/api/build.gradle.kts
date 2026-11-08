plugins {
    `core-convention`
}

dependencies {
    compileOnlyApi(projects.surfApiPaperApi)
    compileOnlyApi(projects.surfApiInternal)
    compileOnly(libs.paper.api)
}
