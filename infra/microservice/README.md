# surf-microservice

The `dev.slne.surf.microservice` Gradle plugin configures Surf microservice dependencies and can generate a production-oriented, multi-stage Dockerfile for Coolify.

## Generate a Dockerfile

Run:

```shell
./gradlew generateMicroserviceDockerfile
```

The typed task is registered lazily in the `distribution` group. By default it writes `Dockerfile` in the consuming project root, uses the existing `shadowJar` task in the container build stage, derives Eclipse Temurin JDK/JRE images from the configured Java toolchain, and runs the application as a non-root user. The generated artifact path is relative to the root project, which is the Docker build context; for a subproject such as `surf-clan-microservice`, the generated `COPY` source therefore includes that subproject directory. Additional JVM flags can be supplied with `JAVA_OPTS`.

The output can be configured through the existing extension:

```kotlin
surfMicroservice {
    withMicroserviceApi()

    docker {
        outputFile.set(layout.projectDirectory.file("deploy/Dockerfile"))
        baseImage.set("eclipse-temurin:25-jre")
        builderImage.set("eclipse-temurin:25-jdk")
        overwrite.set(false)
    }
}
```

The default `overwrite = false` protects manually maintained Dockerfiles. A file carrying the generated marker can be regenerated safely; replacing any other existing file requires explicitly setting `overwrite = true`.

At task execution, the plugin inspects the resolved `runtimeClasspath` component graph. RabbitMQ environment documentation is emitted only when a resolved component belongs to `dev.slne.surf.rabbitmq`. Database documentation is emitted only for `dev.slne.surf:surf-database-r2dbc`, including when either dependency is transitive. Values and credentials are never generated into the Dockerfile—configure them as Coolify runtime variables/secrets.
