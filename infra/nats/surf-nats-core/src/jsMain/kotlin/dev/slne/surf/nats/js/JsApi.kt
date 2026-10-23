@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js

// Exported option and context shapes. The typed TypeScript API is the facade in src/jsMain/resources/index.d.mts.

@JsExport
external interface MessageSchema {
    val kind: String
    val typeName: String
}

@JsExport
external interface MethodSchema {
    val kind: String
    val name: String
}

// A protobuf-es message or an init object for create(); init objects have no $typeName.
@JsExport
external interface ProtoMessageInit {
    val `$typeName`: String?
}

// A protobuf-es message (Message of @bufbuild/protobuf).
@JsExport
external interface ProtoMessage : ProtoMessageInit {
    override val `$typeName`: String
}

@JsExport
external interface ConnectOptions {
    val name: String

    // The facade (index.mjs) splits the comma-separated string form of the TypeScript API.
    val servers: Array<String>?
    val token: String?
    val user: String?
    val pass: String?
    val credsFile: String?
    val connectTimeoutMs: Int?
}

@JsExport
external interface PublishOptions {
    val owner: String?
    val timeoutMs: Int?
}

@JsExport
external interface ListenOptions {
    val owner: String?
    val recipients: String?
    val ignoreFromThisClient: Boolean?
}

@JsExport
external interface CallOptions {
    val owner: String?
    val timeoutMs: Int?
}

@JsExport
external interface EventContext {
    val origin: String?
    val isFromThisClient: Boolean
}

// Result of callAnswer: { known: true, value } or { known: false }.
@JsExport
external interface Answer {
    val known: Boolean
    val value: ProtoMessage?
}

internal external interface EventContextInit : EventContext {
    override var origin: String?
    override var isFromThisClient: Boolean
}

internal external interface AnswerInit : Answer {
    override var known: Boolean
    override var value: ProtoMessage?
}
