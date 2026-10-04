package dev.slne.surf.api.minestom.extension

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.generator.UnitModifier
import net.minestom.server.registry.DynamicRegistry
import net.minestom.server.registry.RegistryKey
import net.minestom.server.world.DimensionType
import java.util.*

inline fun GenerationUnit.modify(block: UnitModifier.() -> Unit) {
    this.modifier().apply(block)
}

inline fun InstanceContainer.generator(crossinline block: GenerationUnit.() -> Unit) =
    setGenerator {
        block(it)
    }

inline fun buildInstance(
    dimensionType: RegistryKey<DimensionType> = DimensionType.OVERWORLD,
    dimensionName: Key = dimensionType.key(),
    register: Boolean = true,
    block: InstanceContainer.() -> Unit,
): InstanceContainer = InstanceContainer(
    MinecraftServer.getRegistries(),
    UUID.randomUUID(),
    dimensionType,
    null,
    dimensionName
).apply {
    block()
    if (register) InstanceManager.registerInstance(this)
}