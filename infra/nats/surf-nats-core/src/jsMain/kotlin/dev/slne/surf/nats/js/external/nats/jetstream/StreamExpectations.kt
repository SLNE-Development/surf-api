@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlinx.js.JsPlainObject

// Partial<StreamExpectations>
@JsPlainObject
internal external interface StreamExpectations {
    val streamName: String?
    val lastMsgID: String?
    val lastSequence: Int?
    val lastSubjectSequence: Int?
}
