package dev.slne.surf.api.shared.internal.nms

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class NmsProviderMarker(val version: NmsVersion)
