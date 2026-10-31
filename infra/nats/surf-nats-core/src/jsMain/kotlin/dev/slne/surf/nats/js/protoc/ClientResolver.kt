package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.descriptor.ProtocolOptionReader
import dev.slne.surf.nats.js.external.protobuf.AnyDesc
import dev.slne.surf.nats.js.external.protobuf.DescComments
import dev.slne.surf.nats.js.external.protobuf.DescExtension
import dev.slne.surf.nats.js.external.protobuf.DescFile
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescService
import dev.slne.surf.nats.js.external.protobuf.hasOption
import dev.slne.surf.nats.js.external.protobuf.protoplugin.Protoplugin
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ServiceNames

/**
 * Validates the services of a proto file with the rules of the Kotlin generator and resolves their clients.
 *
 * Every violation is added to [problems] as `<position>: <message>`, with the message text of the Kotlin generator.
 * [optionExtensions] are the extensions of `surf/nats/v1/options.proto` in the request. They detect surf options in
 * files without a direct import of that file, whose options the option reader does not see.
 */
internal class ClientResolver(
    private val protoplugin: Protoplugin,
    private val problems: MutableList<String>,
    private val optionExtensions: List<DescExtension>,
) {

    /**
     * Returns the clients of [file], or `null` when it declares no services; a file with problems yields no clients.
     */
    fun resolve(file: DescFile): ClientFile? {
        val source = SourceInfo(file)
        val before = problems.size
        if (file.services.isNotEmpty() || file.messages.any(::declaresEvent)) {
            if (file.proto.syntax == PROTO3) problems += "${file.proto.name}: only syntax = \"proto2\" is supported"
            ProtocolNaming.contractPackageProblem(file.proto.`package`)?.let { problems += "${file.proto.name}: $it" }
        }
        if (ProtocolOptions.PROTO_FILE !in file.proto.dependency && usesSurfOptions(file)) {
            problems += "${file.proto.name}: files using surf-nats options must import \"${ProtocolOptions.PROTO_FILE}\" directly"
        }
        val services = file.services.map { service(it, source) }
        return if (services.isEmpty() || problems.size > before) null else ClientFile(file, services)
    }

    private fun service(service: DescService, source: SourceInfo): ServiceClient {
        val comments = protoplugin.getComments(service)
        val resolved = ProtocolResolution.service(ProtocolOptionReader.service(service), service.typeName)
        for (problem in resolved.problems(service.name)) report(source, comments, problem)
        timeoutProblem(ProtocolOptions.SERVICE_OPTIONS, ProtocolOptionReader.serviceTimeoutSeconds(service))?.let {
            report(source, comments, it)
        }
        if (service.methods.isEmpty()) report(source, comments, "service ${service.typeName} declares no rpcs")

        val methods = service.methods.map { method ->
            val methodComments = protoplugin.getComments(method)
            val resolvedMethod = ProtocolResolution.method(
                ProtocolOptionReader.method(method),
                resolved,
                emptyResponse = method.output.typeName == ProtocolFieldRules.EMPTY_TYPE,
                streaming = method.methodKind != UNARY,
            )
            ServiceNames.methodProblem(service.typeName, method.name)?.let { report(source, methodComments, it) }
            for (problem in resolvedMethod.problems(method.name)) report(source, methodComments, problem)
            timeoutProblem(ProtocolOptions.METHOD_OPTIONS, ProtocolOptionReader.methodTimeoutSeconds(method))?.let {
                report(source, methodComments, it)
            }
            ClientMethod(method, resolvedMethod, SourceInfo.comment(methodComments))
        }
        return ServiceClient(service, methods, SourceInfo.comment(comments))
    }

    private fun report(source: SourceInfo, comments: DescComments, problem: String) {
        problems += "${source.position(comments)}: $problem"
    }

    // OptionValues.int does not check the uint32 range; the Kotlin generator rejects values beyond Int.MAX_VALUE
    private fun timeoutProblem(optionType: String, seconds: Double?): String? {
        if (seconds == null || seconds <= Int.MAX_VALUE) return null
        return "option $optionType.${ProtocolOptions.CALL_TIMEOUT_SECONDS} must be between 0 and ${Int.MAX_VALUE}, " +
                "was '${seconds.toLong()}'"
    }

    private fun usesSurfOptions(file: DescFile): Boolean {
        return file.messages.any(::usesSurfOptions) ||
                file.services.any { service -> hasSurfOption(service) || service.methods.any(::hasSurfOption) }
    }

    private fun usesSurfOptions(message: DescMessage): Boolean {
        return hasSurfOption(message) || message.fields.any(::hasSurfOption) || message.nestedMessages.any(::usesSurfOptions)
    }

    private fun hasSurfOption(element: AnyDesc): Boolean = optionExtensions.any { hasOption(element, it) }

    private fun declaresEvent(message: DescMessage): Boolean {
        return ProtocolOptionReader.isEvent(message) || message.nestedMessages.any(::declaresEvent)
    }

    private companion object {
        const val UNARY = "unary"
        const val PROTO3 = "proto3"
    }
}
