plugins {
    `paper-server-test`
}

description = "surf-api-paper-scenario-test"

repositories {
    maven("https://repo.opencollab.dev/main/") {
        name = "opencollab"
        content {
            includeGroup("org.geysermc.mcprotocollib")
            includeGroup("org.cloudburstmc.math")
            includeGroup("com.nukkitx.fastutil")
        }
    }
}

dependencies {
    paperTestImplementation(libs.mcprotocollib)

    paperTestCompileOnly(projects.surfApiPaperScenarioProbe)

    serverPlugins(
        project(path = ":surf-api-paper-scenario-probe", configuration = "runtimeElements"),
    )
    serverPlugins(
        project(path = ":surf-api-paper-scenario-owner", configuration = "runtimeElements"),
    )
}

/**
 * Starts a real Paper server with surf-api and the probe plugin, and drives it with bots.
 *
 * Not wired into `check` and not wired into `test`: run it by name.
 */
registerPaperServerTest(
    name = "paperScenarioTest",
    description = "Drives bots against a real Paper server and checks what the probe reports.",
)
