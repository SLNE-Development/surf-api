package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.argument.ArgumentKind
import dev.slne.surf.api.minestom.command.argument.SuggestionMode

internal fun SuggestionMode<*>.includesBuiltIns(): Boolean =
    this is SuggestionMode.Include<*> || this is SuggestionMode.IncludeSafe<*>

internal tailrec fun ArgumentKind<*>.baseKind(): ArgumentKind<*> = when (this) {
    is ArgumentKind.Custom<*, *> -> base.kind.baseKind()
    is ArgumentKind.List<*> -> element.kind.baseKind()
    else -> this
}
