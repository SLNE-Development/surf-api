package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.BootstrapComponent
import dev.slne.surf.api.spring.SurfBootstrapLifecycle
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.AnnotationBeanNameGenerator
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
 *
 * Classes annotated with Spring Boot's `@AutoConfiguration` are left out: they enter a context only
 * through `@ImportAutoConfiguration`, which orders them by `@AutoConfigureBefore` and
 * `@AutoConfigureAfter`.
 *
 * @param beanNameGenerator names the components that declare no explicit name
 * @throws IllegalStateException if a scanned class carries `@ConditionalOnMissingBean`
 */
internal fun scanComponents(
    context: AnnotationConfigApplicationContext,
    classLoader: ClassLoader,
    basePackages: List<String>,
    selection: ScanSelection,
    source: Path? = null,
    beanNameGenerator: BeanNameGenerator = AnnotationBeanNameGenerator.INSTANCE,
) {
    val scanner = SurfComponentScanner(context, selection != ScanSelection.BOOTSTRAP)
    scanner.setResourceLoader(restrictedResolver(classLoader, source))
    scanner.setBeanNameGenerator(beanNameGenerator)
    scanner.addExcludeFilter(AnnotationTypeFilter(AutoConfiguration::class.java))

    when (selection) {
        ScanSelection.ALL -> {}
        ScanSelection.BOOTSTRAP -> scanner.addIncludeFilter(BootstrapComponentsFilter)
        ScanSelection.WITHOUT_BOOTSTRAP -> scanner.addExcludeFilter(BootstrapComponentsFilter)
    }

    scanner.scan(*basePackages.toTypedArray())
}

/**
 * A component scanner that rejects scanned classes carrying `@ConditionalOnMissingBean`.
 *
 * Spring evaluates that condition on a scanned class only after the class has registered itself as
 * a bean, so the condition matches the class itself and always removes it. The check reads the
 * candidates the scan registered, before any register-phase condition removes them.
 */
private class SurfComponentScanner(
    registry: BeanDefinitionRegistry,
    useDefaultFilters: Boolean,
) : ClassPathBeanDefinitionScanner(registry, useDefaultFilters) {

    override fun doScan(vararg basePackages: String): MutableSet<BeanDefinitionHolder> {
        val scanned = super.doScan(*basePackages)
        val offending = scanned
            .map(BeanDefinitionHolder::getBeanDefinition)
            .filterIsInstance<AnnotatedBeanDefinition>()
            .filter { it.metadata.isAnnotated(MISSING_BEAN_CONDITION) }
            .map { it.metadata.className }

        check(offending.isEmpty()) {
            "The scanned classes $offending carry @ConditionalOnMissingBean, which removes every " +
                "scanned class it annotates; use @Fallback or a @Bean method instead"
        }

        return scanned
    }

    private companion object {
        val MISSING_BEAN_CONDITION: String = ConditionalOnMissingBean::class.java.name
    }
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
    }

    return findCandidates(provider, plugin, annotation)
}

/**
 * Finds the interfaces in [plugin]'s packages that carry [annotation], the way
 * [findAnnotatedClasses] finds classes.
 *
 * @param context the plugin context whose bean factory evaluates conditions
 * @param plugin the plugin whose own source is read
 * @param annotation the annotation an interface must carry
 * @return the interfaces, loaded without initialization
 */
fun findAnnotatedInterfaces(
    context: AnnotationConfigApplicationContext,
    plugin: PluginDescriptor,
    annotation: Class<out Annotation>,
): List<Class<*>> {
    val provider = object : ClassPathScanningCandidateComponentProvider(false) {
        override fun getRegistry(): BeanDefinitionRegistry = context

        override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
            return beanDefinition.metadata.isInterface && !beanDefinition.metadata.isAnnotation
        }
    }

    return findCandidates(provider, plugin, annotation)
}

private fun findCandidates(
    provider: ClassPathScanningCandidateComponentProvider,
    plugin: PluginDescriptor,
    annotation: Class<out Annotation>,
): List<Class<*>> {
    provider.setResourceLoader(restrictedResolver(plugin.classLoader, plugin.source))
    provider.addIncludeFilter(AnnotationTypeFilter(annotation))

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