package dev.slne.surf.nats.js.descriptor

import dev.slne.surf.nats.js.external.protobuf.*
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.reflect
import dev.slne.surf.nats.js.interop.getOrPut
import dev.slne.surf.nats.js.interop.messageOf
import dev.slne.surf.nats.protocol.MethodOptions
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.ServiceOptions
import js.collections.WeakMap

/**
 * Reads the `surf.nats.v1` options of protobuf-es descriptors.
 *
 * The option extensions are looked up in the direct imports of the declaring file, so a file that uses surf
 * options must import `surf/nats/v1/options.proto` directly.
 */
internal object ProtocolOptionReader {

    private val extensionsByFile = WeakMap<DescFile, Map<String, DescExtension>>()

    /**
     * Returns whether [schema] opts in as a surf-nats event with `(surf.nats.v1.event) = true`.
     */
    fun isEvent(schema: DescMessage): Boolean = flag(schema, schema.file, ProtocolOptions.EVENT)

    /**
     * Returns the `(surf.nats.v1.service)` option of [service]; unset fields are `null`.
     */
    fun service(service: DescService): ServiceOptions {
        val options = read(service, service.file, ProtocolOptions.SERVICE)
        return ServiceOptions(
            callTimeoutSeconds = options.int(ProtocolOptions.CALL_TIMEOUT_SECONDS),
            recipients = options.recipients(),
            persistent = options.boolean(ProtocolOptions.PERSISTENT),
        )
    }

    /**
     * Returns the `(surf.nats.v1.method)` option of [method]; unset fields are `null`.
     */
    fun method(method: DescMethod): MethodOptions {
        val options = read(method, method.parent.file, ProtocolOptions.METHOD)
        return MethodOptions(
            persistent = options.boolean(ProtocolOptions.PERSISTENT),
            callTimeoutSeconds = options.int(ProtocolOptions.CALL_TIMEOUT_SECONDS),
            recipients = options.recipients(),
        )
    }

    /**
     * Returns `call_timeout_seconds` of the `(surf.nats.v1.service)` option of [service] without a range check, or
     * `null` when unset.
     */
    fun serviceTimeoutSeconds(service: DescService): Double? =
        read(service, service.file, ProtocolOptions.SERVICE).number(ProtocolOptions.CALL_TIMEOUT_SECONDS)

    /**
     * Returns `call_timeout_seconds` of the `(surf.nats.v1.method)` option of [method] without a range check, or
     * `null` when unset.
     */
    fun methodTimeoutSeconds(method: DescMethod): Double? =
        read(method, method.parent.file, ProtocolOptions.METHOD).number(ProtocolOptions.CALL_TIMEOUT_SECONDS)

    fun isNullable(field: DescField): Boolean = flag(field, field.parent.file, ProtocolOptions.NULLABLE)

    private fun flag(element: AnyDesc, file: DescFile, option: String): Boolean {
        val extension = findExtension(file, option) ?: return false

        return hasOption(element, extension) && getOption(element, extension) == true
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
