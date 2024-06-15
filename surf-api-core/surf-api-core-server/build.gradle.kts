plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
}

description = "surf-api-core-server"
