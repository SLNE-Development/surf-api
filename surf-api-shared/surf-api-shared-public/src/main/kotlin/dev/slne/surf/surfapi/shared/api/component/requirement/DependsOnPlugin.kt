package dev.slne.surf.surfapi.shared.api.component.requirement

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnPlugin(val pluginId: String)
