package dev.slne.surf.surfapi.gradle.platform.common

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import org.jetbrains.annotations.MustBeInvokedByOverriders

abstract class CommonSurfExtension(protected val objects: ObjectFactory) {
    internal val shadeKotlin = objects.property<Boolean>().convention(false)
    internal val addSurfApiToClasspath = objects.property<Boolean>().convention(true)

    fun shadeKotlin(value: Boolean) {
        shadeKotlin.set(value)
        shadeKotlin.finalizeValue()
    }

    fun addSurfApiToClasspath(value: Boolean) {
        addSurfApiToClasspath.set(value)
        addSurfApiToClasspath.finalizeValue()
    }

    @MustBeInvokedByOverriders
    internal open fun validate() {

    }
}