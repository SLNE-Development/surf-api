package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.external.protobuf.protoplugin.GeneratedFile
import dev.slne.surf.nats.js.external.protobuf.protoplugin.ImportSymbol
import dev.slne.surf.nats.js.external.protobuf.protoplugin.Printable
import dev.slne.surf.nats.js.external.protobuf.protoplugin.Schema
import dev.slne.surf.nats.js.external.protobuf.protoplugin.printable
import dev.slne.surf.nats.protocol.RecipientsOption
import dev.slne.surf.nats.protocol.ResultKindOption

/**
 * Generates `<file>_nats.ts` with the typed clients of a [ClientFile].
 *
 * The file imports the descriptors and message types of the protoc-gen-es output (`<file>_pb`) and only types from
 * `@slne/surf-nats` and `@bufbuild/protobuf`. protoplugin resolves import paths and aliases and transpiles the file for
 * the `js` and `dts` targets.
 */
internal class ClientEmitter(schema: Schema, private val client: ClientFile) {
    private val name = "${client.desc.name}_nats"
    private val f: GeneratedFile = schema.generateFile("$name.ts")

    // Identifiers of the generated code: symbols of this file reserve their names, so that imports of the same name
    // are aliased instead of shadowed
    private val nats = local("nats")
    private val request = local("request")
    private val options = local("options")
    private val promise = local("Promise")

    private val messageInitShape = f.import("MessageInitShape", PROTOBUF, typeOnly = true)
    private val callOptions = f.import("CallOptions", SURF_NATS, typeOnly = true)
    private val answer = f.import("Answer", SURF_NATS, typeOnly = true)
    private val operations = f.import("SurfNatsOperations", SURF_NATS, typeOnly = true)

    fun emit() {
        f.preamble(client.desc)
        client.services.forEachIndexed { index, service ->
            if (index > 0) f.print()
            clientInterface(service)
            f.print()
            factory(service)
        }
    }

    private fun local(identifier: String): ImportSymbol = f.import(identifier, "./$name.js")

    private fun clientInterface(service: ServiceClient) {
        val doc = listOfNotNull(service.comment, "Client of the surf-nats service `${service.desc.typeName}`.")
        f.print(f.jsDoc(doc.joinToString("\n\n")))
        f.print(f.export("interface", service.interfaceName), " {".printable)
        for (method in service.methods) {
            val methodDoc = listOfNotNull(method.comment, methodSentence(service, method))
            f.print(f.jsDoc(methodDoc.joinToString("\n\n"), "  "))
            f.print(
                "  ${method.desc.localName}(".printable, request, ": ".printable, messageInitShape,
                "<typeof ".printable, f.importSchema(method.desc.input, typeOnly = true), ">, ".printable,
                options, "?: ".printable, callOptions, "): ".printable, promise, "<".printable, *result(method),
                ">;".printable,
            )
        }
        f.print("}".printable)
    }

    private fun result(method: ClientMethod): Array<Printable> = when (method.resolved.resultKind) {
        ResultKindOption.UNIT -> arrayOf("void".printable)
        ResultKindOption.VALUE -> arrayOf(f.importShape(method.desc.output))
        ResultKindOption.ANSWER -> arrayOf(answer, "<".printable, f.importShape(method.desc.output), ">".printable)
    }

    private fun factory(service: ServiceClient) {
        val descriptor = f.importSchema(service.desc)
        f.print(f.jsDoc("Creates a client of `${service.desc.typeName}` that sends its calls through `nats`."))
        f.print(
            f.export("function", service.factoryName), "(".printable, nats, ": ".printable, operations,
            "): ${service.interfaceName} {".printable,
        )
        f.print("  return {".printable)
        for (method in service.methods) {
            val call = if (method.resolved.resultKind == ResultKindOption.ANSWER) "callAnswer" else "call"
            val name = method.desc.localName
            f.print(
                "    $name: (".printable, request, ", ".printable, options, ") => ".printable, nats,
                ".$call(".printable, descriptor, ".method.$name, ".printable, request, ", ".printable, options,
                "),".printable,
            )
        }
        f.print("  };".printable)
        f.print("}".printable)
    }

    private fun methodSentence(service: ServiceClient, method: ClientMethod): String {
        val rpc = "`${service.desc.typeName}.${method.desc.name}`"
        return when {
            method.resolved.persistent -> "Calls $rpc through its JetStream work queue."
            method.resolved.resultKind == ResultKindOption.ANSWER ->
                "Calls $rpc on all instances; resolves to the first known answer."
            method.resolved.recipients == RecipientsOption.ALL_INSTANCES -> "Calls $rpc on all instances."
            else -> "Calls $rpc."
        }
    }

    private companion object {
        const val PROTOBUF = "@bufbuild/protobuf"
        const val SURF_NATS = "@slne/surf-nats"
    }
}
