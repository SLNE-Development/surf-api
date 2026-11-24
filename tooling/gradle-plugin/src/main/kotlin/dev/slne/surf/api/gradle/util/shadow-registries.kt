package dev.slne.surf.api.gradle.util

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer.MergeStrategy
import org.gradle.api.file.DuplicatesStrategy

private const val SERVICES_PATTERN = "META-INF/services/**"
private const val SPRING_DIRECTORY = "META-INF/spring"
private const val SPRING_IMPORTS_PATTERN = "$SPRING_DIRECTORY/*.imports"
private const val LOG4J2_PLUGIN_CACHE =
    "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"

/**
 * The Spring registries in properties format whose keys list several values.
 */
private val SPRING_LIST_REGISTRIES = listOf(
    "META-INF/spring.factories",
    "$SPRING_DIRECTORY/aot.factories",
)

/**
 * The Spring registries in properties format whose keys map to exactly one value. The spring-aop,
 * spring-beans and spring-context jars each ship their own `spring.handlers` and `spring.schemas`.
 */
private val SPRING_SINGLE_VALUE_REGISTRIES = listOf(
    "META-INF/spring.handlers",
    "META-INF/spring.schemas",
)

private val SPRING_PROPERTIES_REGISTRIES = SPRING_LIST_REGISTRIES + SPRING_SINGLE_VALUE_REGISTRIES

/**
 * Merges the registries that Spring and the surf modules extend from many jars:
 * `META-INF/services`, the `.imports` files in `META-INF/spring`, and the properties files
 * `spring.factories`, `spring.handlers`, `spring.schemas` and `spring/aot.factories`. It also
 * merges the log4j plugin cache `Log4j2Plugins.dat`.
 *
 * - Service files and `.imports` files are merged line by line, so every entry of every jar
 *   survives once.
 * - The properties files are merged per key. In `spring.factories` and `aot.factories` the values
 *   of a key are appended with `,`, so the keys that Boot and the surf modules both define, such as
 *   `org.springframework.context.ApplicationContextInitializer`, keep the values of every jar. In
 *   `spring.handlers` and `spring.schemas` a key maps to one class or resource, so the first value
 *   is kept.
 * - The log4j plugin caches of log4j-core and of every other jar that ships log4j plugins, such as
 *   Spring Boot, are combined into one. With only one of them in the jar, log4j does not know its
 *   own appenders and layouts, finds no configuration, and logs nothing.
 *
 * The transformers only see every copy of a file if the duplicates reach them instead of being
 * dropped first, so the files they handle are included regardless of the duplicates strategy of the
 * task. Every other duplicate keeps following it.
 *
 * Call it only from code that runs after `com.gradleup.shadow` is applied, so builds without Shadow
 * never load this file's class.
 */
internal fun ShadowJar.mergeRegistryFiles() {
    mergeServiceFiles()
    mergeServiceFiles {
        path = SPRING_DIRECTORY
        setIncludes(listOf(SPRING_IMPORTS_PATTERN))
    }
    mergePropertiesFiles(SPRING_LIST_REGISTRIES, MergeStrategy.Append)
    mergePropertiesFiles(SPRING_SINGLE_VALUE_REGISTRIES, MergeStrategy.First)

    transform(Log4j2PluginsCacheFileTransformer::class.java)

    filesMatching(
        listOf(
            SERVICES_PATTERN,
            SPRING_IMPORTS_PATTERN,
            LOG4J2_PLUGIN_CACHE,
        ) + SPRING_PROPERTIES_REGISTRIES,
    ) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

private fun ShadowJar.mergePropertiesFiles(files: List<String>, strategy: MergeStrategy) {
    transform<PropertiesFileTransformer> {
        paths.set(files.map { it.asAnchoredRegex() })
        mergeStrategy.set(strategy)
    }
}

private fun String.asAnchoredRegex(): String = "^${Regex.escape(this)}$"
