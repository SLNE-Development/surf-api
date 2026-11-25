plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiMinestomImpl)
    implementation(projects.surfApiCoreImpl)
    compileOnlyApi(libs.minestom.server)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
    testImplementation(testFixtures(projects.surfApiMinestomImpl))
    testImplementation(libs.log4j.core)
    testRuntimeOnly(libs.log4j.slf4j2.impl)
}

description = "surf-api-minestom-plugins-impl"

tasks.test {
    systemProperty("minestom.inside-test", "true")
    systemProperty("junit.jupiter.execution.timeout.default", "2m")
}
