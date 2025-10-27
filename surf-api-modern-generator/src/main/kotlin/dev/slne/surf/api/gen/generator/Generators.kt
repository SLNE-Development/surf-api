package dev.slne.surf.api.gen.generator

import dev.slne.surf.api.gen.data.AdvancementRegistry
import dev.slne.surf.api.gen.data.GenericRegistry
import dev.slne.surf.api.gen.data.Registries
import dev.slne.surf.api.gen.generator.advancements.AdvancementGenerator
import dev.slne.surf.api.gen.generator.types.GeneratedKeyType

class Generators(registries: Registries, advancementRegistry: AdvancementRegistry) {

    val coreApiGenerators = arrayOf<SourceGenerator>(
        simpleKey("SoundKeys", registries.soundRegistry),
        simpleKey("BlockTypeKeys", registries.blockTypeRegistry),
        simpleKey("ItemTypeKeys", registries.itemTypeRegistry),
        AdvancementGenerator("VanillaAdvancementKeys", CORE_GENERATED_PACKAGE, advancementRegistry),
    )

    val bukkitApiGenerators = arrayOf<SourceGenerator>(
    )

    companion object {
        private const val CORE_GENERATED_PACKAGE = "dev.slne.surf.surfapi.core.api.generated"
        private const val BUKKIT_GENERATED_PACKAGE = "dev.slne.surf.surfapi.bukkit.api.generated"

        private fun simpleKey(className: String, registry: GenericRegistry) =
            GeneratedKeyType(className, CORE_GENERATED_PACKAGE, registry)
    }
}