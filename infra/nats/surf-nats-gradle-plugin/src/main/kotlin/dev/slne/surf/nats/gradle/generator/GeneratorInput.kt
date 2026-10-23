package dev.slne.surf.nats.gradle.generator

import java.io.File
import java.io.Serializable

/**
 * Maps a fully qualified proto message or enum to an existing Kotlin class and its `KSerializer` class.
 */
data class TypeMapping(val protoType: String, val kotlinClass: String, val serializerClass: String) : Serializable

enum class GeneratedVisibility { PUBLIC, INTERNAL }

/**
 * One generation run; [typeMappings] (`mapType`) override [defaultTypeMappings], the platform defaults the plugin
 * provides.
 */
data class GeneratorInput(
    val sourceDirs: List<File>,
    val protoPathDirs: List<File>,
    val typeMappings: List<TypeMapping>,
    val visibility: GeneratedVisibility,
    val defaultTypeMappings: List<TypeMapping>,
)

class GenerationResult(val files: List<File>)

/**
 * Reports every problem found in one generation run, each formatted as `file:line:col: message`.
 */
class SurfNatsGeneratorException(val errors: List<String>) : RuntimeException(
    "surf-nats code generation failed:\n" + errors.joinToString("\n")
)
