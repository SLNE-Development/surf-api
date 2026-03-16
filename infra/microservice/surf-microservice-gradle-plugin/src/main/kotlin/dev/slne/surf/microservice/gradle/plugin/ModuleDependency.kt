package dev.slne.surf.microservice.gradle.plugin

interface ModuleDependency {
    val module: String
    val surfApiPlugin: SurfApiPlugin
}