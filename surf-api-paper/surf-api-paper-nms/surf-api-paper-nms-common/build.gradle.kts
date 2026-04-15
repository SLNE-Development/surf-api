plugins {
    `core-convention`
}

dependencies {
    compileOnlyApi(projects.surfApiPaper.surfApiPaper)
    compileOnly(libs.paper.api)
}
