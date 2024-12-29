package dev.slne.surf.api.gen.generator

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import java.nio.file.Path

abstract class SimpleGenerator(
    protected val className: String,
    protected val packageName: String,
): SourceGenerator {
    abstract val typeSpec: TypeSpec

    abstract fun buildFile(builder: JavaFile.Builder): JavaFile.Builder

    override fun writeToFile(parent: Path) {
        val builder = JavaFile.builder(packageName, typeSpec)
        val file = buildFile(builder)
            .indent("  ")
            .skipJavaLangImports(true)
            .build()

        file.writeTo(parent)
    }
}