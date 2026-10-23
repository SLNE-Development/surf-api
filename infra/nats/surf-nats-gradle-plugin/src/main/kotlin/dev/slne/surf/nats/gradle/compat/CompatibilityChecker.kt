package dev.slne.surf.nats.gradle.compat

import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Reserved
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import dev.slne.surf.nats.gradle.model.DefaultValues
import dev.slne.surf.nats.gradle.model.KotlinNaming
import dev.slne.surf.nats.gradle.model.RawDefaults
import dev.slne.surf.nats.gradle.model.SurfOptions
import dev.slne.surf.nats.gradle.model.orderedMembers
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
 * Compares the current `.proto` sources with a published baseline.
 *
 * Elements match by fully qualified proto name, so a new major package (`.v2`) is never compared with an older one,
 * while a removed package reports every one of its elements as removed. Options are compared in their resolved form
 * (method over service over default), the call timeout is not compared. Beyond the wire rules, a changed Kotlin
 * package of a file and a dropped reservation are incompatible.
 */
object CompatibilityChecker {

    /**
     * Loads both proto sets against the bundled protos and [protoPathDirs]; empty [currentDirs] mean everything was
     * removed. Throws [dev.slne.surf.nats.gradle.generator.SurfNatsGeneratorException] when a set does not load.
     */
    fun check(baselineDirs: List<File>, currentDirs: List<File>, protoPathDirs: List<File>): CompatibilityReport {
        val baseline = loadSchema(baselineDirs, protoPathDirs)
        val current = if (currentDirs.any { it.containsProtos() }) loadSchema(currentDirs, protoPathDirs) else null
        return compare(baseline, current)
    }

    internal fun compare(baseline: LoadedSchema, current: LoadedSchema?): CompatibilityReport {
        return Comparison(current?.sourceFiles.orEmpty()).run(baseline.sourceFiles)
    }
}

private class Comparison(currentFiles: List<ProtoFile>) {
    private val violations = mutableListOf<CompatibilityFinding>()
    private val warnings = mutableListOf<CompatibilityFinding>()

    /**
     * Option parse errors are the generator's to report; the comparison reads what it can.
     */
    private val ignoredProblems = Problems()
    private val rawDefaults = RawDefaults()

    private val currentTypes: Map<String, Type> =
        currentFiles.flatMap { it.typesAndNestedTypes() }.associateBy { it.type.toString() }
    private val currentServices: Map<String, Service> =
        currentFiles.flatMap { it.services }.associateBy { it.type.toString() }

    /**
     * Kotlin package of the current file that declares each top-level type or service.
     */
    private val currentPackages: Map<String, String> = buildMap {
        for (file in currentFiles) {
            val kotlinPackage = FileRules.kotlinPackage(file)
            for (type in file.types) put(type.type.toString(), kotlinPackage)
            for (service in file.services) put(service.type.toString(), kotlinPackage)
        }
    }

    fun run(baselineFiles: List<ProtoFile>): CompatibilityReport {
        for (file in baselineFiles) {
            compareKotlinPackage(file)
            for (type in file.typesAndNestedTypes()) compareType(type)
            for (service in file.services) compareService(service)
        }
        return CompatibilityReport(violations.toList(), warnings.toList())
    }

    private fun compareKotlinPackage(file: ProtoFile) {
        val before = FileRules.kotlinPackage(file)
        val names = file.types.map { it.type.toString() } + file.services.map { it.type.toString() }
        for (name in names) {
            val after = currentPackages[name] ?: continue
            if (after == before) continue
            val location = currentTypes[name]?.location ?: currentServices.getValue(name).location
            changed(location, name, "java_package changed the Kotlin package from '$before' to '$after'")
        }
    }

    private fun compareType(baseline: Type) {
        val name = baseline.type.toString()
        val current = currentTypes[name]
        when {
            current == null -> removed(baseline.location, name, "${kind(baseline)} $name was removed or renamed")
            baseline is MessageType && current is MessageType -> compareMessage(baseline, current)
            baseline is EnumType && current is EnumType -> compareEnum(baseline, current)
            else -> changed(current.location, name, "changed from ${kind(baseline)} to ${kind(current)}")
        }
    }

    private fun compareMessage(baseline: MessageType, current: MessageType) {
        val name = baseline.type.toString()
        val baselineFields = baseline.fieldsAndOneOfFields.associateBy { it.name }
        val currentFields = current.fieldsAndOneOfFields.associateBy { it.name }
        val reserved = Reserved.fromElements(current.toElement().reserveds)

        for ((fieldName, before) in baselineFields) {
            val element = "$name.$fieldName"
            val after = currentFields[fieldName]
            if (after == null) {
                removedField(baseline, before, current, reserved, element)
                continue
            }
            diff(after.location, element, "field number", before.tag, after.tag)
            diff(after.location, element, "type", typeName(before.type), typeName(after.type))
            diff(after.location, element, "label", label(baseline, before), label(current, after))
            diff(after.location, element, "(surf.nats.v1.nullable)", nullable(before), nullable(after))
            val defaultBefore = normalizedDefault(before)
            val defaultAfter = normalizedDefault(after)
            when {
                defaultBefore == defaultAfter -> Unit
                defaultBefore == null -> changed(after.location, element, "[default = ${after.default}] was added")
                defaultAfter == null -> changed(after.location, element, "[default = ${before.default}] was removed")
                else -> changed(after.location, element, "[default] changed from ${before.default} to ${after.default}")
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

        compareReservations(name, Reserved.fromElements(baseline.toElement().reserveds), reserved, current.location)
        compareEvent(baseline, current)
        compareParameterOrder(baseline, current)
    }

    /**
     * Warns when existing Kotlin constructor and request parameters change position. Parameters are ordered by field
     * number, a oneof by its lowest case number, so a new member numbered below an existing one, or a new lowest oneof
     * case, shifts positional calls that still compile.
     */
    private fun compareParameterOrder(baseline: MessageType, current: MessageType) {
        val before = parameterOrder(baseline)
        val after = parameterOrder(current)
        val positionsAfter = after.withIndex().associate { (index, parameter) -> parameter.key to index }
        val moves = before.withIndex().mapNotNull { (index, parameter) ->
            val newIndex = positionsAfter[parameter.key] ?: return@mapNotNull null
            if (newIndex == index) null else "'${parameter.kotlinName}' moves from position ${index + 1} to ${newIndex + 1}"
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
                    causes += parameter.location to "$kind '${parameter.protoName}' = ${parameter.number} is numbered below the baseline maximum $baselineMax"
                }

                previous?.oneofCases != null && parameter.oneofCases != null -> {
                    val lowest = previous.number
                    for (case in parameter.oneofCases.values) {
                        if (case.name !in previous.oneofCases && case.tag < lowest) {
                            causes += case.location to "new oneof case '${case.name}' = ${case.tag} is numbered below the lowest " +
                                    "baseline case $lowest of oneof '${parameter.protoName}'"
                        }
                    }
                }
            }
        }
        for (parameter in before) {
            if (parameter.key !in positionsAfter) causes += current.location to "'${parameter.protoName}' was removed"
        }

        val requests = currentServices.values.flatMap { service ->
            service.rpcs.filter { it.requestType.toString() == current.type.toString() }.map { "${service.name}.${it.name}" }
        }
        val users = "the constructor of ${current.type.simpleName}" +
                if (requests.isEmpty()) "" else " and the parameters of ${requests.joinToString()}"
        val location = causes.firstOrNull()?.first ?: current.location
        val cause = causes.joinToString("; ") { it.second }.ifEmpty { "the parameter order changed" }
        warnings += CompatibilityFinding(
            location.withoutBase().toString(),
            current.type.toString(),
            "$cause, so positional arguments of $users shift: ${moves.joinToString(", ")}. Named arguments are unaffected; " +
                    "new fields always get a number above the current maximum",
        )
    }

    private fun parameterOrder(type: MessageType): List<OrderedParameter> = orderedMembers(
        type,
        field = { field ->
            OrderedParameter("field:${field.name}", field.name, KotlinNaming.lowerCamel(field.name), field.tag, field.location, null)
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
     * A Kotlin constructor parameter in field-number order; [oneofCases] is `null` for plain fields.
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
     * Reports reserved numbers and names of the baseline that the current type no longer reserves.
     */
    private fun compareReservations(name: String, before: List<Reserved>, after: List<Reserved>, location: Location) {
        val afterValues = after.flatMap { it.values }
        val afterRanges = afterValues.mapNotNull { value ->
            when (value) {
                is Int -> value..value
                is IntRange -> value
                else -> null
            }
        }.sortedBy { it.first }
        val afterNames = afterValues.filterIsInstance<String>().toSet()
        for (value in before.flatMap { it.values }) {
            val dropped = when (value) {
                is Int -> if (covers(afterRanges, value..value)) null else "$value"
                is IntRange -> if (covers(afterRanges, value)) null else "${value.first} to ${value.last}"
                is String -> if (value in afterNames) null else "\"$value\""
                else -> null
            }
            if (dropped != null) changed(location, name, "reserved $dropped is no longer reserved; reservations must be kept")
        }
    }

    private fun covers(ranges: List<IntRange>, range: IntRange): Boolean {
        var next = range.first.toLong()
        for (candidate in ranges) {
            if (candidate.first > next) break
            if (candidate.last >= next) next = candidate.last.toLong() + 1
            if (next > range.last) return true
        }
        return next > range.last
    }

    private fun removedField(baseline: MessageType, field: Field, current: MessageType, reserved: List<Reserved>, element: String) {
        val reasons = mutableListOf<String>()
        if (isRequired(field)) {
            reasons += "it was non-null without a [default], so readers of the baseline fail on messages without it"
        }
        val missingReserve = listOfNotNull(
            field.tag.toString().takeUnless { reserved.any { it.matchesTag(field.tag) } },
            "\"${field.name}\"".takeUnless { reserved.any { it.matchesName(field.name) } },
        )
        if (missingReserve.isNotEmpty()) reasons += "add ${missingReserve.joinToString(" ") { "`reserved $it;`" }} to ${current.name}"
        if (reasons.isEmpty()) return
        removed(field.location, element, "field '${field.name}' = ${field.tag} was removed: ${reasons.joinToString("; ")}")
    }

    private fun compareEvent(baseline: MessageType, current: MessageType) {
        val name = baseline.type.toString()
        val optionsBefore = SurfOptions.event(baseline.options, baseline.location, ignoredProblems) ?: return
        val optionsAfter = SurfOptions.event(current.options, current.location, ignoredProblems)
        if (optionsAfter == null) {
            changed(current.location, name, "is no longer an event: (surf.nats.v1.event) was removed")
            return
        }
        val before = ProtocolResolution.event(optionsBefore, baseline.name)
        val after = ProtocolResolution.event(optionsAfter, current.name)
        diff(current.location, name, "event subject", before.subject, after.subject, quoted = true)
        diff(current.location, name, "event global", before.global, after.global)
    }

    private fun compareEnum(baseline: EnumType, current: EnumType) {
        val name = baseline.type.toString()
        diff(current.location, name, "zero value", baseline.constant(0)?.name, current.constant(0)?.name)

        val reserved = Reserved.fromElements(current.toElement().reserveds)
        for (before in baseline.constants) {
            val element = "$name.${before.name}"
            val after = current.constant(before.name)
            if (after == null) {
                val reservedTag = reserved.any { it.matchesTag(before.tag) }
                val reservedName = reserved.any { it.matchesName(before.name) }
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
        compareReservations(name, Reserved.fromElements(baseline.toElement().reserveds), reserved, current.location)
    }

    private fun compareService(baseline: Service) {
        val name = baseline.type.toString()
        val current = currentServices[name]
        if (current == null) {
            removed(baseline.location, name, "service $name was removed or renamed")
            return
        }
        val serviceBefore = ProtocolResolution.service(SurfOptions.service(baseline.options, baseline.location, ignoredProblems), baseline.name)
        val serviceAfter = ProtocolResolution.service(SurfOptions.service(current.options, current.location, ignoredProblems), current.name)
        diff(current.location, name, "service name", serviceBefore.name, serviceAfter.name, quoted = true)
        diff(current.location, name, "service global", serviceBefore.global, serviceAfter.global)

        for (before in baseline.rpcs) {
            val element = "$name.${before.name}"
            val after = current.rpc(before.name)
            if (after == null) {
                removed(before.location, element, "rpc ${before.name} was removed or renamed")
                continue
            }
            diff(after.location, element, "request type", before.requestType.toString(), after.requestType.toString())
            diff(after.location, element, "response type", before.responseType.toString(), after.responseType.toString())
            val methodBefore = resolve(before, serviceBefore)
            val methodAfter = resolve(after, serviceAfter)
            diff(after.location, element, "method name", methodBefore.name, methodAfter.name, quoted = true)
            diff(after.location, element, "persistent", methodBefore.persistent, methodAfter.persistent)
            diff(after.location, element, "recipients", methodBefore.recipients, methodAfter.recipients)
        }
    }

    private fun resolve(rpc: Rpc, service: ResolvedService): ResolvedMethod {
        return ProtocolResolution.method(
            SurfOptions.method(rpc.options, rpc.location, ignoredProblems),
            service,
            rpc.name,
            emptyResponse = rpc.responseType == ProtoType.EMPTY,
            streaming = rpc.requestStreaming || rpc.responseStreaming,
        )
    }

    /**
     * Returns whether a reader rejects a message without [field] ([ProtocolFieldRules.isRequired]).
     */
    private fun isRequired(field: Field): Boolean {
        return ProtocolFieldRules.isRequired(
            explicitPresence = field.type?.isMap != true && !field.isRepeated,
            oneofMember = field.isOneOf,
            nullable = nullable(field),
            hasDefault = field.default != null,
        )
    }

    private fun label(type: MessageType, field: Field): String = when {
        field.type?.isMap == true -> "map"
        field.isRepeated -> "repeated"
        field.isOneOf -> "oneof " + (type.oneOfs.firstOrNull { oneOf -> oneOf.fields.any { it.name == field.name } }?.name ?: "?")
        field.label == Field.Label.REQUIRED -> REQUIRED
        else -> OPTIONAL
    }

    private fun nullable(field: Field): Boolean = SurfOptions.nullable(field.options, field.location, ignoredProblems)

    /**
     * Returns the `[default]` as the value the generator emits, so `0x10` and `16` or `"\303\251"` and `"é"` compare
     * equal. String and bytes literals are read from the source like the generator does.
     */
    private fun normalizedDefault(field: Field): String? {
        val literal = field.default ?: return null
        val type = field.type ?: return literal
        return when {
            type == ProtoType.STRING -> rawDefaults.bytes(field.location, literal)?.decodeToString() ?: literal
            type == ProtoType.BYTES -> rawDefaults.bytes(field.location, literal)?.joinToString(",") ?: literal
            type.isScalar -> DefaultValues.scalar(type.simpleName, literal)?.toString() ?: literal
            else -> literal
        }
    }

    /**
     * Returns the linked type name; map key and value types are resolved separately from the declared map text.
     */
    private fun typeName(type: ProtoType?): String = when {
        type == null -> "?"
        type.isMap -> "map<${type.keyType}, ${type.valueType}>"
        else -> type.toString()
    }

    private fun kind(type: Type): String = if (type is EnumType) "enum" else "message"

    /**
     * Reports `<what> changed from <before> to <after>` when the values differ; [quoted] wraps both values in `'`.
     */
    private fun <T> diff(location: Location, element: String, what: String, before: T, after: T, quoted: Boolean = false) {
        if (before == after) return
        val quote = if (quoted) "'" else ""
        changed(location, element, "$what changed from $quote$before$quote to $quote$after$quote")
    }

    private fun changed(location: Location, element: String, message: String) {
        violations += CompatibilityFinding(location.withoutBase().toString(), element, message)
    }

    private fun removed(location: Location, element: String, message: String) {
        violations += CompatibilityFinding("baseline ${location.withoutBase()}", element, message)
    }

    private companion object {
        const val OPTIONAL = "optional"
        const val REQUIRED = "required"
    }
}
