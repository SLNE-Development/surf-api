package dev.slne.surf.nats.gradle.codegen

/**
 * Service identity used to detect duplicate resolved service names within one generation run; equal keys have equal
 * runtime registration keys.
 */
internal data class ServiceKey(val global: Boolean, val name: String, val majorVersion: Int)
