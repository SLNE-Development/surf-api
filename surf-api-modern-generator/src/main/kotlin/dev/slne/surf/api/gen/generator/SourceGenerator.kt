package dev.slne.surf.api.gen.generator

import java.nio.file.Path

fun interface SourceGenerator {
    fun writeToFile(parentJava: Path, parentKotlin: Path)
}