package dev.slne.surf.api.generator.nms

/** Scope marker that prevents leaking receivers across nested DSL blocks. */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class NmsGeneratorDsl

