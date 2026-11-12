package dev.slne.surf.nats.interop

import dev.slne.surf.nats.test.proto.v1.ColorHolder
import dev.slne.surf.nats.test.proto.v1.InteropDefaults
import dev.slne.surf.nats.test.proto.v1.InteropEmpty
import dev.slne.surf.nats.test.proto.v1.InteropMaps
import dev.slne.surf.nats.test.proto.v1.InteropNullables
import dev.slne.surf.nats.test.proto.v1.InteropOneof
import dev.slne.surf.nats.test.proto.v1.InteropPacked
import dev.slne.surf.nats.test.proto.v1.InteropRequired
import dev.slne.surf.nats.test.proto.v1.InteropScalars
import dev.slne.surf.nats.test.proto.v1.InteropTypes
import dev.slne.surf.nats.test.proto.v1.InteropVectorRequest
import dev.slne.surf.nats.test.proto.v1.InteropVectorService

internal class VectorProvider(private val book: VectorBook) : InteropVectorService {
    override suspend fun scalars(request: InteropVectorRequest): InteropScalars = book.serveTyped(request.name)

    override suspend fun maps(request: InteropVectorRequest): InteropMaps = book.serveTyped(request.name)

    override suspend fun packed(request: InteropVectorRequest): InteropPacked = book.serveTyped(request.name)

    override suspend fun colors(request: InteropVectorRequest): ColorHolder = book.serveTyped(request.name)

    override suspend fun nullables(request: InteropVectorRequest): InteropNullables = book.serveTyped(request.name)

    override suspend fun defaults(request: InteropVectorRequest): InteropDefaults = book.serveTyped(request.name)

    override suspend fun required(request: InteropVectorRequest): InteropRequired = book.serveTyped(request.name)

    override suspend fun types(request: InteropVectorRequest): InteropTypes = book.serveTyped(request.name)

    override suspend fun empty(request: InteropVectorRequest): InteropEmpty = book.serveTyped(request.name)

    override suspend fun oneof(request: InteropVectorRequest): InteropOneof = book.serveTyped(request.name)
}
