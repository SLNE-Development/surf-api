package dev.slne.surf.surfapi.gradle.platform

class InvalidPluginFileException(message: String): Exception("Invalid plugin file configuration: $message")

fun invalidPluginFile(message: String): Nothing {
    throw  InvalidPluginFileException(message)
}