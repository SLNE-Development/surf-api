package dev.slne.surf.surfapi.gradle.platform.paper.plugin

import dev.slne.surf.surfapi.gradle.platform.paper.AbstractPaperSurfExtension
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.NamedDomainObjectContainerScope
import org.gradle.kotlin.dsl.property
import xyz.jpenilla.runpaper.task.RunServer
import javax.inject.Inject

open class PaperPluginSurfExtension @Inject constructor(objects: ObjectFactory) :
    AbstractPaperSurfExtension(objects) {
    internal val mainClass = objects.property<String>()
    internal val bootstrapper = objects.property<String>().convention(null)
    internal val bootstrapDependencies =
        objects.property<Action<NamedDomainObjectContainerScope<PaperPluginDescription.DependencyDefinition>>>()
            .convention(null)
    internal val serverDependencies =
        objects.property<Action<NamedDomainObjectContainerScope<PaperPluginDescription.DependencyDefinition>>>()
            .convention(null)
    internal val runServer = objects.property<Action<RunServer>>().convention(null)

    fun mainClass(mainClass: String) {
        this.mainClass.set(mainClass)
        this.mainClass.finalizeValue()
    }

    fun bootstrapper(bootstrapper: String) {
        this.bootstrapper.set(bootstrapper)
        this.bootstrapper.finalizeValue()
    }

    fun bootstrapDependencies(action: Action<NamedDomainObjectContainerScope<PaperPluginDescription.DependencyDefinition>>) {
        bootstrapDependencies.set(action)
        bootstrapDependencies.finalizeValue()
    }

    fun serverDependencies(action: Action<NamedDomainObjectContainerScope<PaperPluginDescription.DependencyDefinition>>) {
        serverDependencies.set(action)
        serverDependencies.finalizeValue()
    }

    fun runServer(action: Action<RunServer>) {
        runServer.set(action)
        runServer.finalizeValue()
    }

    override fun validate() {
        super.validate()
        require(mainClass.isPresent) { "Main class must be set to your plugin's main class" }
    }
}