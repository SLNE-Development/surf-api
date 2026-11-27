plugins {
    `paper-server-test`
}

description = "surf-api-paper-shared-bean-test"

dependencies {
    paperTestCompileOnly(projects.surfApiPaperSharedBeanConsumer)
    paperTestCompileOnly(projects.surfApiPaperSharedBeanProvider)

    serverPlugins(
        project(path = ":surf-api-paper-shared-bean-provider", configuration = "runtimeElements"),
    )
    serverPlugins(
        project(
            path = ":surf-api-paper-shared-bean-consumer",
            configuration = "shadowRuntimeElements",
        ),
    )
}

/**
 * Starts a real Paper server with surf-api and the two test plugins, and asserts on its log.
 *
 * Not wired into `check` and not wired into `test`: run it by name.
 */
registerPaperServerTest(
    name = "paperSharedBeanBootTest",
    description = "Boots a real Paper server and proves a @PluginApi bean crosses two plugins.",
)
