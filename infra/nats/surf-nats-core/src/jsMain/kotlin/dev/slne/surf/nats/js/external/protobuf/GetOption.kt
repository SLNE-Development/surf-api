@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

/**
 * Binds `getOption`, which returns the value of [option] on [element].
 *
 * The value has the type of the extension: a message, an enum number or a scalar. It is never `undefined`, an unset
 * option yields the default.
 */
internal external fun getOption(element: AnyDesc, option: DescExtension): Any?
