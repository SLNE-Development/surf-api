package dev.slne.surf.api.gen.generator

import dev.slne.surf.api.gen.data.GenericRegistry
import dev.slne.surf.api.gen.data.Registries
import dev.slne.surf.api.gen.generator.types.GeneratedKeyType

class Generators(registries: Registries) {

    val coreApiGenerators = arrayOf<SourceGenerator>(
        simpleKey("SoundKeys", registries.soundRegistry)
    )

    private fun simpleKey(className: String, registry: GenericRegistry) =
        GeneratedKeyType(className, "dev.slne.surf.surfapi.core.api.generated", registry)
}