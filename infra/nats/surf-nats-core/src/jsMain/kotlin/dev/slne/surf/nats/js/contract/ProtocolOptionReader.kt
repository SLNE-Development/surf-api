package dev.slne.surf.nats.js.contract

import dev.slne.surf.nats.js.external.protobuf.*
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.reflect
import dev.slne.surf.nats.js.interop.getOrPut
import dev.slne.surf.nats.js.interop.messageOf
import dev.slne.surf.nats.protocol.ProtocolOptions
import js.collections.WeakMap
import kotlin.collections.contains

/**
 * Reads the `surf.nats.v1` options of protobuf-es descriptors.
 *
 * The option extensions are looked up in the direct imports of the declaring file, so a file that uses surf
 * options must import `surf/nats/v1/options.proto` directly.
 */
internal object ProtocolOptionReader {

    private val extensionsByFile = WeakMap<DescFile, Map<String, DescExtension>>()

    fun event(schema: DescMessage): OptionValues {
        return read(schema, schema.file, ProtocolOptions.EVENT)
    }

    fun service(service: DescService): OptionValues {
        return read(service, service.file, ProtocolOptions.SERVICE)
    }

    fun method(method: DescMethod): OptionValues {
        return read(method, method.parent.file, ProtocolOptions.METHOD)
    }

    fun isNullable(field: DescField): Boolean {
        val extension = findExtension(field.parent.file, ProtocolOptions.NULLABLE) ?: return false

        return hasOption(field, extension) && getOption(field, extension) == true
    }

    private fun read(element: AnyDesc, file: DescFile, option: String): OptionValues {
        val extension = findExtension(file, option)
        if (extension == null || !hasOption(element, extension)) return OptionValues.UNSET

        return OptionValues(optionMessage(element, extension))
    }

    private fun findExtension(file: DescFile, option: String): DescExtension? {
        val extensions = extensionsByFile.getOrPut(file) {
            buildMap {
                for (dependency in file.dependencies) {
                    for (extension in dependency.extensions) {
                        if (extension.typeName !in this) {
                            put(extension.typeName, extension)
                        }
                    }
                }
            }
        }

        return extensions[ProtocolOptions.qualified(option)]
    }

    private fun optionMessage(element: AnyDesc, extension: DescExtension): ReflectMessage {
        val type = checkNotNull(extension.message) {
            "${extension.typeName} is not a message option"
        }

        val value = checkNotNull(messageOf(type, getOption(element, extension))) {
            "Option ${extension.typeName} is not a ${type.typeName} message"
        }

        return reflect(type, value)
    }
}
