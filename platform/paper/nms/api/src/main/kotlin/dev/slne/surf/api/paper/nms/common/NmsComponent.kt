package dev.slne.surf.api.paper.nms.common

/**
 * Marks a class of an NMS version module as a bean of that module.
 *
 * The annotation is no Spring stereotype, so surf-api's root component scan skips the class. Only
 * the module's [ConditionalOnMinecraftVersion] configuration scans for it, and only when its
 * version is the selected one. The class needs a constructor Spring can call, which receives its
 * collaborators; Spring cannot scan a Kotlin `object`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class NmsComponent
