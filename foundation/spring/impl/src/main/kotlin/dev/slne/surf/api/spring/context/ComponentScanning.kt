package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.BootstrapComponent
import dev.slne.surf.api.spring.SurfBootstrapLifecycle
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.core.type.filter.TypeFilter
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * Which components a scan registers.
 */
internal enum class ScanSelection {

    /**
     * Every component.
     */
    ALL,

    /**
     * Only bootstrap components, for a bootstrap context.
     */
    BOOTSTRAP,

    /**
     * Every component except the bootstrap components, for the context below a bootstrap context.
     */
    WITHOUT_BOOTSTRAP,
}

/**
 * Scans [basePackages] for components into [context].
 *
 * With a [source], only classes from that jar or directory are registered; without one, every
 * class [classLoader] can see under the packages is. Supertypes are read through
 * [PluginResourceLoader], so they resolve even when they live in a jar the class loader joins.
 */
internal fun scanComponents(
    context: AnnotationConfigApplicationContext,
    classLoader: ClassLoader,
    basePackages: List<String>,
    selection: ScanSelection,
    source: Path? = null,
) {
    ClassPathBeanDefinitionScanner(context, selection != ScanSelection.BOOTSTRAP).apply {
        setResourceLoader(restrictedResolver(classLoader, source))
        when (selection) {
            ScanSelection.ALL -> {}
            ScanSelection.BOOTSTRAP -> addIncludeFilter(BootstrapComponentsFilter)
            ScanSelection.WITHOUT_BOOTSTRAP -> addExcludeFilter(BootstrapComponentsFilter)
        }
    }.scan(*basePackages.toTypedArray())
}

/**
 * Finds the classes in [plugin]'s packages that carry [annotation], reading only the plugin's
 * own source as the component scan does, without registering them.
 *
 * Classes are loaded through the plugin's class loader without being initialized. Conditions
 * such as `@ConditionalOnPlugin` on a class are evaluated against [context]'s bean factory.
 */
fun findAnnotatedClasses(
    context: AnnotationConfigApplicationContext,
    plugin: PluginDescriptor,
    annotation: Class<out Annotation>,
): List<Class<*>> {
    val provider = object : ClassPathScanningCandidateComponentProvider(false) {
        override fun getRegistry(): BeanDefinitionRegistry = context
    }.apply {
        setResourceLoader(restrictedResolver(plugin.classLoader, plugin.source))
        addIncludeFilter(AnnotationTypeFilter(annotation))
    }

    return plugin.basePackages.asSequence()
        .flatMap(provider::findCandidateComponents)
        .mapNotNull { it.beanClassName }
        .distinct()
        .map { Class.forName(it, false, plugin.classLoader) }
        .toList()
}

/**
 * The resources a scan reads: every class [classLoader] sees, or only those of [source] if given.
 */
private fun restrictedResolver(classLoader: ClassLoader, source: Path?): ResourcePatternResolver {
    val resolver = PathMatchingResourcePatternResolver(PluginResourceLoader(classLoader))
    return source?.let { PluginSourceResourceResolver(resolver, it) } ?: resolver
}

/**
 * Matches the components of a bootstrap context: those implementing [SurfBootstrapLifecycle] and
 * those annotated with [BootstrapComponent]. Both checks read bytecode metadata only.
 */
private object BootstrapComponentsFilter : TypeFilter {
    private val component = AnnotationTypeFilter(Component::class.java)
    private val bootstrapLifecycle = AssignableTypeFilter(SurfBootstrapLifecycle::class.java)
    private val bootstrapComponent = AnnotationTypeFilter(BootstrapComponent::class.java)

    override fun match(
        metadataReader: MetadataReader,
        metadataReaderFactory: MetadataReaderFactory,
    ): Boolean {
        val isComponent = component.match(metadataReader, metadataReaderFactory)
        val isBootstrapComponent = bootstrapComponent.match(metadataReader, metadataReaderFactory)
        val isBootstrapLifecycle = bootstrapLifecycle.match(metadataReader, metadataReaderFactory)

        return isComponent && (isBootstrapComponent || isBootstrapLifecycle)
    }
}