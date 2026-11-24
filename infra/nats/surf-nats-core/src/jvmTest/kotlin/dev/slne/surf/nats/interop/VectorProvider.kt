package dev.slne.surf.nats.interop

import dev.slne.surf.nats.test.proto.v1.*

internal class VectorProvider(private val book: VectorBook) : InteropVectorService {
    override suspend fun scalars(request: InteropVectorRequest): InteropScalars {
        return book.serveTyped(request.name)
    }

    override suspend fun maps(request: InteropVectorRequest): InteropMaps {
        return book.serveTyped(request.name)
    }

    override suspend fun packed(request: InteropVectorRequest): InteropPacked {
        return book.serveTyped(request.name)
    }

    override suspend fun colors(request: InteropVectorRequest): ColorHolder {
        return book.serveTyped(request.name)
    }

    override suspend fun nullables(request: InteropVectorRequest): InteropNullables {
        return book.serveTyped(request.name)
    }

    override suspend fun defaults(request: InteropVectorRequest): InteropDefaults {
        return book.serveTyped(request.name)
    }

    override suspend fun required(request: InteropVectorRequest): InteropRequired {
        return book.serveTyped(request.name)
    }

    override suspend fun types(request: InteropVectorRequest): InteropTypes {
        return book.serveTyped(request.name)
    }

    override suspend fun empty(request: InteropVectorRequest): InteropEmpty {
        return book.serveTyped(request.name)
    }

    override suspend fun oneof(request: InteropVectorRequest): InteropOneof {
        return book.serveTyped(request.name)
    }
}
