package dev.slne.surf.surfapi.shared.api.hook.requirement

import dev.slne.surf.surfapi.shared.api.hook.Hook
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnHook(val hook: KClass<out Hook>)
