@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

// Partial<StreamExpectations>
internal external interface StreamExpectations {
    var streamName: String?
    var lastMsgID: String?
    var lastSequence: Int?
    var lastSubjectSequence: Int?
}
