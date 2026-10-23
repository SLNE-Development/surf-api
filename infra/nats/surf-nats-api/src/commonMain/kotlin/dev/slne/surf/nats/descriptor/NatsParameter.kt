package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi
import kotlin.reflect.KClass

public class NatsParameter @NatsGeneratedApi constructor(
    public val name: String,
    public val type: KClass<*>,
    public val nullable: Boolean,
)
