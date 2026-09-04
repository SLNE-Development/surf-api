package dev.slne.surf.api.minestom.command.commandapi.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import net.minestom.server.particle.Particle

/**
 * Reads a namespaced particle key and resolves it to its option-free [Particle.Simple] value.
 *
 * A key naming a particle that carries options (`dust`, `block`, `vibration`, and similar) is
 * rejected as unsupported rather than silently resolved to its default-valued instance, since this
 * parser has no syntax for particle options. An unrecognized key is rejected as unknown. Both
 * failures are reported at the position the key started at.
 */
internal object ParticleParser : ArgumentType<Particle> {
    private val UNKNOWN = DynamicCommandExceptionType { value ->
        LiteralMessage("Unknown particle '$value'")
    }

    private val UNSUPPORTED = DynamicCommandExceptionType { value ->
        LiteralMessage("Unsupported particle '$value'; it requires options this argument cannot express")
    }

    override fun parse(reader: StringReader): Particle {
        val start = reader.cursor
        val key = ResourceLocationParser.readKey(reader)
        val particle = Particle.fromKey(key) ?: run {
            reader.cursor = start
            throw UNKNOWN.createWithContext(reader, key.asString())
        }

        if (particle !is Particle.Simple) {
            reader.cursor = start
            throw UNSUPPORTED.createWithContext(reader, key.asString())
        }

        return particle
    }
}
