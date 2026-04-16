plugins {
    `core-convention`
}

dependencies {
    compileOnlyApi(projects.surfApiPaper.surfApiPaper)
    compileOnlyApi(projects.surfApiShared.surfApiSharedInternal)
    compileOnly(libs.paper.api)
}
