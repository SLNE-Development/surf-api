package dev.slne.surf.surfapi.shared.api.component.requirement

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnEnvironment(val environments: Array<String>)
