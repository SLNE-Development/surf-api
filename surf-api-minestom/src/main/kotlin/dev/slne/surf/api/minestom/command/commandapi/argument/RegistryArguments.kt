/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.argument

import com.mojang.brigadier.arguments.ArgumentType
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.InstanceParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.RegistryParser
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.Instance
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.potion.PotionEffect
import net.minestom.server.registry.Registry
import net.minestom.server.registry.RegistryKey
import net.minestom.server.sound.SoundEvent
import net.minestom.server.world.biome.Biome

class SoundArgument(nodeName: String) : Argument<SoundEvent>(nodeName) {
    override val kind = ArgumentKind.Sound
    override val rawType: ArgumentType<SoundEvent> = RegistryParser(
        registryName = "sound event",
        keys = { SoundEvent.values().map(SoundEvent::name) },
    ) { key -> SoundEvent.fromKey(key) }

    override fun stringify(value: SoundEvent): String = value.name()
}

class PotionEffectArgument(nodeName: String) : Argument<PotionEffect>(nodeName) {
    override val kind = ArgumentKind.PotionEffect
    override val rawType: ArgumentType<PotionEffect> = RegistryParser(
        registryName = "potion effect",
        keys = { PotionEffect.values().map { effect -> effect.key().asString() } },
    ) { key -> PotionEffect.fromKey(key) }

    override fun stringify(value: PotionEffect): String = value.key().asString()
}

class BiomeArgument(nodeName: String) : Argument<RegistryKey<Biome>>(nodeName) {
    override val kind = ArgumentKind.Biome
    override val rawType: ArgumentType<RegistryKey<Biome>> = RegistryParser(
        registryName = "biome",
        keys = { MinecraftServer.getBiomeRegistry().keys().map(RegistryKey<Biome>::name) },
    ) { key -> MinecraftServer.getBiomeRegistry().getKey(key) }

    override fun stringify(value: RegistryKey<Biome>): String = value.name()
}

class EnchantmentArgument(nodeName: String) : Argument<RegistryKey<Enchantment>>(nodeName) {
    override val kind = ArgumentKind.Enchantment
    override val rawType: ArgumentType<RegistryKey<Enchantment>> = RegistryParser(
        registryName = "enchantment",
        keys = {
            MinecraftServer.getEnchantmentRegistry().keys().map(RegistryKey<Enchantment>::name)
        },
    ) { key -> MinecraftServer.getEnchantmentRegistry().getKey(key) }

    override fun stringify(value: RegistryKey<Enchantment>): String = value.name()
}

/**
 * A resource resolved from an arbitrary [Registry] by namespaced key, advertised to the client
 * under [identifier].
 */
class ResourceArgument<T>(
    nodeName: String,
    identifier: String,
    registry: Registry<T>,
) : Argument<RegistryKey<T>>(nodeName) {
    override val kind = ArgumentKind.Resource(identifier, registry)
    override val rawType: ArgumentType<RegistryKey<T>> = RegistryParser(
        registryName = identifier,
        keys = { registry.keys().map(RegistryKey<T>::name) },
    ) { key -> registry.getKey(key) }

    override fun stringify(value: RegistryKey<T>): String = value.name()
}

/**
 * A live [Instance], resolved by its UUID or by its dimension name when that name is unique
 * among currently registered instances.
 */
class InstanceArgument(nodeName: String) : Argument<Instance>(nodeName) {
    override val kind = ArgumentKind.Instance
    override val rawType: ArgumentType<Instance> = InstanceParser
    override fun stringify(value: Instance): String = value.uuid.toString()
}
