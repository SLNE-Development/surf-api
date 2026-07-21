package dev.slne.surf.microservice.gradle.plugin.rabbit

enum class RabbitModule(
    internal val artifactSuffix: String,
) {
    CLIENT_API("client-api"),
    COMMON_API("common-api"),
    SERVER_API("server-api"),
}