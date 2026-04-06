package dev.slne.surf.api.gradle.util

import net.minecrell.pluginyml.paper.PaperPluginDescription.DependencyDefinition
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder
import org.gradle.kotlin.dsl.NamedDomainObjectContainerScope

/**
 * Registers a soft dependency.
 *
 * @param name The name of the dependency.
 * @param joinClassPath Whether the dependency should be joined to the classpath.
 * @param loadOrder The load order of the dependency.
 */
fun NamedDomainObjectContainerScope<DependencyDefinition>.registerSoft(
    name: String,
    joinClassPath: Boolean = true,
    loadOrder: RelativeLoadOrder = RelativeLoadOrder.BEFORE,
) = register(name) {
    this.required = false
    this.joinClasspath = joinClassPath
    this.load = loadOrder
}

/**
 * Registers a required dependency.
 *
 * @param name The name of the dependency.
 * @param joinClassPath Whether the dependency should be joined to the classpath.
 * @param loadOrder The load order of the dependency.
 */
fun NamedDomainObjectContainerScope<DependencyDefinition>.registerRequired(
    name: String,
    joinClassPath: Boolean = true,
    loadOrder: RelativeLoadOrder = RelativeLoadOrder.BEFORE,
) = register(name) {
    this.required = true
    this.joinClasspath = joinClassPath
    this.load = loadOrder
}
