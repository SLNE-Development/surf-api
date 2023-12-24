plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    api(project(":surf-api-core:surf-api-core-server"))
    api(project(":surf-api-velocity:surf-api-velocity-api"))
}

description = "surf-api-velocity-server"
