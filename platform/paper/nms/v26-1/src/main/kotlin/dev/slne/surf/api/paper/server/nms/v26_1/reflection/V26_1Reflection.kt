package dev.slne.surf.api.paper.server.nms.v26_1.reflection

/**
 * Where this version module's reflection proxies would be created. There are none.
 *
 * [initialize] is empty because this module declares no `@Proxies` interface, not because anything
 * is switched off. It used to hold a `ReflectionRemapper.forReobfMappingsInPaperJar()` and a
 * `ReflectionProxyFactory`, kept as a comment, and that would be the wrong thing to restore: a
 * remapper translates *reobfuscated* names, and Paper has shipped Mojang-mapped since 1.20.5, so
 * there is nothing to translate. The 1.21.11 module still builds one that way, for
 * `V1_21_11ItemProxy`; from 26.1 on a member that has to be reached reflectively is declared with
 * `@ReflectedField` or `@ReflectedMethod` on the module's `NmsReflections` interface, and the
 * processor generates the `VarHandle` for it.
 *
 * Adding a proxy here means creating it in [initialize] and exposing it, the way the 1.21.11 module
 * does. Calling [initialize] without doing that gets nothing, and says nothing -- which is why this
 * reads the way it does.
 */
@Suppress("ClassName")
object V26_1Reflection {

    /** Nothing to do; see the class documentation before adding something. */
    fun initialize() = Unit
}
