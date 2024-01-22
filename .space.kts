job("Build and publish") {
    container("Run gradle build", "eclipse-temurin:17") {
        env["REPOSITORY_URL"] = "https://packages.slne.dev/maven/p/surf/maven"

        kotlinScript { api ->
            api.gradlew("shadowJar", "--stacktrace")
            api.gradlew("publish", "--stacktrace")
        }
    }
}