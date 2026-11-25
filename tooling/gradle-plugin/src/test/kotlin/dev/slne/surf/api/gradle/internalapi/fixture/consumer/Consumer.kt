package dev.slne.surf.api.gradle.internalapi.fixture.consumer

import dev.slne.surf.api.gradle.internalapi.fixture.library.*
import dev.slne.surf.api.shared.api.annotation.InternalAPIMarker

class CallsPublicApi {
    fun run() {
        Library.publicApi()
        Library.documentedApi()
    }
}

class CallsInternalApi {
    fun run() = Library.internalApi()
}

class CallsPublishedApi {
    fun run() = Library.publishedApi()
}

class CallsLibraryInternalApi {
    fun run() = Library.libraryInternalApi()
}

class ReadsInternalProperty {
    fun run() = Library.internalProperty
}

class ExtendsInternalBase : InternalBase()

class CallsNestedOfInternal {
    fun run() = InternalOuter.Nested().call()
}

class CallsInheritedInternalApi : PublicBase() {
    fun run() = internalInherited()
}

class CallsInheritedInterfaceDefault : PublicContract {
    fun run() = internalDefault()
}

class CallsInternalApiWithDefaults {
    fun run() = DefaultArguments.internalWithDefault()
}

class CallsInternalConstructorWithDefaults {
    fun run() = InternalConstructor()
}

class CallsPublicApiThroughInternalSubclass {
    fun run() = InternalSubs.create().publicInherited()
}

@InternalConsumerApi
class InheritsPublicApiInOwnInternalClass : PublicBase() {
    fun run() = publicInherited()
}

/**
 * The consumer's own marker; references between the consumer's classes are never reported.
 */
@InternalAPIMarker
annotation class InternalConsumerApi

@InternalConsumerApi
object ConsumerInternals {
    fun helper() = Unit
}

class CallsOwnInternalApi {
    fun run() = ConsumerInternals.helper()
}
