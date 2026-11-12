package dev.slne.surf.nats.gradle.contract

import com.squareup.wire.schema.EnumConstant
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.Service

/**
 * What [ContractResolver] decided for the generated declarations of one run, looked up by Wire declaration.
 *
 * Mapped types have no entry. A lookup of a declaration the resolver did not resolve throws
 * [IllegalStateException]. Fields, enum values and rpcs that cannot be represented are left out, so the result
 * is complete only if resolution reported no problem.
 *
 * The maps must compare keys by identity: Wire declarations are data classes with structural equality.
 */
internal class ResolvedContract(
    private val members: Map<MessageType, List<MessageMember>>,
    private val entries: Map<EnumType, Map<String, EnumConstant>>,
    private val methods: Map<Service, List<ContractMethod>>,
) {
    /**
     * Returns the members of [message] in constructor order.
     */
    fun members(message: MessageType): List<MessageMember> {
        return members[message] ?: error("${message.type} was not resolved")
    }

    /**
     * Returns the Kotlin entry names of [enumType] with their constants in declaration order.
     */
    fun entries(enumType: EnumType): Map<String, EnumConstant> {
        return entries[enumType] ?: error("${enumType.type} was not resolved")
    }

    /**
     * Returns the methods of [service] in rpc declaration order.
     */
    fun methods(service: Service): List<ContractMethod> {
        return methods[service] ?: error("${service.type} was not resolved")
    }
}
