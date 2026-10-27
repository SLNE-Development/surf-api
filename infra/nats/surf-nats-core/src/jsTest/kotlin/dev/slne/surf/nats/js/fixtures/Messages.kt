package dev.slne.surf.nats.js.fixtures

import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import js.typedarrays.Uint8Array
import kotlin.js.Json

// protobuf-es shapes of the fixture messages, used as init objects and to read decoded messages (see fields()).

internal external interface Uuid : ProtoMessageInit {
    var value: Uint8Array<*>?
}

internal external interface Location : ProtoMessageInit {
    var world: Uuid?
    var x: Double?
    var y: Double?
    var z: Double?
    var yaw: Double?
    var pitch: Double?
}

internal external interface Inner : ProtoMessageInit {
    var label: String?
    var count: Int?
}

// A oneof group: { case: "<field>", value } or { case: undefined }
internal external interface OneofCase {
    var case: String?
    var value: Any?
}

internal external interface Rules : ProtoMessageInit {
    var requiredText: String?
    var nullableText: String?
    var defaulted: Int?
    var inner: Inner?
    var inners: Array<Inner>?
    var innerMap: Json?
    var choice: OneofCase?
    var at: ProtoMessageInit?
    var uuid: Uuid?
    var location: Location?
    var color: Int?
    var nullableColor: Int?
    var colors: Array<Int>?
    var colorMap: Json?
    var big: Double?
}

internal external interface PlayerJoined : ProtoMessageInit {
    var name: String?
    var id: Uuid?
    var server: String?
}

internal external interface ChatMessage : ProtoMessageInit {
    var text: String?
}

internal external interface ServerStarted : ProtoMessageInit {
    var server: String?
}

internal external interface FindPlayerRequest : ProtoMessageInit {
    var name: String?
}

internal external interface FindPlayerResponse : ProtoMessageInit {
    var name: String?
    var server: String?
}

internal external interface LocateResponse : ProtoMessageInit {
    var server: String?
}

internal external interface BlobData : ProtoMessageInit {
    var data: Uint8Array<*>?
}

internal external interface AuditEntry : ProtoMessageInit {
    var action: String?
    var data: Uint8Array<*>?
}

internal external interface AuditResult : ProtoMessageInit {
    var count: Int?
}

// The fields of a decoded message as the fixture shape [T].
internal fun <T : ProtoMessageInit> ProtoMessage?.fields(): T = checkNotNull(this) { "Expected a message" }.unsafeCast<T>()
