plugins {
    id("dev.slne.surf.api.gradle.core")
}

val surfDatabaseR2dbc = "dev.slne.surf:surf-database-r2dbc:2.+"

dependencies {
    api(projects.surfMicroserviceApi)
    compileOnly(surfDatabaseR2dbc)

    testImplementation(surfDatabaseR2dbc)
}
