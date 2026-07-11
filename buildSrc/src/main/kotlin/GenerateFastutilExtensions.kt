import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class GenerateFastutilExtensions : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val templates = FastutilTemplates(templateDirectory.get().asFile)
        val outputFile = outputDirectory.file(
            "dev/slne/surf/api/core/util/fast-util-util.kt"
        ).get().asFile
        val content = templates.renderSource()

        outputFile.parentFile.mkdirs()
        if (!outputFile.exists() || outputFile.readText() != content) {
            outputFile.writeText(content)
        }
    }
}

private class FastutilTemplates(private val directory: File) {
    private data class PrimitiveType(val name: String) {
        val lowerName = name.replaceFirstChar(Char::lowercaseChar)
        val arrayType = "${name}Array"
        val arrayConversion = "to${name}Array()"

        fun variables() = mapOf(
            "NAME" to name,
            "LOWER_NAME" to lowerName,
            "ARRAY_TYPE" to arrayType,
            "ARRAY_CONVERSION" to arrayConversion,
        )
    }

    private val primitiveTypes = listOf(
        PrimitiveType("Boolean"),
        PrimitiveType("Byte"),
        PrimitiveType("Char"),
        PrimitiveType("Short"),
        PrimitiveType("Int"),
        PrimitiveType("Long"),
        PrimitiveType("Float"),
        PrimitiveType("Double"),
    )

    // Fastutil has no Boolean2* map family.
    private val primitiveMapKeyTypes = primitiveTypes.filterNot { it.name == "Boolean" }

    fun renderSource(): String = buildString {
        appendTemplate("header")
        appendTemplate("object-set")
        primitiveTypes.forEach { appendTemplate("primitive-set", it.variables()) }
        appendLine("// endregion")
        appendLine()

        appendLine("// region List")
        appendTemplate("object-list")
        primitiveTypes.forEach { appendTemplate("primitive-list", it.variables()) }
        appendLine("// endregion")
        appendLine()

        appendLine("// region Map")
        appendTemplate("object-map")
        primitiveTypes.forEach { appendTemplate("object-to-primitive-map", it.variables()) }
        primitiveMapKeyTypes.forEach { key ->
            primitiveTypes.forEach { value ->
                appendTemplate(
                    "primitive-map",
                    mapOf(
                        "KEY_NAME" to key.name,
                        "KEY_LOWER_NAME" to key.lowerName,
                        "VALUE_NAME" to value.name,
                    )
                )
            }
            appendTemplate("primitive-to-object-map", key.variables())
        }
        appendLine("// endregion")
    }

    private fun StringBuilder.appendTemplate(
        name: String,
        variables: Map<String, String> = emptyMap(),
    ) {
        var rendered = directory.resolve("$name.kt.template").readText().trimEnd()
        for ((key, value) in variables) {
            rendered = rendered.replace("{{$key}}", value)
        }

        val unresolved = PLACEHOLDER.find(rendered)
        require(unresolved == null) {
            "Unresolved placeholder ${unresolved?.value} in $name.kt.template"
        }

        appendLine(rendered)
        appendLine()
    }

    private companion object {
        val PLACEHOLDER = Regex("\\{\\{[A-Z_]+}}")
    }
}
