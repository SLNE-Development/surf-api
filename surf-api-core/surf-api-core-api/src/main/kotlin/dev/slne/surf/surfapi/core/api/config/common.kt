package dev.slne.surf.surfapi.core.api.config

import org.intellij.lang.annotations.Language
import org.intellij.lang.annotations.Pattern

@Language("RegExp")
private const val YAML_CONFIG_FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+\\.(yml|yaml)$"

@Language("RegExp")
private const val JSON_CONFIG_FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+\\.(json)$"

@Pattern(YAML_CONFIG_FILE_NAME_PATTERN)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
@MustBeDocumented
annotation class YamlConfigFileNamePattern

@Pattern(JSON_CONFIG_FILE_NAME_PATTERN)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
@MustBeDocumented
annotation class JsonConfigFileNamePattern