package dev.slne.surf.surfapi.shared.api.hook.requirement

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnClass(val clazz: KClass<*>)
