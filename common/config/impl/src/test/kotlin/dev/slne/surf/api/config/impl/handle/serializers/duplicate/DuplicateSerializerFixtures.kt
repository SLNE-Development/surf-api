package dev.slne.surf.api.config.impl.handle.serializers.duplicate

import dev.slne.surf.api.config.impl.handle.serializers.CentsSerializer
import dev.slne.surf.api.config.serializer.SurfConfigSerializer

/**
 * One of two `applyToAll` serializers of one plugin for the same type.
 */
@SurfConfigSerializer
class FirstCentsSerializer : CentsSerializer()

/**
 * The other one.
 */
@SurfConfigSerializer
class SecondCentsSerializer : CentsSerializer()
