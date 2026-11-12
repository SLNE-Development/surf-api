package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock

/**
 * Represents a resolved RPC request or response message.
 *
 * Contains the Kotlin message type and the expression used to obtain
 * its serializer. `google.protobuf.Empty` is represented by Kotlin [Unit].
 */
internal class RpcMessage(val kotlinType: ClassName, val serializer: CodeBlock)
