plugins {
    `core-convention`
}

description = "surf-api-minestom-example-plugin"

// A real plugin applies `dev.slne.surf.api.gradle.minestom`, which adds the api and generates the
// files in src/main/resources from `minestomPluginFile { }`. Inside this build that plugin is not
// available, so both are done by hand here.
dependencies {
    compileOnly(projects.surfApiMinestom.surfApiMinestom)
    compileOnly(libs.minestom.server)
}
