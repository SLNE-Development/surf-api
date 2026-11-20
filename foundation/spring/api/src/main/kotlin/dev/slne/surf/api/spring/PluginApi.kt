package dev.slne.surf.api.spring

import org.springframework.stereotype.Component

/**
 * Marks an interface as part of a plugin's public API.
 *
 * Every bean of a plugin that implements an interface carrying this annotation is exported: plugins
 * that declare the owning plugin as a direct dependency can inject it by that interface. All other
 * beans of a plugin stay private to its own context.
 *
 * The owning plugin keeps creating, initializing and destroying the exported instance; consumers
 * only receive a reference. An exported bean must therefore not implement
 * [org.springframework.context.Lifecycle] or [org.springframework.context.ApplicationListener],
 * because a consumer context would start, stop or register it a second time. Such an export fails
 * the start of the owning plugin.
 *
 * The annotation is meta-annotated with [Component] so that IDE tooling treats the interface as a
 * Spring type. Component scanning never registers an interface, so the interface itself does not
 * become a bean, and an implementing class still needs a stereotype such as `@Service` of its own.
 */
@Component
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class PluginApi
