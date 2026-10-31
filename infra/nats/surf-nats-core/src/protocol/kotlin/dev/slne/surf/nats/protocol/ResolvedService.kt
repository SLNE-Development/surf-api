package dev.slne.surf.nats.protocol

/**
 * A service identified by its proto full name [protoName]; its methods inherit [options].
 */
internal class ResolvedService(val protoName: String, val options: ServiceOptions) {

    /**
     * Returns the rule violations of the service name, see [ServiceNames.problem].
     *
     * [declaredName], when given, is the simple name the `.proto` declares and must not contain `.`
     * ([ProtocolNaming.nameProblem]).
     */
    fun problems(declaredName: String? = null): List<String> = listOfNotNull(
        ProtocolNaming.nameProblem("service", protoName, declaredName)
            ?: ServiceNames.problem(protoName)
    )
}
