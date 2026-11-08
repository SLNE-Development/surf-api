package dev.slne.surf.api.gen.generator.advancements

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.PUBLIC
import dev.slne.surf.api.gen.data.AdvancementCategory
import dev.slne.surf.api.gen.data.AdvancementRegistry
import dev.slne.surf.api.gen.generator.SourceGenerator
import java.nio.file.Path

class AdvancementGenerator(
    private val className: String,
    private val packageName: String,
    private val registry: AdvancementRegistry,
) : SourceGenerator {
    private val keyFun = MemberName("net.kyori.adventure.key.Key", "key")
    private val keyClass = ClassName("net.kyori.adventure.key", "Key")

    override fun writeToFile(parentJava: Path, parentKotlin: Path) {
        val root = TypeSpec.objectBuilder(className).apply {
            for (category in registry.categories) {
                addType(buildCategoryObject(category, emptyList()))
            }
        }.build()

        val fileSpec = FileSpec.builder(packageName, className)
            .addType(root)
            .build()
        fileSpec.writeTo(parentKotlin)
    }

    private fun buildCategoryObject(
        category: AdvancementCategory,
        parentSegments: List<String>,
    ): TypeSpec {
        val objectName = category.name.pascalCase()
        val type = TypeSpec.objectBuilder(objectName)

        val currentSegments = parentSegments + category.name.segment()

        for (entry in category.entries) {
            val propertyName = entry.upperSnake()
            val fullPath = (currentSegments + entry.segment()).joinToString("/")
            val property = PropertySpec.builder(propertyName, keyClass)
                .addModifiers(PUBLIC)
                .addAnnotation(JvmField::class)
                .initializer("%M(%S)", keyFun, fullPath)
                .build()

            type.addProperty(property)
        }

        for (children in category.children) {
            type.addType(buildCategoryObject(children, currentSegments))
        }

        return type.build()
    }

    private fun String.pascalCase(): String = split('_', '-', ' ', '.', '/')
        .filter { it.isNotBlank() }
        .joinToString("") { part ->
            part.lowercase().replaceFirstChar { it.titlecase() }
        }

    private fun String.classify(): String = split("_")
        .filter { it.isNotEmpty() }
        .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }

    private fun String.segment(): String = lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    private fun String.upperSnake(): String = replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .replace(Regex("[^A-Za-z0-9]+"), "_")
        .trim('_')
        .uppercase()
}