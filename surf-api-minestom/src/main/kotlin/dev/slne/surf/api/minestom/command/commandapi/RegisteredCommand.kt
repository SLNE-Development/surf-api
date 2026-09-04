package dev.slne.surf.api.minestom.command.commandapi

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSets

class RegisteredCommand(
    val name: String,
    aliases: Collection<String> = emptySet(),
    val namespace: String? = null,
) {
    val aliases: Set<String> = ObjectSets.unmodifiable(ObjectLinkedOpenHashSet(aliases))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegisteredCommand) return false

        if (name != other.name) return false
        if (namespace != other.namespace) return false
        if (aliases != other.aliases) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + aliases.hashCode()
        return result
    }

    override fun toString(): String {
        return "RegisteredCommand(name='$name', namespace=$namespace, aliases=$aliases)"
    }
}
