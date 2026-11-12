package dev.slne.surf.nats.gradle.compat

import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.model.*
import dev.slne.surf.nats.gradle.schema.LoadedSchema
import dev.slne.surf.nats.gradle.schema.containsProtos
import dev.slne.surf.nats.gradle.schema.loadSchema
import dev.slne.surf.nats.gradle.validate.Problems
import dev.slne.surf.nats.gradle.validate.kotlinPackage
import dev.slne.surf.nats.protocol.ResolvedService
import java.io.File

/**
 * Checks whether changes to Protobuf sources are compatible with a published baseline.
 *
 * Types and services are matched by their fully qualified Protobuf names. Renaming an element or moving it to another
 * package is therefore treated as removing it.
 *
 * Besides wire compatibility, reports Kotlin package changes, removed reservations, event configuration changes and
 * changed RPC options. Call timeouts are not compared.
 */
object CompatibilityChecker {

    /**
     * Loads the baseline and current sources against the bundled protos and [protoPathDirs], then compares them.
     *
     * [currentDirs] without `.proto` files count as removing every baseline declaration.
     *
     * @param baselineDirs directories containing the published baseline sources
     * @param currentDirs directories containing the sources to check
     * @param protoPathDirs additional directories used to resolve imported Protobuf definitions
     * @return all compatibility violations and warnings
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
     * Compares already loaded schemas without reloading their source files.
     *
     * A `null` [current] schema is treated as containing no declarations.
     *
     * @param baseline the published baseline schema
     * @param current the current schema, or `null` if no current sources exist
     * @return all compatibility violations and warnings
     */
    internal fun compare(baseline: LoadedSchema, current: LoadedSchema?): CompatibilityReport {
        return Comparison(current?.sourceFiles.orEmpty()).run(baseline.sourceFiles)
    }
}

/**
 * Compares baseline declarations with the current declarations and collects compatibility findings.
 *
 * Findings are emitted in baseline file and declaration order. Violations and warnings are kept separately,
 * preserving their original locations and message text.
 *
 * @param currentFiles the current Protobuf files used to match baseline declarations
 */
private class Comparison(currentFiles: List<ProtoFile>) {
    private val violations = mutableListOf<CompatibilityFinding>()
    private val warnings = mutableListOf<CompatibilityFinding>()

    // The option readers only report call timeouts that do not fit an Int, and timeouts are not compared.
    private val ignoredProblems = Problems()
    private val rawDefaults = RawDefaults()

    private val currentTypes: Map<String, Type> = currentFiles
        .flatMap { it.typesAndNestedTypes() }
        .associateBy { it.type.toString() }

    private val currentServices: Map<String, Service> = currentFiles
        .flatMap { it.services }
        .associateBy { it.type.toString() }

    /**
     * Maps each top-level type and service to its current Kotlin package.
     */
    private val currentPackages: Map<String, String> = buildMap {
        for (file in currentFiles) {
            for (type in file.types) put(type.type.toString(), file.kotlinPackage)
            for (service in file.services) put(service.type.toString(), file.kotlinPackage)
        }
    }

    /**
     * Compares baseline files against the indexed current declarations.
     *
     * Checks package changes before types and services in each file, preserving the order in which findings are reported.
     *
     * @param baselineFiles the baseline source files to check
     * @return the collected violations and warnings
     */
    fun run(baselineFiles: List<ProtoFile>): CompatibilityReport {
        for (file in baselineFiles) {
            compareKotlinPackage(file)
            for (type in file.typesAndNestedTypes()) compareType(type)
            for (service in file.services) compareService(service)
        }

        return CompatibilityReport(violations.toList(), warnings.toList())
    }

    /**
     * Reports changes to the Kotlin package of any top-level type or service in [file].
     *
     * Declarations removed from the current schema are handled separately and are skipped here.
     */
    private fun compareKotlinPackage(file: ProtoFile) {
        val baselinePackage = file.kotlinPackage
        val names = file.types.map { it.type.toString() } + file.services.map { it.type.toString() }
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

    /**
     * Finds the current declaration for [baseline] and checks its type-specific compatibility.
     *
     * Missing declarations and changes between message and enum kinds are reported as violations.
     */
    private fun compareType(baseline: Type) {
        val name = baseline.type.toString()
        val current = currentTypes[name]
        when {
            current == null -> removed(
                baseline.location,
                name,
                "${kind(baseline)} $name was removed or renamed"
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
     * Compares a message's fields, reservations, event opt-in, and generated constructor parameter order.
     *
     * Field and reservation violations are reported before the constructor-order warning.
     */
    private fun compareMessage(baseline: MessageType, current: MessageType) {
        val name = baseline.type.toString()
        val currentReservations = Reserved.fromElements(current.toElement().reserveds)

        compareMessageFields(baseline, current, currentReservations)
        val baselineReservations = Reserved.fromElements(baseline.toElement().reserveds)
        compareReservations(name, baselineReservations, currentReservations, current.location)
        compareEvent(baseline, current)
        compareParameterOrder(baseline, current)
    }

    /**
     * Matches message fields by their Protobuf names and compares existing and newly added fields.
     *
     * Existing fields, including removals, are processed before newly added required fields.
     */
    private fun compareMessageFields(
        baseline: MessageType,
        current: MessageType,
        currentReservations: List<Reserved>,
    ) {
        val baselineFields = baseline.fieldsAndOneOfFields.associateBy { it.name }
        val currentFields = current.fieldsAndOneOfFields.associateBy { it.name }
        val messageName = baseline.type.toString()

        for ((fieldName, before) in baselineFields) {
            val element = "$messageName.$fieldName"
            val after = currentFields[fieldName]
            if (after == null) {
                removedField(before, current, currentReservations, element)
                continue
            }
            compareExistingField(baseline, current, before, after, element)
        }

        for ((fieldName, field) in currentFields) {
            if (fieldName in baselineFields || !isRequired(field)) continue
            reportNewRequiredField(messageName, fieldName, field)
        }
    }

    /**
     * Compares the wire number, type, effective label, and default of an existing field.
     *
     * Comparisons are performed in the same order in which their violations are reported.
     */
    private fun compareExistingField(
        baseline: MessageType,
        current: MessageType,
        before: Field,
        after: Field,
        element: String,
    ) {
        diff(after.location, element, "field number", before.tag, after.tag)
        diff(after.location, element, "type", typeName(before.type), typeName(after.type))
        diff(after.location, element, "label", label(baseline, before), label(current, after))
        compareDefault(before, after, element)
    }

    /**
     * Reports a newly added field that cannot be omitted by senders using the baseline schema.
     */
    private fun reportNewRequiredField(messageName: String, fieldName: String, field: Field) {
        changed(
            field.location,
            "$messageName.$fieldName",
            "new field '$fieldName' = ${field.tag} is required, so readers fail on messages of senders that do not " +
                    "know it; declare it optional or optional with a [default]",
        )
    }

    /**
     * Compares the normalized default values of two matching fields.
     *
     * Reports whether a default was added, removed, or changed. Equivalent source literals do not produce findings.
     */
    private fun compareDefault(before: Field, after: Field, element: String) {
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

    /**
     * Warns when existing Kotlin constructor parameters change their positions.
     *
     * Parameters are ordered by field number, a oneof by its lowest case number. Adding a lower-numbered field or
     * oneof case therefore shifts existing positional arguments even when the constructor call still compiles.
     */
    private fun compareParameterOrder(baseline: MessageType, current: MessageType) {
        val before = parameterOrder(baseline)
        val after = parameterOrder(current)
        val moves = movedParameters(before, after)
        if (moves.isEmpty()) return

        val causes = shiftCauses(baseline, current, before, after)
        val location = causes.firstOrNull()?.first ?: current.location
        val cause =
            causes.joinToString("; ") { it.second }.ifEmpty { "the parameter order changed" }
        warnings += CompatibilityFinding(
            location.withoutBase().toString(),
            current.type.toString(),
            "$cause, so positional arguments of the constructor of ${current.type.simpleName} shift: " +
                    "${moves.joinToString(", ")}. Named arguments are unaffected; " +
                    "new fields always get a number above the current maximum",
        )
    }

    /**
     * Describes the positions of existing constructor parameters that moved between [before] and [after].
     *
     * Removed parameters are excluded. The descriptions follow the baseline parameter order.
     */
    private fun movedParameters(
        before: List<OrderedParameter>,
        after: List<OrderedParameter>
    ): List<String> {
        val positionsAfter =
            after.withIndex().associate { (index, parameter) -> parameter.key to index }
        return before.withIndex().mapNotNull { (index, parameter) ->
            val newIndex = positionsAfter[parameter.key]
            if (newIndex == null || newIndex == index) {
                null
            } else {
                "'${parameter.kotlinName}' moves from position ${index + 1} to ${newIndex + 1}"
            }
        }
    }

    /**
     * Finds changes that explain shifted constructor parameters, along with their reporting locations.
     *
     * Added fields and oneof cases are considered first, followed by removed parameters. Cause order is significant
     * because the first cause determines the warning location.
     */
    private fun shiftCauses(
        baseline: MessageType,
        current: MessageType,
        before: List<OrderedParameter>,
        after: List<OrderedParameter>,
    ): List<Pair<Location, String>> {
        val causes = mutableListOf<Pair<Location, String>>()
        val baselineMax = baseline.fieldsAndOneOfFields.maxOfOrNull { it.tag } ?: 0
        val beforeByKey = before.associateBy { it.key }

        for (parameter in after) {
            val previous = beforeByKey[parameter.key]
            if (previous == null) {
                newParameterShiftCause(parameter, baselineMax)?.let { causes += it }
            } else {
                causes += newOneofCaseShiftCauses(previous, parameter)
            }
        }

        val keysAfter = after.mapTo(HashSet()) { it.key }
        for (parameter in before) {
            if (parameter.key !in keysAfter) {
                causes += current.location to "'${parameter.protoName}' was removed"
            }
        }
        return causes
    }

    /**
     * Explains when a newly added field or oneof is numbered below the highest baseline field number.
     *
     * @return the cause and location, or `null` when the new parameter does not explain a shift
     */
    private fun newParameterShiftCause(
        parameter: OrderedParameter,
        baselineMax: Int
    ): Pair<Location, String>? {
        if (parameter.number >= baselineMax) return null
        val kind = if (parameter.oneofCases != null) "new oneof" else "new field"
        return parameter.location to
                "$kind '${parameter.protoName}' = ${parameter.number} is numbered below the baseline maximum $baselineMax"
    }

    /**
     * Finds newly added oneof cases that precede the oneof's previous lowest field number.
     *
     * Cases retain their current declaration order so the first reported cause stays unchanged.
     */
    private fun newOneofCaseShiftCauses(
        previous: OrderedParameter,
        current: OrderedParameter,
    ): List<Pair<Location, String>> {
        val previousCases = previous.oneofCases ?: return emptyList()
        val currentCases = current.oneofCases ?: return emptyList()
        val causes = mutableListOf<Pair<Location, String>>()

        for (case in currentCases.values) {
            if (case.name !in previousCases && case.tag < previous.number) {
                causes += case.location to
                        "new oneof case '${case.name}' = ${case.tag} is numbered below the lowest " +
                        "baseline case ${previous.number} of oneof '${current.protoName}'"
            }
        }
        return causes
    }

    /**
     * Returns the generated constructor parameters in field-number order.
     *
     * Each oneof contributes one parameter positioned at the lowest field number of its cases.
     */
    private fun parameterOrder(type: MessageType): List<OrderedParameter> = orderedMembers(
        type,
        field = { field ->
            OrderedParameter(
                key = "field:${field.name}",
                protoName = field.name,
                kotlinName = KotlinNaming.lowerCamel(field.name),
                number = field.tag,
                location = field.location,
                oneofCases = null,
            )
        },
        oneof = { oneOf, number ->
            OrderedParameter(
                key = "oneof:${oneOf.name}",
                protoName = oneOf.name,
                kotlinName = KotlinNaming.lowerCamel(oneOf.name),
                number = number,
                location = oneOf.location,
                oneofCases = oneOf.fields.associateBy { it.name },
            )
        },
    )

    /**
     * Describes a generated Kotlin constructor parameter and its position in a Protobuf message.
     *
     * [key] distinguishes plain fields from oneofs. [oneofCases] is `null` for plain fields and contains the cases
     * of a oneof parameter otherwise.
     */
    private class OrderedParameter(
        val key: String,
        val protoName: String,
        val kotlinName: String,
        val number: Int,
        val location: Location,
        val oneofCases: Map<String, Field>?,
    )

    /**
     * Reports reserved names or numbers of the baseline that the current type no longer reserves.
     *
     * A number stays reserved when the union of current number ranges covers its original range.
     */
    private fun compareReservations(
        name: String,
        before: List<Reserved>,
        after: List<Reserved>,
        location: Location
    ) {
        val currentValues = after.flatMap { it.values }
        val currentRanges = currentValues
            .mapNotNull { value ->
                when (value) {
                    is Int -> value..value
                    is IntRange -> value
                    else -> null
                }
            }
            .sortedBy { it.first }
        val currentNames = currentValues.filterIsInstance<String>().toSet()

        for (value in before.flatMap { it.values }) {
            val dropped = when (value) {
                is Int -> "$value".takeUnless { covers(currentRanges, value..value) }
                is IntRange -> "${value.first} to ${value.last}".takeUnless {
                    covers(
                        currentRanges,
                        value
                    )
                }

                is String -> "\"$value\"".takeUnless { value in currentNames }
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

    /**
     * Returns whether [ranges], sorted by start, together cover all of [range].
     *
     * Uses long arithmetic to avoid overflow when a reservation reaches the largest possible field number.
     */
    private fun covers(ranges: List<IntRange>, range: IntRange): Boolean {
        var next = range.first.toLong()
        for (candidate in ranges) {
            if (candidate.first > next) break
            if (candidate.last >= next) next = candidate.last.toLong() + 1
            if (next > range.last) return true
        }
        return next > range.last
    }

    /**
     * Reports a removed field that was required or whose number or name is not reserved.
     *
     * Both reasons are combined into one violation when applicable. A fully reserved optional field produces no finding.
     */
    private fun removedField(
        field: Field,
        current: MessageType,
        reserved: List<Reserved>,
        element: String
    ) {
        val reasons = mutableListOf<String>()
        if (isRequired(field)) {
            reasons += "it was required, so readers of the baseline fail on messages without it"
        }

        val missingReservations = listOfNotNull(
            field.tag.toString().takeUnless { reserved.any { it.matchesTag(field.tag) } },
            "\"${field.name}\"".takeUnless { reserved.any { it.matchesName(field.name) } },
        )
        if (missingReservations.isNotEmpty()) {
            reasons += "add ${missingReservations.joinToString(" ") { "`reserved $it;`" }} to ${current.name}"
        }

        if (reasons.isEmpty()) return
        removed(
            field.location,
            element,
            "field '${field.name}' = ${field.tag} was removed: ${reasons.joinToString("; ")}"
        )
    }

    /**
     * Reports when a message previously declared as a NATS event loses its event option.
     *
     * Adding the event option does not produce a finding.
     */
    private fun compareEvent(baseline: MessageType, current: MessageType) {
        val wasEvent = SurfProtoOptions.isEvent(baseline.options)
        if (!wasEvent) return
        val isEvent = SurfProtoOptions.isEvent(current.options)
        if (!isEvent) {
            changed(
                current.location,
                baseline.type.toString(),
                "is no longer an event: (surf.nats.v1.event) = true was removed"
            )
        }
    }

    /**
     * Matches enum values by name and checks their numbers and reservations.
     *
     * Removing a value is compatible only when both its number and name remain reserved.
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
            compareEnumConstant(name, before, current, currentReservations)
        }

        val baselineReservations = Reserved.fromElements(baseline.toElement().reserveds)
        compareReservations(name, baselineReservations, currentReservations, current.location)
    }

    /**
     * Compares an enum constant with its current namesake, or verifies reservations after its removal.
     */
    private fun compareEnumConstant(
        enumName: String,
        before: EnumConstant,
        current: EnumType,
        reservations: List<Reserved>,
    ) {
        val element = "$enumName.${before.name}"
        val after = current.constant(before.name)
        if (after != null) {
            diff(after.location, element, "number", before.tag, after.tag)
            return
        }

        val reservedTag = reservations.any { it.matchesTag(before.tag) }
        val reservedName = reservations.any { it.matchesName(before.name) }
        if (!reservedTag || !reservedName) {
            removed(
                before.location,
                element,
                "enum value ${before.name} = ${before.tag} was removed without reserving its number and name " +
                        "(add `reserved ${before.tag};` `reserved \"${before.name}\";` to ${current.name})",
            )
        }
    }

    /**
     * Matches RPCs by name and compares their request and response types, persistence, and recipients.
     *
     * Removed services and RPCs are violations. Call timeout changes are intentionally ignored.
     */
    private fun compareService(baseline: Service) {
        val name = baseline.type.toString()
        val current = currentServices[name]
        if (current == null) {
            removed(baseline.location, name, "service $name was removed or renamed")
            return
        }

        val serviceBefore = SurfProtoOptions.service(baseline, ignoredProblems)
        val serviceAfter = SurfProtoOptions.service(current, ignoredProblems)
        for (before in baseline.rpcs) {
            compareRpc(name, before, current, serviceBefore, serviceAfter)
        }
    }

    /**
     * Compares an RPC with the current declaration of the same name, if one exists.
     *
     * Request and response type changes are reported before differences in resolved method options.
     */
    private fun compareRpc(
        serviceName: String,
        before: Rpc,
        current: Service,
        serviceBefore: ResolvedService,
        serviceAfter: ResolvedService,
    ) {
        val element = "$serviceName.${before.name}"
        val after = current.rpc(before.name)
        if (after == null) {
            removed(before.location, element, "rpc ${before.name} was removed or renamed")
            return
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

        val methodBefore = SurfProtoOptions.method(before, serviceBefore, ignoredProblems)
        val methodAfter = SurfProtoOptions.method(after, serviceAfter, ignoredProblems)
        diff(after.location, element, "persistent", methodBefore.persistent, methodAfter.persistent)
        diff(after.location, element, "recipients", methodBefore.recipients, methodAfter.recipients)
    }

    /**
     * Returns whether a reader rejects a message that omits [field].
     */
    private fun isRequired(field: Field): Boolean = field.label == Field.Label.REQUIRED

    /**
     * Returns the effective label of [field], including map and oneof membership.
     */
    private fun label(type: MessageType, field: Field): String = when {
        field.type?.isMap == true -> "map"
        field.isRepeated -> "repeated"
        field.isOneOf -> {
            val oneofName =
                type.oneOfs.firstOrNull { oneOf -> oneOf.fields.any { it.name == field.name } }?.name
            "oneof ${oneofName ?: "?"}"
        }

        field.label == Field.Label.REQUIRED -> "required"
        else -> "optional"
    }

    /**
     * Normalizes the default of [field] to the value the generator emits.
     *
     * Equivalent literals then compare equal, such as `0x10` and `16`, or differently escaped forms of the same string
     * or byte sequence.
     *
     * Scalars compare as their generated Kotlin source text, which also decides how a default compares across a type
     * change: `int32` and `string` defaults `5` are equal, `int32` and `int64` ones (`5`, `5L`) are not. Changing that
     * changes the reported findings.
     */
    private fun normalizedDefault(field: Field): String? {
        val literal = field.default ?: return null
        val type = field.type ?: return literal
        return when {
            type == ProtoType.STRING -> rawDefaults.bytes(field.location, literal)?.decodeToString()
                ?: literal

            type == ProtoType.BYTES -> rawDefaults.bytes(field.location, literal)?.joinToString(",")
                ?: literal

            type.isScalar -> DefaultValues.scalar(type, literal)?.toString() ?: literal
            else -> literal
        }
    }

    /**
     * Formats a Protobuf type for field compatibility messages, including map key and value types.
     */
    private fun typeName(type: ProtoType?): String = when {
        type == null -> "?"
        type.isMap -> "map<${type.keyType}, ${type.valueType}>"
        else -> type.toString()
    }

    /**
     * Returns the declaration kind used in removal and kind-change findings.
     */
    private fun kind(type: Type): String = if (type is EnumType) "enum" else "message"

    /**
     * Reports a changed value when [before] and [after] differ.
     *
     * The finding is attributed to the current declaration's [location].
     */
    private fun <T> diff(location: Location, element: String, what: String, before: T, after: T) {
        if (before == after) return
        changed(location, element, "$what changed from $before to $after")
    }

    /**
     * Adds a violation located in the current sources.
     */
    private fun changed(location: Location, element: String, message: String) {
        violations += CompatibilityFinding(location.withoutBase().toString(), element, message)
    }

    /**
     * Adds a violation located in the baseline sources.
     */
    private fun removed(location: Location, element: String, message: String) {
        violations += CompatibilityFinding("baseline ${location.withoutBase()}", element, message)
    }
}
