package dev.slne.surf.api.gradle.internalapi

import java.io.File

/**
 * The binary names of the annotations that define what counts as internal API.
 */
internal object InternalApiMarkers {

    /**
     * The meta-annotation that turns an annotation class into an internal API marker.
     */
    const val MARKER = "dev.slne.surf.api.shared.api.annotation.InternalAPIMarker"

    /**
     * surf-api's marker for internal declarations that public `inline` functions reference.
     * Inlined plugin bytecode references them, so they are exempt.
     */
    const val PUBLISHED = "dev.slne.surf.api.shared.api.util.PublishedSurfApi"
}

/**
 * A reference from a project's own class to a declaration of a dependency that carries an
 * internal API marker.
 *
 * @property source the class or method that holds the reference.
 * @property target the referenced declaration.
 * @property markers the binary names of the internal API markers on [target].
 */
internal data class InternalApiViolation(
    val source: String,
    val target: String,
    val markers: List<String>,
) {
    override fun toString(): String {
        val markerNames = markers.joinToString { "@${it.substringAfterLast('.')}" }

        return "$source references $target ($markerNames)"
    }
}

/**
 * Finds references from a project's compiled classes to internal API of its dependencies.
 *
 * A declaration is internal API when it, or a class that encloses it, carries an annotation whose
 * class is annotated with [InternalApiMarkers.MARKER]. A declaration that also carries
 * [InternalApiMarkers.PUBLISHED] is exempt, and so are the markers in [allowedMarkers].
 * A member is checked in the class that declares it, which may be a superclass or an interface
 * of the class bytecode names as its owner; an internal owner of a dependency is reported too.
 * References to declarations of the project's own classes are never reported.
 *
 * @param index looks up the project's classes and its compile classpath.
 * @param allowedMarkers binary names of markers whose declarations the project may use.
 */
internal class InternalApiUsageScanner(
    private val index: ClassIndex,
    private val allowedMarkers: Set<String>,
) {
    private val markerCache = HashMap<String, Boolean>()

    /**
     * Returns the violations in the classes compiled into [roots], sorted by source.
     */
    fun scan(roots: List<File>): List<InternalApiViolation> {
        val ownClasses = roots.flatMap(::classNamesIn).toSortedSet()

        return ownClasses.flatMap { className ->
            val parsed = index.find(className) ?: return@flatMap emptyList()

            referencesBySource(parsed).flatMap { (source, references) ->
                references.mapNotNull { reference -> violationOf(source, reference, ownClasses) }
            }
        }.distinct()
    }

    private fun classNamesIn(root: File): List<String> {
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .map { it.relativeTo(root).invariantSeparatorsPath.removeSuffix(".class") }
            .toList()
    }

    private fun referencesBySource(parsed: ParsedClass): List<Pair<String, Set<Reference>>> {
        val className = parsed.name.replace('/', '.')
        val methodReferences = parsed.methods.map { (key, method) ->
            "$className.${key.substringBefore('(')}" to method.references
        }

        return listOf(className to parsed.declarationReferences) + methodReferences
    }

    /**
     * Checks the declaration [reference] resolves to and, if that passes, the class bytecode names
     * as its owner, unless that class is one of [ownClasses].
     */
    private fun violationOf(
        source: String,
        reference: Reference,
        ownClasses: Set<String>,
    ): InternalApiViolation? {
        val declaration = index.declarationOf(reference)
        if (declaration.owner in ownClasses) return null

        val declarationAnnotations = index.annotationsOf(declaration)
        val declarationViolation = violationAt(source, declaration, declarationAnnotations)
        if (declarationViolation != null) return declarationViolation
        if (declaration == reference || reference.owner in ownClasses) return null

        val receiverAnnotations = index.annotationsOf(Reference.Type(reference.owner))

        return violationAt(source, reference, receiverAnnotations)
    }

    private fun violationAt(
        source: String,
        target: Reference,
        annotations: Set<String>,
    ): InternalApiViolation? {
        if (InternalApiMarkers.PUBLISHED in annotations) return null

        val markers = annotations
            .filter { it !in allowedMarkers && isInternalApiMarker(it) }
            .sorted()
        if (markers.isEmpty()) return null

        return InternalApiViolation(source, target.toDisplay(), markers)
    }

    private fun isInternalApiMarker(annotation: String): Boolean {
        return markerCache.getOrPut(annotation) {
            val annotationClass = index.find(annotation.replace('.', '/'))
            annotationClass != null && InternalApiMarkers.MARKER in annotationClass.annotations
        }
    }
}
