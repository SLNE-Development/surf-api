package dev.slne.surf.api.minestom.plugin.annotation

import dev.slne.surf.api.minestom.plugin.MinestomPlugin
import org.intellij.lang.annotations.Pattern

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class MinestomPluginMeta(
    @MinestomPluginIdPattern
    val id: String,
    val dependsOn: Array<@MinestomPluginIdPattern String> = []
) {

    companion object {
        private const val ID_PATTERN_STRING = "[a-z0-9][a-z0-9._-]*"

        @Pattern(ID_PATTERN_STRING)
        @Retention(AnnotationRetention.BINARY)
        @Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
        annotation class MinestomPluginIdPattern

        val ID_PATTERN = Regex(ID_PATTERN_STRING)

        internal fun get(clazz: Class<out MinestomPlugin>): MinestomPluginMeta {
            val annotation = clazz.getDeclaredAnnotation(MinestomPluginMeta::class.java)
            requireNotNull(annotation) {
                "Class ${clazz.name} is not annotated with @MinestomPluginMeta"
            }
            return annotation
        }

        internal fun validate(meta: MinestomPluginMeta) {
            require(ID_PATTERN.matches(meta.id)) {
                "Invalid Minestom plugin id '${meta.id}'. " +
                        "Expected lowercase characters, digits, '.', '_' or '-'."
            }

            require(meta.id !in meta.dependsOn) {
                "Minestom plugin '${meta.id}' must not depend on itself"
            }
        }
    }
}
