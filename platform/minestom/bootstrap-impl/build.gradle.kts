plugins {
    id("surf.module-impl")
}

dependencies {
    implementation(libs.gremlin.runtime)
}

description = "surf-api-minestom-bootstrap-impl"

publishRelocationFrom("surf-api-minestom-server-bootstrap")
