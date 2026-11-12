@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

/**
 * The result of `callAnswer`: the first known answer of an `ALL_INSTANCES` method, or none.
 *
 * TypeScript callers see it as the discriminated union `Answer<T>` of `index.d.mts`.
 */
@JsExport
@JsPlainObject
external interface Answer {
    /**
     * `true` if an instance answered with a value, `false` if no known answer arrived.
     */
    val known: Boolean

    /**
     * The response of the first instance with a known answer, absent when [known] is `false`.
     */
    val value: ProtoMessage?
}
