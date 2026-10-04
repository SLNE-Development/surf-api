plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiMinestom.surfApiMinestomServer)
    compileOnlyApi(libs.minestom.server)
    api(libs.minestom.npc)

    testImplementation(libs.minestom.server)
    testImplementation(libs.minestom.testing)
}

tasks.test {
    systemProperty("minestom.inside-test", "true")
}

description = "surf-api-minestom-server-npc"
