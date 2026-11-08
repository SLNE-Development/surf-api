package dev.slne.surf.api.core.util

import java.lang.reflect.AccessibleObject

/**
 * Returns the annotation of type [A] that is directly present on this accessible object,
 * or null if no such annotation is present.
 *
 * This function provides a Kotlin-idiomatic way to retrieve annotations from Java reflection
 * objects without explicitly passing the annotation class. It uses reified type parameters
 * to infer the annotation type at compile time.
 *
 * @param A the type of annotation to query for
 * @return this element's annotation for the specified type if directly present, null otherwise
 */
inline fun <reified A : Annotation> AccessibleObject.findAnnotation(): A? =
    getDeclaredAnnotation(A::class.java)