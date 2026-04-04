package dev.slne.surf.api.core.messages

import org.intellij.lang.annotations.Pattern

@Pattern("[A-Z][a-zA-Z0-9]*")
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
annotation class NoLowercase

@Pattern("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)*$")
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
annotation class BundlePath