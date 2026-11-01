package dev.slne.surf.nats.gradle.compat

import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.model.*
import dev.slne.surf.nats.gradle.schema.LoadedSchema
import dev.slne.surf.nats.gradle.schema.containsProtos
import dev.slne.surf.nats.gradle.schema.loadSchema
import dev.slne.surf.nats.gradle.validate.FileRules
import dev.slne.surf.nats.gradle.validate.Problems
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ResolvedMethod
import dev.slne.surf.nats.protocol.ResolvedService
import java.io.File

/**
 * Checks whether changes to Protobuf sources are compatible with a published baseline.
 *
 * Types and services are matched by their fully qualified Protobuf names. Renaming an
 * element or moving it to another package is therefore treated as removing it.
 *
 * In addition to wire compatibility, this checker validates Kotlin package changes,
 * removed reservations, event configuration, and resolved RPC options. Changes to
 * call timeouts are intentionally excluded.
 */
object CompatibilityChecker {

    /**
     * Loads the baseline and current Protobuf sources and compares their declarations.
     *
     * Both schemas are resolved using the bundled Protobuf definitions and [protoPathDirs].
     * If [currentDirs] contain no Protobuf files, all baseline declarations are treated
     * as removed.
     *
     * @param baselineDirs directories containing the published baseline sources
     * @param currentDirs directories containing the current sources
     * @param protoPathDirs additional directories used to resolve Protobuf imports
     * @return a report of compatibility violations and warnings
     * @throws dev.slne.surf.nats.gradle.generator.SurfNatsGeneratorException if a schema cannot be loaded
     */
    fun check(
        baselineDirs: List<File>,
        currentDirs: List<File>,
        protoPathDirs: List<File>,
    ): CompatibilityReport {
        val baseline = loadSchema(baselineDirs, protoPathDirs)
        val current = if (currentDirs.any { it.containsProtos() }) {
            loadSchema(currentDirs, protoPathDirs)
        } else {
            null
        }

        return compare(baseline, current)
    }

    /**
     * Compares two already loaded schemas without loading files again.
     *
     * A null [current] schema represents a current version without any declarations.
     */
    internal fun compare(baseline: LoadedSchema, current: LoadedSchema?): CompatibilityReport {
        return Comparison(current?.sourceFiles.orEmpty()).run(baseline.sourceFiles)
    }
}

/** Collects compatibility findings while comparing baseline declarations to current ones. */
private class Comparison(currentFiles: List<ProtoFile>) {
    private val violations = mutableListOf<CompatibilityFinding>()
    private val warnings = mutableListOf<CompatibilityFinding>()

    // Option parsing errors are reported by the generator, not by compatibility checking.
    private val ignoredProblems = Problems()
    private val rawDefaults = RawDefaults()

    private val currentTypes: Map<String, Type> = currentFiles
        .flatMap { it.typesAndNestedTypes() }
        .associateBy { it.type.toString() }

    private val currentServices: Map<String, Service> = currentFiles
        .flatMap { it.services }
        .associateBy { it.type.toString() }

    /** Maps each top-level type and service to its current Kotlin package. */
    private val currentPackages: Map<String, String> = buildMap {
        for (file in currentFiles) {
            val kotlinPackage = FileRules.kotlinPackage(file)

            for (type in file.types) {
                put(type.type.toString(), kotlinPackage)
            }

            for ((type) in file.services) {
                put(type.toString(), kotlinPackage)
            }
        }
    }

    /** Compares all baseline files and returns the collected violations and warnings. */
    fun run(baselineFiles: List<ProtoFile>): CompatibilityReport {
        for (file in baselineFiles) {
            compareKotlinPackage(file)

            for (type in file.typesAndNestedTypes()) {
                compareType(type)
            }

            for (service in file.services) {
                compareService(service)
            }
        }

        return CompatibilityReport(violations.toList(), warnings.toList())
    }

    /** Reports Kotlin package changes for existing top-level types and services. */
    private fun compareKotlinPackage(file: ProtoFile) {
        val baselinePackage = FileRules.kotlinPackage(file)
        val names = file.types.map { it.type.toString() } +
                file.services.map { it.type.toString() }

        for (name in names) {
            val currentPackage = currentPackages[name] ?: continue
            if (currentPackage == baselinePackage) continue

            val location = currentTypes[name]?.location ?: currentServices.getValue(name).location
            changed(
                location,
                name,
                "java_package changed the Kotlin package from '$baselinePackage' to '$currentPackage'",
            )
        }
    }

    /** Checks whether a baseline type still exists and retains its message or enum kind. */
    private fun compareType(baseline: Type) {
        val name = baseline.type.toString()
        val current = currentTypes[name]

        when {
            current == null -> removed(
                baseline.location,
                name,
                "${kind(baseline)} $name was removed or renamed",
            )

            baseline is MessageType && current is MessageType -> compareMessage(baseline, current)
            baseline is EnumType && current is EnumType -> compareEnum(baseline, current)
            else -> changed(
                current.location,
                name,
                "changed from ${kind(baseline)} to ${kind(current)}"
            )
        }
    }

    /**
     * Compares message fields and the message-level compatibility requirements.
     *
     * Existing fields are matched by name. Newly required fields, removed fields,
     * changed field definitions, dropped reservations, and event changes are reported.
     * Constructor parameter moves are reported separately as warnings.
     */
    private fun compareMessage(baseline: MessageType, current: MessageType) {
        val name = baseline.type.toString()
        val baselineFields = baseline.fieldsAndOneOfFields.associateBy { it.name }
        val currentFields = current.fieldsAndOneOfFields.associateBy { it.name }
        val currentReservations = Reserved.fromElements(current.toElement().reserveds)

        for ((fieldName, before) in baselineFields) {
            val element = "$name.$fieldName"
            val after = currentFields[fieldName]

            if (after == null) {
                removedField(baseline, before, current, currentReservations, element)
                continue
            }

            diff(after.location, element, "field number", before.tag, after.tag)
            diff(after.location, element, "type", typeName(before.type), typeName(after.type))
            diff(after.location, element, "label", label(baseline, before), label(current, after))
            diff(
                after.location,
                element,
                "(surf.nats.v1.nullable)",
                nullable(before),
                nullable(after)
            )

            val defaultBefore = normalizedDefault(before)
            val defaultAfter = normalizedDefault(after)

            when {
                defaultBefore == defaultAfter -> Unit
                defaultBefore == null -> changed(
                    after.location,
                    element,
                    "[default = ${after.default}] was added"
                )

                defaultAfter == null -> changed(
                    after.location,
                    element,
                    "[default = ${before.default}] was removed"
                )

                else -> changed(
                    after.location,
                    element,
                    "[default] changed from ${before.default} to ${after.default}"
                )
            }
        }

        for ((fieldName, field) in currentFields) {
            if (fieldName in baselineFields || !isRequired(field)) continue

            changed(
                field.location,
                "$name.$fieldName",
                "new field '$fieldName' = ${field.tag} is non-null without a [default], so readers fail on messages of " +
                        "senders that do not know it; mark it [(surf.nats.v1.nullable) = true] or give it a [default]",
            )
        }

        compareReservations(
            name,
            Reserved.fromElements(baseline.toElement().reserveds),
            currentReservations,
            current.location,
        )
        compareEvent(baseline, current)
        compareParameterOrder(baseline, current)
    }

    /**
     * Warns when existing Kotlin constructor parameters change their positions.
     *
     * Parameters are ordered by field number. A oneof uses its lowest case number.
     * Adding a lower-numbered field or oneof case can therefore shift existing
     * positional arguments even when the constructor call still compiles.
     */
    private fun compareParameterOrder(baseline: MessageType, current: MessageType) {
        val before = parameterOrder(baseline)
        val after = parameterOrder(current)
        val positionsAfter = after.withIndex()
            .associate { (index, parameter) -> parameter.key to index }

        val moves = before.withIndex().mapNotNull { (index, parameter) ->
            val newIndex = positionsAfter[parameter.key] ?: return@mapNotNull null

            if (newIndex == index) {
                null
            } else {
                "'${parameter.kotlinName}' moves from position ${index + 1} to ${newIndex + 1}"
            }
        }
        if (moves.isEmpty()) return

        val causes = mutableListOf<Pair<Location, String>>()
        val baselineMax = baseline.fieldsAndOneOfFields.maxOfOrNull { it.tag } ?: 0
        val beforeKeys = before.associateBy { it.key }

        for (parameter in after) {
            val previous = beforeKeys[parameter.key]

            when {
                previous == null && parameter.number < baselineMax -> {
                    val kind = if (parameter.oneofCases != null) "new oneof" else "new field"
                    causes += parameter.location to
                            "$kind '${parameter.protoName}' = ${parameter.number} is numbered below the baseline maximum $baselineMax"
                }

                previous?.oneofCases != null && parameter.oneofCases != null -> {
                    val lowest = previous.number

                    for (case in parameter.oneofCases.values) {
                        if (case.name !in previous.oneofCases && case.tag < lowest) {
                            causes += case.location to
                                    "new oneof case '${case.name}' = ${case.tag} is numbered below the lowest " +
                                    "baseline case $lowest of oneof '${parameter.protoName}'"
                        }
                    }
                }
            }
        }

        for (parameter in before) {
            if (parameter.key !in positionsAfter) {
                causes += current.location to "'${parameter.protoName}' was removed"
            }
        }

        val location = causes.firstOrNull()?.first ?: current.location
        val cause = causes.joinToString("; ") { it.second }
            .ifEmpty { "the parameter order changed" }

        warnings += CompatibilityFinding(
            location.withoutBase().toString(),
            current.type.toString(),
            "$cause, so positional arguments of the constructor of ${current.type.simpleName} shift: ${
                moves.joinToString(
                    ", "
                )
            }. Named arguments are unaffected; " +
                    "new fields always get a number above the current maximum",
        )
    }

    /** Returns the generated constructor parameters in field-number order. */
    private fun parameterOrder(type: MessageType): List<OrderedParameter> = orderedMembers(
        type,
        field = { field ->
            OrderedParameter(
                "field:${field.name}",
                field.name,
                KotlinNaming.lowerCamel(field.name),
                field.tag,
                field.location,
                null,
            )
        },
        oneof = { oneOf, number ->
            OrderedParameter(
                "oneof:${oneOf.name}",
                oneOf.name,
                KotlinNaming.lowerCamel(oneOf.name),
                number,
                oneOf.location,
                oneOf.fields.associateBy { it.name },
            )
        },
    )

    /**
     * Describes a generated Kotlin constructor parameter.
     *
     * [oneofCases] is null for ordinary fields and contains the cases for a oneof.
     */
    private class OrderedParameter(
        val key: String,
        val protoName: String,
        val kotlinName: String,
        val number: Int,
        val location: Location,
        val oneofCases: Map<String, Field>?,
    )

    /** Reports reserved names or numbers that are no longer reserved in the current type. */
    private fun compareReservations(
        name: String,
        before: List<Reserved>,
        after: List<Reserved>,
        location: Location,
    ) {
        val currentValues = after.flatMap { it.values }
        val currentRanges = currentValues.mapNotNull { value ->
            when (value) {
                is Int -> value..value
                is IntRange -> value
                else -> null
            }
        }.sortedBy { it.first }
        val currentNames = currentValues.filterIsInstance<String>().toSet()

        for (value in before.flatMap { it.values }) {
            val dropped = when (value) {
                is Int -> if (covers(currentRanges, value..value)) null else "$value"
                is IntRange -> if (covers(
                        currentRanges,
                        value
                    )
                ) null else "${value.first} to ${value.last}"

                is String -> if (value in currentNames) null else "\"$value\""
                else -> null
            }

            if (dropped != null) {
                changed(
                    location,
                    name,
                    "reserved $dropped is no longer reserved; reservations must be kept"
                )
            }
        }
    }

    /** Returns whether the sorted [ranges] together cover the entire requested [range]. */
    private fun covers(ranges: List<IntRange>, range: IntRange): Boolean {
        var next = range.first.toLong()

        for (candidate in ranges) {
            if (candidate.first > next) break
            if (candidate.last >= next) {
                next = candidate.last.toLong() + 1
            }
            if (next > range.last) return true
        }

        return next > range.last
    }

    /**
     * Reports a removed field if it was required or its name or number was not reserved.
     *
     * The message includes the missing reservations when applicable.
     */
    private fun removedField(
        baseline: MessageType,
        field: Field,
        current: MessageType,
        reserved: List<Reserved>,
        element: String,
    ) {
        val reasons = mutableListOf<String>()

        if (isRequired(field)) {
            reasons += "it was non-null without a [default], so readers of the baseline fail on messages without it"
        }

        val missingReserve = listOfNotNull(
            field.tag.toString().takeUnless { reserved.any { it.matchesTag(field.tag) } },
            "\"${field.name}\"".takeUnless { reserved.any { it.matchesName(field.name) } },
        )

        if (missingReserve.isNotEmpty()) {
            reasons += "add ${missingReserve.joinToString(" ") { "`reserved $it;`" }} to ${current.name}"
        }
        if (reasons.isEmpty()) return

        removed(
            field.location,
            element,
            "field '${field.name}' = ${field.tag} was removed: ${reasons.joinToString("; ")}",
        )
    }

    /** Reports when a baseline event loses its event opt-in in the current schema. */
    private fun compareEvent(baseline: MessageType, current: MessageType) {
        val name = baseline.type.toString()
        if (!SurfProtoOptions.event(baseline.options, baseline.location, ignoredProblems)) return

        if (!SurfProtoOptions.event(current.options, current.location, ignoredProblems)) {
            changed(
                current.location,
                name,
                "is no longer an event: (surf.nats.v1.event) = true was removed"
            )
        }
    }

    /**
     * Compares enum values by name, including the zero value and removed reservations.
     *
     * Removing an enum constant is compatible only when its number and name are reserved.
     */
    private fun compareEnum(baseline: EnumType, current: EnumType) {
        val name = baseline.type.toString()
        diff(
            current.location,
            name,
            "zero value",
            baseline.constant(0)?.name,
            current.constant(0)?.name
        )

        val currentReservations = Reserved.fromElements(current.toElement().reserveds)

        for (before in baseline.constants) {
            val element = "$name.${before.name}"
            val after = current.constant(before.name)

            if (after == null) {
                val reservedTag = currentReservations.any { it.matchesTag(before.tag) }
                val reservedName = currentReservations.any { it.matchesName(before.name) }

                if (!reservedTag || !reservedName) {
                    removed(
                        before.location,
                        element,
                        "enum value ${before.name} = ${before.tag} was removed without reserving its number and name " +
                                "(add `reserved ${before.tag};` `reserved \"${before.name}\";` to ${current.name})",
                    )
                }
                continue
            }

            diff(after.location, element, "number", before.tag, after.tag)
        }

        compareReservations(
            name,
            Reserved.fromElements(baseline.toElement().reserveds),
            currentReservations,
            current.location,
        )
    }

    /**
     * Compares existing RPCs and their resolved options for a baseline service.
     *
     * RPCs are matched by name. Request and response types, persistence, and recipient
     * settings are compared; call timeouts are intentionally ignored.
     */
    private fun compareService(baseline: Service) {
        val name = baseline.type.toString()
        val current = currentServices[name]

        if (current == null) {
            removed(baseline.location, name, "service $name was removed or renamed")
            return
        }

        val serviceBefore = ProtocolResolution.service(
            SurfProtoOptions.service(baseline.options, baseline.location, ignoredProblems),
            name,
        )
        val serviceAfter = ProtocolResolution.service(
            SurfProtoOptions.service(current.options, current.location, ignoredProblems),
            name,
        )

        for (before in baseline.rpcs) {
            val element = "$name.${before.name}"
            val after = current.rpc(before.name)

            if (after == null) {
                removed(before.location, element, "rpc ${before.name} was removed or renamed")
                continue
            }

            diff(
                after.location,
                element,
                "request type",
                before.requestType.toString(),
                after.requestType.toString()
            )
            diff(
                after.location,
                element,
                "response type",
                before.responseType.toString(),
                after.responseType.toString()
            )

            val methodBefore = resolve(before, serviceBefore)
            val methodAfter = resolve(after, serviceAfter)

            diff(
                after.location,
                element,
                "persistent",
                methodBefore.persistent,
                methodAfter.persistent
            )
            diff(
                after.location,
                element,
                "recipients",
                methodBefore.recipients,
                methodAfter.recipients
            )
        }
    }

    /** Resolves an RPC's effective configuration using its enclosing service defaults. */
    private fun resolve(rpc: Rpc, service: ResolvedService): ResolvedMethod {
        return ProtocolResolution.method(
            SurfProtoOptions.method(rpc.options, rpc.location, ignoredProblems),
            service,
            emptyResponse = rpc.responseType == ProtoType.EMPTY,
            streaming = rpc.requestStreaming || rpc.responseStreaming,
        )
    }

    /** Returns whether a reader rejects a message that omits [field]. */
    private fun isRequired(field: Field): Boolean {
        return ProtocolFieldRules.isRequired(
            explicitPresence = field.type?.isMap != true && !field.isRepeated,
            oneofMember = field.isOneOf,
            nullable = nullable(field),
            hasDefault = field.default != null,
        )
    }

    /** Returns the effective Protobuf field label, including map and oneof membership. */
    private fun label(type: MessageType, field: Field): String = when {
        field.type?.isMap == true -> "map"
        field.isRepeated -> "repeated"
        field.isOneOf -> {
            val oneofName = type.oneOfs.firstOrNull { oneOf ->
                oneOf.fields.any { it.name == field.name }
            }?.name ?: "?"

            "oneof $oneofName"
        }

        field.label == Field.Label.REQUIRED -> REQUIRED
        else -> OPTIONAL
    }

    /** Returns the effective nullable option for a field. */
    private fun nullable(field: Field): Boolean {
        return SurfProtoOptions.nullable(field.options, field.location, ignoredProblems)
    }

    /**
     * Normalizes a field's default to the value emitted by the generator.
     *
     * This makes equivalent literals compare equal, such as `0x10` and `16`, or
     * differently escaped representations of the same string or byte sequence.
     */
    private fun normalizedDefault(field: Field): String? {
        val literal = field.default ?: return null
        val type = field.type ?: return literal

        return when {
            type == ProtoType.STRING -> rawDefaults.bytes(field.location, literal)?.decodeToString()
                ?: literal

            type == ProtoType.BYTES -> rawDefaults.bytes(field.location, literal)?.joinToString(",")
                ?: literal

            type.isScalar -> DefaultValues.scalar(type.simpleName, literal)?.toString() ?: literal
            else -> literal
        }
    }

    /** Returns the resolved type name, formatting map key and value types separately. */
    private fun typeName(type: ProtoType?): String = when {
        type == null -> "?"
        type.isMap -> "map<${type.keyType}, ${type.valueType}>"
        else -> type.toString()
    }

    /** Returns the Protobuf declaration kind used in compatibility messages. */
    private fun kind(type: Type): String = if (type is EnumType) "enum" else "message"

    /** Reports a change only when [before] and [after] differ. */
    private fun <T> diff(location: Location, element: String, what: String, before: T, after: T) {
        if (before == after) return
        changed(location, element, "$what changed from $before to $after")
    }

    /** Adds a compatibility violation pointing to the current source location. */
    private fun changed(location: Location, element: String, message: String) {
        violations += CompatibilityFinding(location.withoutBase().toString(), element, message)
    }

    /** Adds a compatibility violation pointing to the baseline source location. */
    private fun removed(location: Location, element: String, message: String) {
        violations += CompatibilityFinding("baseline ${location.withoutBase()}", element, message)
    }

    private companion object {
        const val OPTIONAL = "optional"
        const val REQUIRED = "required"
    }
}
