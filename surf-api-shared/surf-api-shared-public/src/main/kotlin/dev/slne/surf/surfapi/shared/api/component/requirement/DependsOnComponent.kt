package dev.slne.surf.surfapi.shared.api.component.requirement

import dev.slne.surf.surfapi.shared.api.component.Component
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnComponent(val component: KClass<out Component>)
