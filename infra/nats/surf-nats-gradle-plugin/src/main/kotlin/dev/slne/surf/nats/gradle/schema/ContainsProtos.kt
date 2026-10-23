package dev.slne.surf.nats.gradle.schema

import java.io.File

internal fun File.containsProtos(): Boolean = isDirectory && walkTopDown().any { it.isFile && it.extension == "proto" }
