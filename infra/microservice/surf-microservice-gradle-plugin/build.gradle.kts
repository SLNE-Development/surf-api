plugins {
    id("dev.slne.surf.api.gradle.core")
    id("com.github.gmazzo.buildconfig") version "6.0.9"

    idea
    `kotlin-dsl`
}

/**
 * Gradle plugins that TestKit builds may apply next to the plugin under test (surf-api, Shadow, KSP, Kotlin).
 */
val surfApiTestPlugins = configurations.dependencyScope("surfApiTestPlugins")
val surfApiTestPluginClasspath = configurations.resolvable("surfApiTestPluginClasspath") {
    extendsFrom(surfApiTestPlugins.get())
    attributes.addAllLater(configurations.runtimeClasspath.get().attributes)
}

dependencies {
    implementation("com.bmuschko.docker-remote-api:com.bmuschko.docker-remote-api.gradle.plugin:10.0.0")

    // Brings Shadow; only touched inside pluginManager.withPlugin, so the plugin loads without it.
    compileOnly("dev.slne.surf.api:surf-api-gradle-plugin:+")
    surfApiTestPlugins("dev.slne.surf.api:surf-api-gradle-plugin:+")

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()

    val surfApiClasspath = surfApiTestPluginClasspath.map { it.incoming.files }
    inputs.files(surfApiClasspath)
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("surfApiTestPluginClasspath")
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Dsurf.microservice.test.surfApiClasspath=${surfApiClasspath.get().asPath}")
    }
}

gradlePlugin {
    plugins {
        create("surfMicroservice") {
            id = "dev.slne.surf.microservice"
            implementationClass = "dev.slne.surf.microservice.gradle.MicroservicePlugin"
        }
    }

    publishing {
        repositories {
            maven("https://reposilite.slne.dev/releases") {
                credentials {
                    username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                    password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
                }
            }
        }
    }
}

buildConfig {
    forClass("dev.slne.surf.microservice.gradle.generated", "Constants") {
        useKotlinOutput {
            internalVisibility = true
        }
        buildConfigField(
            "SURF_MICROSERVICE_VERSION",
            providers.gradleProperty("version").map { "${it.substringBefore('.')}.+" }
        )
    }
}
