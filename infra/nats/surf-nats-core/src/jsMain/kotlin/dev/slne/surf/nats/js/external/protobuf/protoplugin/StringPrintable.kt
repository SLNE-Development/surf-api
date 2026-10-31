package dev.slne.surf.nats.js.external.protobuf.protoplugin

// A string is a member of the Printable union, which Kotlin cannot express as a supertype of String
internal inline val String.printable: Printable get() = unsafeCast<Printable>()
