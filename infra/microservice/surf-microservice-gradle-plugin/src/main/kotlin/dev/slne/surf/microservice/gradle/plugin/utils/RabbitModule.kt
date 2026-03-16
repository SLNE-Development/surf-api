package dev.slne.surf.microservice.gradle.plugin.utils

enum class RabbitModule(
    val module: String
) {
    CLIENT_API("client-api"),
    COMMON_API("common-api"),
    SERVER_API("server-api"),
}