package dev.slne.surf.nats.gradle.contract

/**
 * Names of members that generated declarations always have. Proto names that map to them are rejected.
 */
internal object GeneratedNames {
    /**
     * Name of the companion object the serialization plugin generates for `@Serializable` classes and interfaces.
     */
    const val COMPANION = "Companion"
    const val NATS_DESCRIPTOR = "natsDescriptor"
    const val ENUM_NUMBER = "number"
    const val ENUM_FOR_NUMBER = "forNumber"
    const val ENUM_SERIALIZER = "Serializer"
}
