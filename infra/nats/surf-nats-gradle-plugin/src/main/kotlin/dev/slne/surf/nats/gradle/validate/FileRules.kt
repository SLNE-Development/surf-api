package dev.slne.surf.nats.gradle.validate

import com.squareup.wire.Syntax
import com.squareup.wire.schema.Extend
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Type
import dev.slne.surf.nats.gradle.model.SurfOptions
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolOptions

/**
 * Per-file facts the generator derives before emitting code.
 */
internal class FileFacts(val file: ProtoFile, val kotlinPackage: String)

/**
 * File-level rules: syntax, version suffix, Kotlin package hygiene, options import and unsupported extensions.
 */
internal class FileRules(private val problems: Problems) {
    private val packageOwners = HashMap<String, Pair<String, Location>>()

    fun check(file: ProtoFile): FileFacts {
        val location = file.location
        if (file.syntax == Syntax.PROTO_3) {
            problems.error(location, "only syntax = \"proto2\" is supported")
        }

        val protoPackage = file.packageName.orEmpty()
        val kotlinPackage = kotlinPackage(file)
        val majorVersion = ProtocolNaming.majorVersionOrNull(protoPackage)

        val declaresContracts = file.services.isNotEmpty() || file.types.any(::declaresEvent)
        if (declaresContracts) {
            ProtocolNaming.contractPackageProblem(protoPackage)?.let { problems.error(location, it) }
        }
        if (majorVersion != null && kotlinPackage.substringAfterLast('.') != "v$majorVersion") {
            problems.error(location, "Kotlin package '$kotlinPackage' of proto package '$protoPackage' must end with '.v$majorVersion'")
        }

        val owner = packageOwners.putIfAbsent(kotlinPackage, protoPackage to location)
        if (owner != null && owner.first != protoPackage) {
            problems.error(
                location,
                "proto package '$protoPackage' resolves to the Kotlin package '$kotlinPackage', which proto package " +
                        "'${owner.first}' (${owner.second}) already uses; give each proto package its own java_package",
            )
        }

        if (usesSurfOptions(file) && ProtocolOptions.PROTO_FILE !in file.imports && ProtocolOptions.PROTO_FILE !in file.publicImports) {
            problems.error(location, "files using surf-nats options must import \"${ProtocolOptions.PROTO_FILE}\" directly")
        }

        for (extend in file.extendList + file.types.flatMap(::nestedExtends)) checkExtend(extend)
        return FileFacts(file, kotlinPackage)
    }

    private fun checkExtend(extend: Extend) {
        val type = extend.type?.toString() ?: return
        if (!type.startsWith(ProtocolFieldRules.WELL_KNOWN_PREFIX) || !type.endsWith("Options")) {
            problems.error(extend.location, "extensions of $type are not supported")
        }
    }

    private fun nestedExtends(type: Type): List<Extend> = type.nestedExtendList + type.nestedTypes.flatMap(::nestedExtends)

    private fun declaresEvent(type: Type): Boolean {
        return (type is MessageType && SurfOptions.isEvent(type.options)) || type.nestedTypes.any(::declaresEvent)
    }

    private fun usesSurfOptions(file: ProtoFile): Boolean {
        return file.types.any(::usesSurfOptions) ||
                file.services.any { service ->
                    SurfOptions.isSet(service.options, SurfOptions.SERVICE) ||
                            service.rpcs.any { SurfOptions.isSet(it.options, SurfOptions.METHOD) }
                }
    }

    private fun usesSurfOptions(type: Type): Boolean {
        val own = type is MessageType && (
                SurfOptions.isSet(type.options, SurfOptions.EVENT) ||
                        type.fieldsAndOneOfFields.any { SurfOptions.isSet(it.options, SurfOptions.NULLABLE) })
        return own || type.nestedTypes.any(::usesSurfOptions)
    }

    companion object {
        /**
         * Returns the Kotlin package of [file]: its `java_package`, else its proto package.
         */
        fun kotlinPackage(file: ProtoFile): String = file.javaPackage() ?: file.packageName.orEmpty()
    }
}
