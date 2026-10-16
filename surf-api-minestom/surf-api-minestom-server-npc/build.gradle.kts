plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestomServer)
    compileOnlyApi(libs.minestom.server)
    api(libs.minestom.npc) { exclude(group = "net.minestom") }

    compileOnly(libs.sponge.mixin)
    compileOnly(libs.mixinextras)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
    testImplementation(testFixtures(projects.surfApiMinestom.surfApiMinestomServer))
}

tasks.test {
    systemProperty("minestom.inside-test", "true")
}

description = "surf-api-minestom-server-npc"
