package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandDefinition
import dev.slne.surf.api.minestom.command.RegisteredCommand
import dev.slne.surf.api.minestom.command.argument.ArgumentKind
import dev.slne.surf.api.minestom.command.argument.SuggestionMode
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import java.util.Locale

/**
 * The names a registered command answers to, together with the handle its registrant receives.
 */
internal data class CompiledRegistration(
    val names: Set<String>,
    val registration: RegisteredCommand,
)

/**
 * Turns a command definition into the set of names it occupies and validates the parts of it that
 * cannot be expressed to a client.
 */
internal class MinestomCommandCompiler {

    fun compile(definition: CommandDefinition, namespace: String?): CompiledRegistration {
        validateSuggestionModes(definition)

        val names = ObjectLinkedOpenHashSet<String>()
        val baseNames = ObjectArrayList<String>(definition.aliases.size + 1)

        baseNames += normalize(definition.name)
        definition.aliases.forEach { alias -> baseNames += normalize(alias) }

        names.addAll(baseNames)
        if (namespace != null) {
            baseNames.forEach { name -> names += normalize("$namespace:$name") }
        }

        return CompiledRegistration(
            names = names,
            registration = RegisteredCommand(definition.name, definition.aliases, namespace),
        )
    }

    /**
     * Rejects suggestion providers on kinds whose client-facing node cannot advertise them.
     *
     * A literal is not an argument node at all, and a command redirect keeps the client's own
     * redirect behaviour, so neither can carry an `ask_server` suggestion type.
     */
    private fun validateSuggestionModes(definition: CommandDefinition) {
        definition.paths.forEach { path ->
            path.arguments.forEach { argument ->
                if (argument.suggestions == SuggestionMode.BuiltIns) return@forEach

                // The client completes an entity type from its own registry, so extra entries
                // cannot be merged into that set without replacing it wholesale.
                val includesBuiltIns = argument.suggestions is SuggestionMode.Include<*> ||
                        argument.suggestions is SuggestionMode.IncludeSafe<*>
                if (baseKindOf(argument.kind) == ArgumentKind.EntityType && includesBuiltIns) {
                    throw CommandValidationException(
                        "EntityType argument '${argument.nodeName}' cannot include custom suggestions " +
                                "because Minestom does not expose the native summonable entity set; " +
                                "use built-ins or replace suggestions",
                    )
                }

                val kindName = when (baseKindOf(argument.kind)) {
                    is ArgumentKind.Literal -> "Literal"
                    ArgumentKind.Command -> "Command"
                    ArgumentKind.Position -> "Position"
                    ArgumentKind.Position2D -> "Position2D"
                    ArgumentKind.BlockPosition -> "BlockPosition"
                    ArgumentKind.Rotation -> "Rotation"
                    else -> return@forEach
                }

                throw CommandValidationException(
                    "$kindName argument '${argument.nodeName}' cannot use custom suggestions",
                )
            }
        }
    }

    /**
     * The kind a suggestion mode really applies to, looking through wrappers.
     *
     * A custom or list argument advertises the node of the argument it is built on, so a provider
     * attached to it lands on that node's parser.
     */
    private tailrec fun baseKindOf(kind: ArgumentKind<*>): ArgumentKind<*> = when (kind) {
        is ArgumentKind.Custom<*, *> -> baseKindOf(kind.base.kind)
        is ArgumentKind.List<*> -> baseKindOf(kind.element.kind)
        else -> kind
    }

    private fun normalize(name: String): String = name.lowercase(Locale.ROOT)
}
