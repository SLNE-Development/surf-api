plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnlyApi("it.unimi.dsi:fastutil:8.5.12")
    compileOnlyApi("net.kyori:adventure-text-serializer-plain:4.14.0")
}

description = "surf-api-core-server"
