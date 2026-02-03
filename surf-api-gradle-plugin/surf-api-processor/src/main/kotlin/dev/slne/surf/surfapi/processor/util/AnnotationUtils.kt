package dev.slne.surf.surfapi.processor.util

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration

object AnnotationUtils {
    private const val MAX_RECURSION_DEPTH = 25

    data class FoundAnnotation(
        val annotation: KSAnnotation,
        val distance: Int
    )

    fun findAllByDistance(
        annotated: KSAnnotated,
        targetAnnotationFqName: String,
        maxDepth: Int = MAX_RECURSION_DEPTH
    ): List<FoundAnnotation> {
        val results = mutableListOf<FoundAnnotation>()
        val q: ArrayDeque<Pair<KSAnnotated, Int>> = ArrayDeque()
        q.add(annotated to 0)

        val visitedAnnDecls = mutableSetOf<String>()

        while (q.isNotEmpty()) {
            val (current, dist) = q.removeFirst()
            if (dist > maxDepth) continue

            for (ann in current.annotations) {
                val decl = ann.annotationType.resolve().declaration as? KSClassDeclaration ?: continue
                val fq = decl.qualifiedName?.asString() ?: continue

                if (fq == targetAnnotationFqName) {
                    results += FoundAnnotation(ann, dist)
                    continue
                }

                if (fq.startsWith("kotlin.") || fq.startsWith("java.")) continue

                if (visitedAnnDecls.add(fq)) {
                    q.add(decl to (dist + 1))
                }
            }
        }

        return results
    }

    fun findAnnotationRecursive(
        annotated: KSAnnotated,
        annotationFqName: String,
        tieBreaker: (List<KSAnnotation>) -> KSAnnotation? = { it.firstOrNull() }
    ): KSAnnotation? {
        val found = findAllByDistance(annotated, annotationFqName)
        if (found.isEmpty()) return null

        val bestDistance = found.minOf { it.distance }
        val bestLevel = found
            .filter { it.distance == bestDistance }
            .map { it.annotation }

        return tieBreaker(bestLevel)
    }

    fun findAllAnnotationsRecursive(
        annotated: KSAnnotated,
        annotationFqName: String
    ): List<KSAnnotation> = findAllByDistance(annotated, annotationFqName)
        .sortedBy { it.distance }
        .map { it.annotation }

    fun findMetaAnnotations(
        resolver: Resolver,
        baseAnnotation: String,
        visited: MutableSet<String> = mutableSetOf(),
        depth: Int = 0,
        logger: KSPLogger? = null
    ): Set<String> {
        if (!visited.add(baseAnnotation)) {
            return emptySet()
        }

        if (depth >= MAX_RECURSION_DEPTH) {
            logger?.warn("Max meta-annotation depth reached for $baseAnnotation")
            return emptySet()
        }

        val metaAnnotations = mutableSetOf<String>()

        resolver.getSymbolsWithAnnotation(baseAnnotation)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.ANNOTATION_CLASS }
            .forEach { annotationClass ->
                annotationClass.qualifiedName?.asString()?.let { fqName ->
                    metaAnnotations.add(fqName)
                    metaAnnotations.addAll(
                        findMetaAnnotations(resolver, fqName, visited, depth + 1, logger)
                    )
                }
            }

        return metaAnnotations
    }

    fun findAnnotatedClasses(
        resolver: Resolver,
        annotationFqName: String,
        includeMetaAnnotations: Boolean = true,
        excludeAnnotationClasses: Boolean = true,
        logger: KSPLogger? = null
    ): Sequence<KSClassDeclaration> {
        val candidates = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .let { seq ->
                if (!excludeAnnotationClasses) seq
                else seq.filterNot { it.classKind == ClassKind.ANNOTATION_CLASS }
            }

        return if (!includeMetaAnnotations) {
            candidates.filter { decl ->
                decl.annotations.any { ann ->
                    (ann.annotationType.resolve().declaration as? KSClassDeclaration)
                        ?.qualifiedName?.asString() == annotationFqName
                }
            }
        } else {
            candidates.filter { decl ->
                AnnotationUtils.findAnnotationRecursive(decl, annotationFqName) != null
            }
        }
    }
}

fun KSAnnotated.findAnnotationRecursive(annotationFqName: String): KSAnnotation? {
    return AnnotationUtils.findAnnotationRecursive(this, annotationFqName)
}

fun KSAnnotated.findAllAnnotationsRecursive(annotationFqName: String): List<KSAnnotation> {
    return AnnotationUtils.findAllAnnotationsRecursive(this, annotationFqName)
}

fun KSAnnotation.getArgumentValue(name: String): Any? {
    return arguments.find { it.name?.asString() == name }?.value
}

inline fun <reified T> KSAnnotation.getArgumentValueAs(name: String): T? {
    return getArgumentValue(name) as? T
}

inline fun <reified T> List<KSAnnotation>.collectArgumentValues(argumentName: String = "value"): List<T> {
    return this.flatMap { annotation ->
        when (val value = annotation.getArgumentValue(argumentName)) {
            is List<*> -> value.filterIsInstance<T>()
            is T -> listOf(value)
            else -> emptyList()
        }
    }
}

inline fun <reified T> List<KSAnnotation>.collectArgumentValuesGrouped(argumentName: String = "value"): List<List<T>> {
    return this.mapNotNull { annotation ->
        when (val value = annotation.getArgumentValue(argumentName)) {
            is List<*> -> value.filterIsInstance<T>().takeIf { it.isNotEmpty() }
            is Array<*> -> value.filterIsInstance<T>().toList().takeIf { it.isNotEmpty() }
            is T -> listOf(value)
            else -> null
        }
    }
}