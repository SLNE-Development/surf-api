plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestom)
    implementation(projects.surfApiCore.surfApiCoreServer)
    compileOnlyApi(libs.minestom.server)

    implementation(libs.packetevents.netty.common)
    runtimeOnly(libs.flogger.slf4j.backend)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
    testImplementation(libs.log4j.core)
    testImplementation(libs.log4j.slf4j2.impl)
}

description = "surf-api-minestom-server"

tasks.test {
    systemProperty("minestom.inside-test", "true")
}
