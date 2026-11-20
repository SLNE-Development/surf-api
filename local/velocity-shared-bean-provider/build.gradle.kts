plugins {
    `core-convention`
    kotlin("kapt")
}

description = "surf-api-velocity-shared-bean-provider"

dependencies {
    compileOnlyApi(projects.surfApiVelocityApi)
    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)
}

kapt {
    keepJavacAnnotationProcessors = true
}
