
package dev.slne.surf.nats.gradle.validate

import com.squareup.wire.Syntax
import com.squareup.wire.schema.Extend
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Type
import dev.slne.surf.nats.gradle.model.SurfProtoOptions
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolOptions

/**
 * Returns the Kotlin package of this file, preferring `java_package` over
 * the declared proto package.
 */
internal val ProtoFile.kotlinPackage: String
    get() = javaPackage() ?: packageName.orEmpty()

/**
 * Validates file-level proto rules, including syntax, package naming,
 * option imports, and extensions.
 *
 * Tracks Kotlin package ownership across checked files to detect conflicts
 * between different proto packages.
 */
internal class FileRules(private val problems: Problems) {

    private val packageOwners = HashMap<String, Pair<String, Location>>()

    fun check(file: ProtoFile) {
        checkSyntax(file)
        checkPackages(file)
        checkOptionsImport(file)
        checkExtensions(file)
    }

    private fun checkSyntax(file: ProtoFile) {
        if (file.syntax == Syntax.PROTO_3) {
            problems.error(file.location, "only syntax = \"${Syntax.PROTO_2}\" is supported")
        }
    }

    private fun checkPackages(file: ProtoFile) {
        val protoPackage = file.packageName.orEmpty()
        val kotlinPackage = file.kotlinPackage
        val majorVersion = ProtocolNaming.majorVersionOrNull(protoPackage)

        val declaresContracts = file.services.isNotEmpty() ||
                file.types.any(::declaresEvent)

        if (declaresContracts) {
            ProtocolNaming.contractPackageProblem(protoPackage)?.let {
                problems.error(file.location, it)
            }
        }

        if (majorVersion != null && kotlinPackage.substringAfterLast('.') != "v$majorVersion") {
            problems.error(
                file.location,
                "Kotlin package '$kotlinPackage' of proto package '$protoPackage' must end with '.v$majorVersion'",
            )
        }

        checkPackageOwnership(protoPackage, kotlinPackage, file.location)
    }

    /**
     * Ensures that different proto packages do not resolve to the same
     * Kotlin package. The first declaration determines the package owner.
     */
    private fun checkPackageOwnership(
        protoPackage: String,
        kotlinPackage: String,
        location: Location,
    ) {
        val owner = packageOwners.putIfAbsent(
            kotlinPackage,
            protoPackage to location,
        )

        if (owner != null && owner.first != protoPackage) {
            problems.error(
                location,
                "proto package '$protoPackage' resolves to the Kotlin package '$kotlinPackage', which proto package " +
                        "'${owner.first}' (${owner.second}) already uses; give each proto package its own java_package",
            )
        }
    }

    private fun checkOptionsImport(file: ProtoFile) {
        if (
            usesSurfOptions(file) &&
            ProtocolOptions.PROTO_FILE !in file.imports &&
            ProtocolOptions.PROTO_FILE !in file.publicImports
        ) {
            problems.error(
                file.location,
                "files using surf-nats options must import \"${ProtocolOptions.PROTO_FILE}\" directly",
            )
        }
    }

    private fun checkExtensions(file: ProtoFile) {
        val extensions = file.extendList + file.types.flatMap(::nestedExtends)

        for (extension in extensions) {
            checkExtension(extension)
        }
    }

    private fun checkExtension(extension: Extend) {
        val type = extension.type?.toString() ?: return

        if (
            !type.startsWith(ProtocolFieldRules.WELL_KNOWN_PREFIX) ||
            !type.endsWith("Options")
        ) {
            problems.error(
                extension.location,
                "extensions of $type are not supported",
            )
        }
    }

    private fun nestedExtends(type: Type): List<Extend> {
        return type.nestedExtendList + type.nestedTypes.flatMap(::nestedExtends)
    }

    private fun declaresEvent(type: Type): Boolean {
        return (type is MessageType && SurfProtoOptions.isEvent(type.options)) ||
                type.nestedTypes.any(::declaresEvent)
    }

    private fun usesSurfOptions(file: ProtoFile): Boolean {
        return file.types.any(::usesSurfOptions) ||
                file.services.any { service ->
                    SurfProtoOptions.isSet(service.options, SurfProtoOptions.SERVICE) ||
                            service.rpcs.any { rpc ->
                                SurfProtoOptions.isSet(rpc.options, SurfProtoOptions.METHOD)
                            }
                }
    }

    private fun usesSurfOptions(type: Type): Boolean {
        val usesOwnOptions =
            type is MessageType && SurfProtoOptions.isSet(type.options, SurfProtoOptions.EVENT)
        return usesOwnOptions || type.nestedTypes.any(::usesSurfOptions)
    }
}
