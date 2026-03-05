package dev.slne.surf.surfapi.shared.api.util

import dev.slne.surf.surfapi.shared.api.annotation.InternalAPIMarker

@RequiresOptIn(
    "This API is internal and should not be used from outside the library",
    RequiresOptIn.Level.ERROR
)
@InternalAPIMarker
annotation class InternalSurfApi