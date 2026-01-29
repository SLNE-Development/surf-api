package dev.slne.surf.surfapi.shared.api.component

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ComponentMeta(
    val priority: Short = 0
)
