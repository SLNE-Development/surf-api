package dev.slne.surf.surfapi.shared.api.hook

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookMeta(
    val priority: Short = 0
)
