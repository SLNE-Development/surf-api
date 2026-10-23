package dev.slne.surf.nats.protocol

/**
 * A service with its resolved name; its methods inherit [options].
 */
internal class ResolvedService(val name: String, val global: Boolean, val options: ServiceOptions) {

    /**
     * Returns the rule violations of the service [label] (its proto type name).
     */
    fun problems(label: String): List<String> = listOfNotNull(ProtocolNaming.serviceNameProblem(name, label))
}
