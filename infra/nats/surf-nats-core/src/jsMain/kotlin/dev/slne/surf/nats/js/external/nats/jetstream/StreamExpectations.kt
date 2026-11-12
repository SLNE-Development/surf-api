@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlinx.js.JsPlainObject

/**
 * Binds `Partial<StreamExpectations>`, the conditions on the stream state that a publish expects.
 */
@JsPlainObject
internal external interface StreamExpectations {
    val streamName: String?
    val lastMsgID: String?
    val lastSequence: Int?
    val lastSubjectSequence: Int?
}
