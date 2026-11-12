package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.ClassName
import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.model.KotlinTypeResolver
import dev.slne.surf.nats.gradle.model.SurfProtoOptions
import dev.slne.surf.nats.protocol.ProtocolNaming
import java.util.IdentityHashMap

/**
 * Resolves the Kotlin shape of every generated declaration and reports what cannot be generated.
 *
 * One instance resolves one generator run. Problems go to the [Problems][KotlinTypeResolver.problems] of [types]:
 * per source file its types depth-first in declaration order, then its services. Within a message: its members in
 * constructor order, property name clashes, event rules, then per nested type its name check followed by the nested
 * type.
 */
internal class ContractResolver(private val types: KotlinTypeResolver) {
    private val problems get() = types.problems
    private val memberResolver = MemberResolver(types)
    private val enumResolver = EnumResolver(types.problems)
    private val serviceResolver = ServiceResolver(types)

    private val membersByMessage = IdentityHashMap<MessageType, List<MessageMember>>()
    private val entriesByEnum = IdentityHashMap<EnumType, Map<String, EnumConstant>>()
    private val methodsByService = IdentityHashMap<Service, List<ContractMethod>>()

    fun resolve(sourceFiles: List<ProtoFile>): ResolvedContract {
        for (file in sourceFiles) {
            file.types.forEach(::resolveType)
            for (service in file.services) {
                methodsByService[service] = serviceResolver.resolve(service)
            }
        }

        return ResolvedContract(membersByMessage, entriesByEnum, methodsByService)
    }

    private fun resolveType(type: Type) {
        when (type) {
            is MessageType -> resolveMessage(type)
            is EnumType -> if (!types.isMapped(type.type)) entriesByEnum[type] = enumResolver.resolve(type)
            else -> Unit
        }
    }

    private fun resolveMessage(type: MessageType) {
        val isEvent = SurfProtoOptions.isEvent(type.options)
        if (types.isMapped(type.type)) {
            if (isEvent) {
                problems.error(
                    type.location,
                    "event message ${type.type} is mapped by mapType; events must be generated"
                )
            }
            return
        }

        val className = checkNotNull(types.generatedClass(type.type))
        val members = memberResolver.resolve(type, className)
        membersByMessage[type] = members
        if (isEvent) checkEvent(type, members)
        resolveNestedTypes(type, className, members)
    }

    private fun checkEvent(type: MessageType, members: List<MessageMember>) {
        members.firstOrNull { it.name == GeneratedNames.NATS_DESCRIPTOR }?.let { owner ->
            problems.error(
                owner.location,
                "event ${type.type} must not have a field named ${GeneratedNames.NATS_DESCRIPTOR}"
            )
        }
        ProtocolNaming.nameProblem("event", type.type.toString(), type.name)
            ?.let { problems.error(type.location, it) }
    }

    /**
     * Resolves the nested types of [type] after checking each name against the oneof interfaces of [members] and
     * the nested types before it.
     *
     * The name check also runs for mapped nested types, which are not resolved.
     */
    private fun resolveNestedTypes(type: MessageType, className: ClassName, members: List<MessageMember>) {
        val nestedNames = HashMap<String, Location>()
        for (member in members) {
            if (member is OneofMember) nestedNames[member.interfaceName.simpleName] = member.location
        }

        for (nested in type.nestedTypes) {
            checkNestedName(className, nested, nestedNames)
            resolveType(nested)
        }
    }

    /**
     * Reports nested type names that collide with the enclosing class, companion, or oneof types.
     *
     * [used] maps the oneof and already visited nested type names to their locations. Non-reserved nested names are
     * recorded even when they collide with oneofs, and the previous declaration location is reported for a collision.
     */
    private fun checkNestedName(
        owner: ClassName,
        nested: Type,
        used: MutableMap<String, Location>
    ) {
        when (val name = nested.type.simpleName) {
            owner.simpleName ->
                problems.error(
                    nested.location,
                    "nested type $name must not have the name of its enclosing type"
                )

            GeneratedNames.COMPANION ->
                problems.error(
                    nested.location,
                    "nested type name Companion clashes with the generated companion object"
                )

            else -> used.put(name, nested.location)?.let { previousLocation ->
                problems.error(
                    nested.location,
                    "nested type $name clashes with the oneof declared at $previousLocation"
                )
            }
        }
    }
}
