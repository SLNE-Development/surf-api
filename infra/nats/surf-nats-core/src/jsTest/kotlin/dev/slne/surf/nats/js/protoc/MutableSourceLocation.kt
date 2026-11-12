package dev.slne.surf.nats.js.protoc

// Writable view of a decoded SourceCodeInfo.Location, to rewrite its comments in tests
internal external interface MutableSourceLocation {
    var leadingComments: String
    var trailingComments: String
}
