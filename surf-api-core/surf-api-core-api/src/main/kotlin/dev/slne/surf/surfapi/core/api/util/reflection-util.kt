package dev.slne.surf.surfapi.core.api.util

import java.lang.reflect.AccessibleObject

inline fun <reified A : Annotation> AccessibleObject.findAnnotation(): A? =
    getDeclaredAnnotation(A::class.java)