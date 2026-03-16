package dev.slne.surf.microservice.gradle.plugin

interface ModuleDependency {
    val apiModule: String
    val runtimeModule: String
}