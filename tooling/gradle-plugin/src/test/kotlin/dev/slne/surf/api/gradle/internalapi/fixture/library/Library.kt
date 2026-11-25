package dev.slne.surf.api.gradle.internalapi.fixture.library

import dev.slne.surf.api.shared.api.annotation.InternalAPIMarker
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.shared.api.util.PublishedSurfApi

/**
 * A library's own internal API marker, retained only in bytecode.
 */
@InternalAPIMarker
@Retention(AnnotationRetention.BINARY)
annotation class InternalLibraryApi

/**
 * An annotation that is not an internal API marker.
 */
annotation class Documented

object Library {
    fun publicApi() = Unit

    @InternalSurfApi
    fun internalApi() = Unit

    @InternalSurfApi
    @PublishedSurfApi
    fun publishedApi() = Unit

    @InternalLibraryApi
    fun libraryInternalApi() = Unit

    @Documented
    fun documentedApi() = Unit

    @InternalSurfApi
    var internalProperty: Int = 0
}

@InternalSurfApi
open class InternalBase

open class PublicBase {
    fun publicInherited() = Unit

    @InternalSurfApi
    fun internalInherited() = Unit
}

@InternalSurfApi
class InternalSub : PublicBase()

object InternalSubs {
    fun create() = InternalSub()
}

interface PublicContract {
    @InternalSurfApi
    fun internalDefault() = Unit
}

object DefaultArguments {
    @InternalSurfApi
    fun internalWithDefault(value: Int = 1) = Unit
}

class InternalConstructor @InternalSurfApi constructor(val value: Int = 1)

@InternalSurfApi
class InternalOuter {
    class Nested {
        fun call() = Unit
    }
}
