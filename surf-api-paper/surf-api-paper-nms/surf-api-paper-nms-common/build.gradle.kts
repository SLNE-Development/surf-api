plugins {
    `core-convention`
}

dependencies {
    api(projects.surfApiPaper.surfApiPaper)
    compileOnly(libs.paper.api)
}
