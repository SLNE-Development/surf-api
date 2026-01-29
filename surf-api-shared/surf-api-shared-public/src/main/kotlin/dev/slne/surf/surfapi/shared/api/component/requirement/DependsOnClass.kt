package dev.slne.surf.surfapi.shared.api.component.requirement

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnClass(val clazz: KClass<*>)
