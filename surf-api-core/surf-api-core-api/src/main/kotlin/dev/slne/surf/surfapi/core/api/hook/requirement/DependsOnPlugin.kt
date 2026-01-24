package dev.slne.surf.surfapi.core.api.hook.requirement

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnPlugin(val pluginId: String)
