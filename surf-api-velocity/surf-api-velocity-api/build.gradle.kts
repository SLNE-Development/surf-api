plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    compileOnlyApi(libs.velocity.api)
    api(project(":surf-api-core-api"))
}

description = "surf-api-velocity-api"
