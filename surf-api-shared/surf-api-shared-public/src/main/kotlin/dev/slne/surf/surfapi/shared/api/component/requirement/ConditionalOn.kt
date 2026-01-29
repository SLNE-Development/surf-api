package dev.slne.surf.surfapi.shared.api.component.requirement

import dev.slne.surf.surfapi.shared.api.component.condition.ComponentCondition
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOn(val condition: KClass<out ComponentCondition>)
