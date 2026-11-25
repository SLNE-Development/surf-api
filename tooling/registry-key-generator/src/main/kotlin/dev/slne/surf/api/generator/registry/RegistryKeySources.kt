package dev.slne.surf.api.generator.registry

import com.squareup.kotlinpoet.*
import java.util.*

/**
 * Builds the Kotlin sources of `surf-api-registry-keys` from vanilla data.
 *
 * Every key is a `@JvmField` property of an `object`, compiled to a `public static final Key`
 * field without a getter.
 */
object RegistryKeySources {
    private const val PACKAGE = "dev.slne.surf.api.core.generated"
    private const val VERSION_NOTE =
        "The keys mirror the vanilla game data and may change, including removals,\n" +
        "with any Minecraft version."

    private val keyClass = ClassName("net.kyori.adventure.key", "Key")
    private val keyFunction = MemberName(keyClass, "key")
    private val trailingNumber = Regex("_(\\d+)$")
    private val keyOrder = compareBy<String> { it.replace(trailingNumber, "") }
        .thenBy { trailingNumber.find(it)?.groupValues?.get(1)?.toBigInteger() }

    /**
     * Builds the key objects from vanilla data.
     *
     * @param data the vanilla data generator output
     * @return one file per key object, ready to be written to a Kotlin source root
     */
    fun create(data: VanillaData): List<FileSpec> {
        return listOf(
            registryKeys("SoundKeys", "minecraft:sound_event", data),
            registryKeys("BlockTypeKeys", "minecraft:block", data),
            registryKeys("ItemTypeKeys", "minecraft:item", data),
            advancementKeys("VanillaAdvancementKeys", data.advancementCategories()),
        )
    }

    private fun registryKeys(name: String, registry: String, data: VanillaData): FileSpec {
        val type = TypeSpec.objectBuilder(name)
            .addKdoc("Vanilla keys of the `%L` registry.\n\n%L", registry, VERSION_NOTE)
        for (key in data.registryEntries(registry).sortedWith(keyOrder)) {
            type.addProperty(keyProperty(registryFieldName(key), key))
        }

        return keyFile(type.build())
    }

    private fun registryFieldName(key: String): String {
        return key.removePrefix("minecraft:")
            .uppercase(Locale.ROOT)
            .replace('.', '_')
            .replace('/', '_')
    }

    private fun advancementKeys(name: String, categories: List<AdvancementCategory>): FileSpec {
        val type = TypeSpec.objectBuilder(name)
            .addKdoc("Vanilla advancement keys, grouped by tab.\n\n%L", VERSION_NOTE)
        for (category in categories) {
            type.addType(advancementCategory(category, parentPath = emptyList()))
        }

        return keyFile(type.build())
    }

    private fun keyFile(type: TypeSpec): FileSpec {
        return FileSpec.builder(PACKAGE, type.name!!)
            .addKotlinDefaultImports()
            .addType(type)
            .build()
    }

    private fun advancementCategory(
        category: AdvancementCategory,
        parentPath: List<String>,
    ): TypeSpec {
        val path = parentPath + category.name.pathSegment()
        val type = TypeSpec.objectBuilder(category.name.pascalCase())

        for (entry in category.entries) {
            val key = (path + entry.pathSegment()).joinToString("/")
            type.addProperty(keyProperty(entry.upperSnakeCase(), key))
        }

        for (child in category.children) {
            type.addType(advancementCategory(child, path))
        }

        return type.build()
    }

    private fun keyProperty(name: String, key: String): PropertySpec {
        return PropertySpec.builder(name, keyClass)
            .addAnnotation(JvmField::class)
            .initializer("%M(%S)", keyFunction, key)
            .build()
    }

    private fun String.pascalCase(): String {
        return split('_', '-', ' ', '.', '/')
            .filter { it.isNotBlank() }
            .joinToString("") { part -> part.lowercase().replaceFirstChar { it.titlecase() } }
    }

    private fun String.pathSegment(): String {
        return lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    private fun String.upperSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .uppercase()
    }
}
