plugins {
    `core-convention`
    kotlin("kapt")
}

description = "surf-api-velocity-shared-bean-consumer"

dependencies {
    compileOnlyApi(projects.surfApiVelocityApi)
    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)

    compileOnly(projects.surfApiVelocitySharedBeanProvider)

    compileOnly(libs.luckperms)
}

kapt {
    keepJavacAnnotationProcessors = true
}
