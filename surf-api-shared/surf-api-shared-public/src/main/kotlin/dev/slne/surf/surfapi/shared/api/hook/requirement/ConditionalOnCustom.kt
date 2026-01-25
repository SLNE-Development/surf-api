package dev.slne.surf.surfapi.shared.api.hook.requirement

import dev.slne.surf.surfapi.shared.api.hook.condition.HookCondition
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnCustom(val condition: KClass<out HookCondition>)