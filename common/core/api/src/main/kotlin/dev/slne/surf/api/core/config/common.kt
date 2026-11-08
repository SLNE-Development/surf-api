package dev.slne.surf.api.core.config

import org.intellij.lang.annotations.Language
import org.intellij.lang.annotations.Pattern

@Language("RegExp")
private const val YAML_CONFIG_FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+\\.(yml|yaml)$"

@Language("RegExp")
private const val JSON_CONFIG_FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+\\.(json)$"

/**
 * Annotation to specify that a file name must follow a YAML configuration file name pattern.
 * The file name must match the regular expression: `^[a-zA-Z0-9_-]+\\.(yml|yaml)$`.
 */
@Pattern(YAML_CONFIG_FILE_NAME_PATTERN)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
@MustBeDocumented
annotation class YamlConfigFileNamePattern

/**
 * Annotation to specify that a file name must follow a JSON configuration file name pattern.
 * The file name must match the regular expression: `^[a-zA-Z0-9_-]+\\.(json)$`.
 */
@Pattern(JSON_CONFIG_FILE_NAME_PATTERN)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
@MustBeDocumented
annotation class JsonConfigFileNamePattern