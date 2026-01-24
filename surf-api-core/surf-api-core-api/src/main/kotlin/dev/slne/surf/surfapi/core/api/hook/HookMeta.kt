package dev.slne.surf.surfapi.core.api.hook

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookMeta(
    val priority: Short = 0
)
