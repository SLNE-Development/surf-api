plugins {
    `core-convention`
    `api-validation`
}

repositories {
    maven {
        url = uri("https://repo.opencollab.dev/main/")
    }
}

dependencies {
    api(projects.surfApiPaper.surfApiPaper)
    compileOnly(libs.geyser.api)
    compileOnly(libs.floodgate.api)
}

description = "surf-api-paper"