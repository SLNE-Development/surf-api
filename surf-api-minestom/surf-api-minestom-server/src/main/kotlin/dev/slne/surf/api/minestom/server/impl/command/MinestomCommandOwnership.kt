package dev.slne.surf.api.minestom.server.impl.command

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal class MinestomCommandOwnership {
    private val registrations = ConcurrentHashMap<String, CompiledRegistration>()

    internal fun contains(name: String): Boolean = registrations.containsKey(normalize(name))

    internal fun find(name: String): CompiledRegistration? = registrations[normalize(name)]

    internal fun findInput(text: String): CompiledRegistration? {
        val root = text
            .removePrefix("/")
            .takeWhile { character -> !character.isWhitespace() }

        return root
            .takeIf(String::isNotEmpty)
            ?.let(::find)
    }

    internal fun all(): Set<CompiledRegistration> {
        val registrations = ReferenceOpenHashSet<CompiledRegistration>()
        this.registrations.values.forEach { registration ->
            registrations += registration
        }

        return registrations
    }

    internal fun claim(names: Collection<String>, registration: CompiledRegistration) {
        val claimed = ObjectArrayList<String>(names.size)
        try {
            names.forEach { rawName ->
                val name = normalize(rawName)
                val previous = registrations.putIfAbsent(name, registration)

                check(previous == null || previous === registration) {
                    "Command name '$name' is already owned"
                }

                if (previous == null) claimed += name
            }
        } catch (failure: Throwable) {
            claimed.forEach { name -> registrations.remove(name, registration) }
            throw failure
        }
    }

    internal fun release(registration: CompiledRegistration) {
        registration.names.forEach { rawName ->
            registrations.remove(normalize(rawName), registration)
        }
    }

    fun ownsInput(text: String): Boolean {
        return findInput(text) != null
    }

    private fun normalize(name: String): String = name.lowercase(Locale.ROOT)
}
