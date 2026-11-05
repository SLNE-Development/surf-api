package dev.slne.surf.microservice.runtime.command.transport

internal data class MicroserviceCommandRequest(
    val label: String,
    val args: List<String>,
)
