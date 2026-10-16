package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandDefinition
import dev.slne.surf.api.minestom.command.RegisteredCommand
import dev.slne.surf.api.minestom.command.argument.ArgumentKind
import dev.slne.surf.api.minestom.command.argument.SuggestionMode
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import java.util.*

/**
 * A compiled command: the lowercase labels it occupies and the handle returned to its registrant.
 */
internal data class CompiledRegistration(
    val names: Set<String>,
    val registration: RegisteredCommand,
)

/**
 * Derives the labels a [CommandDefinition] occupies and validates the parts of it that cannot be
 * declared to a client.
 */
internal class MinestomCommandCompiler {

    /**
     * Compiles [definition] into its labels: the name and aliases, plus their `namespace:label`
     * forms when [namespace] is not `null`. Labels are lowercased and ordered by declaration.
     *
     * @throws CommandValidationException if an argument uses a suggestion mode its kind cannot carry
     */
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
     * Rejects suggestion modes on argument kinds that cannot carry them.
     *
     * Literal, command, position (3D, 2D and block) and rotation arguments accept no custom
     * suggestions. An entity type argument accepts only the replacing modes, because the native set
     * of summonable entity types is not available to merge extra entries into.
     */
    private fun validateSuggestionModes(definition: CommandDefinition) {
        definition.paths.forEach { path ->
            path.arguments.forEach { argument ->
                if (argument.suggestions == SuggestionMode.BuiltIns) return@forEach

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
     * Unwraps a custom kind to its base kind and a list kind to its element kind.
     */
    private tailrec fun baseKindOf(kind: ArgumentKind<*>): ArgumentKind<*> = when (kind) {
        is ArgumentKind.Custom<*, *> -> baseKindOf(kind.base.kind)
        is ArgumentKind.List<*> -> baseKindOf(kind.element.kind)
        else -> kind
    }

    private fun normalize(name: String): String = name.lowercase(Locale.ROOT)
}
