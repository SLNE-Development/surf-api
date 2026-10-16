plugins {
    `core-convention`
    `api-validation`
    `java-test-fixtures`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestom)
    implementation(projects.surfApiCore.surfApiCoreServer)
    compileOnlyApi(libs.minestom.server)

    implementation(libs.packetevents.netty.common)
    implementation(libs.sponge.mixin)
    implementation(libs.mixinextras)
    // EntityTrackerMixin shadows a field of this type; Minestom brings it at runtime
    compileOnly(libs.flare.fastutil)
    runtimeOnly(libs.flogger.slf4j.backend)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
    testImplementation(libs.log4j.core)
    testImplementation(libs.log4j.slf4j2.impl)

    testFixturesApi(libs.sponge.mixin)
    testFixturesImplementation(libs.bytebuddy.agent)
    testFixturesImplementation(libs.junit.platform.launcher)
}

description = "surf-api-minestom-server"

tasks.test {
    systemProperty("minestom.inside-test", "true")
    systemProperty("junit.jupiter.execution.timeout.default", "2m")
}
