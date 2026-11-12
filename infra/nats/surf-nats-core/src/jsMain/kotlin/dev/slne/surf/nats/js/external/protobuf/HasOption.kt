@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

/**
 * Binds `hasOption`, which returns whether [option] is set on [element].
 */
internal external fun hasOption(element: AnyDesc, option: DescExtension): Boolean
