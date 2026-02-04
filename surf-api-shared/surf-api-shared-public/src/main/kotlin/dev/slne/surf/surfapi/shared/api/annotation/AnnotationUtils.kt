package dev.slne.surf.surfapi.shared.api.annotation

import java.util.*

object AnnotationUtils {

    private val cache = Collections.synchronizedMap(WeakHashMap<Class<*>, MutableMap<Class<out Annotation>, Any?>>())
    private val NULL_SENTINEL = Any()

    /**
     * Finds a single annotation of [annotationType] on [clazz], searching in this order:
     * 1) Directly on the class
     * 2) Meta-annotations (annotations on the class' annotations), recursively
     * 3) Interfaces, recursively
     * 4) Superclasses, recursively
     *
     * Notes:
     * - Only runtime-retained annotations can be found.
     */
    fun <A : Annotation> findAnnotation(clazz: Class<*>, annotationType: Class<A>): A? {
        val cached = getCached(clazz, annotationType)
        if (cached !== null) {
            @Suppress("UNCHECKED_CAST")
            return if (cached === NULL_SENTINEL) null else cached as A
        }

        return findAnnotationInternal(
            clazz = clazz,
            annotationType = annotationType,
            visitedClasses = HashSet(),
            visitedAnnotationTypes = HashSet()
        ).also {
            putCached(
                clazz = clazz,
                annotationType = annotationType,
                value = it ?: NULL_SENTINEL
            )
        }
    }

    private fun <A : Annotation> getCached(clazz: Class<*>, annotationType: Class<A>): Any? {
        val perClass = cache[clazz] ?: return null
        return perClass[annotationType]
    }

    private fun putCached(clazz: Class<*>, annotationType: Class<out Annotation>, value: Any) {
        val perClass = cache.getOrPut(clazz) { HashMap() }
        perClass[annotationType] = value
    }

    private fun <A : Annotation> findAnnotationInternal(
        clazz: Class<*>,
        annotationType: Class<A>,
        visitedClasses: MutableSet<Class<*>>,
        visitedAnnotationTypes: MutableSet<Class<out Annotation>>
    ): A? {
        // Prevent cycles in class/interface hierarchies
        if (!visitedClasses.add(clazz)) return null

        // 1) Directly present?
        clazz.getDeclaredAnnotation(annotationType)?.let { return it }

        // 2) Meta-annotations: scan annotations declared on this class
        for (ann in clazz.declaredAnnotations) {
            val annType = ann.annotationClass.java

            // Avoid infinite loops in meta-annotation graphs
            if (!visitedAnnotationTypes.add(annType)) continue

            // If the target annotation is directly present on the annotation type, return it
            annType.getDeclaredAnnotation(annotationType)?.let { return it }

            // Recurse into the annotation type (meta-meta-annotations etc.)
            // Skips deep traversal into very common Java/Kotlin meta-annotations (performance).
            if (!isFrameworkMetaAnnotation(annType)) {
                findAnnotationInternal(
                    clazz = annType,
                    annotationType = annotationType,
                    visitedClasses = visitedClasses,
                    visitedAnnotationTypes = visitedAnnotationTypes
                )?.let { return it }
            }
        }

        // 3) Interfaces
        for (ifc in clazz.interfaces) {
            findAnnotationInternal(
                clazz = ifc,
                annotationType = annotationType,
                visitedClasses = visitedClasses,
                visitedAnnotationTypes = visitedAnnotationTypes
            )?.let { return it }
        }

        // 4) Superclass
        val superclass = clazz.superclass ?: return null
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        if (superclass == Any::class.java || superclass == Object::class.java) return null

        return findAnnotationInternal(
            clazz = superclass,
            annotationType = annotationType,
            visitedClasses = visitedClasses,
            visitedAnnotationTypes = visitedAnnotationTypes
        )
    }

    private fun isFrameworkMetaAnnotation(type: Class<out Annotation>): Boolean {
        val name = type.name
        return name.startsWith("java.lang.annotation.") || name.startsWith("kotlin.")
    }

    fun clearCache() {
        cache.clear()
    }
}