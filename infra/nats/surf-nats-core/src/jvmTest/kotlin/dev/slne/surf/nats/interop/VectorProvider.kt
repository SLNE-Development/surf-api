package dev.slne.surf.nats.interop

import dev.slne.surf.nats.test.proto.v1.ColorHolder
import dev.slne.surf.nats.test.proto.v1.InteropDefaults
import dev.slne.surf.nats.test.proto.v1.InteropEmpty
import dev.slne.surf.nats.test.proto.v1.InteropEvolved
import dev.slne.surf.nats.test.proto.v1.InteropEvolvedV2
import dev.slne.surf.nats.test.proto.v1.InteropMaps
import dev.slne.surf.nats.test.proto.v1.InteropNullables
import dev.slne.surf.nats.test.proto.v1.InteropOneof
import dev.slne.surf.nats.test.proto.v1.InteropPacked
import dev.slne.surf.nats.test.proto.v1.InteropRequired
import dev.slne.surf.nats.test.proto.v1.InteropScalars
import dev.slne.surf.nats.test.proto.v1.InteropTypes
import dev.slne.surf.nats.test.proto.v1.InteropVectorService

internal class VectorProvider(private val book: VectorBook) : InteropVectorService {
    override suspend fun scalars(name: String): InteropScalars = book.serveTyped(name)

    override suspend fun maps(name: String): InteropMaps = book.serveTyped(name)

    override suspend fun packed(name: String): InteropPacked = book.serveTyped(name)

    override suspend fun colors(name: String): ColorHolder = book.serveTyped(name)

    override suspend fun nullables(name: String): InteropNullables = book.serveTyped(name)

    override suspend fun defaults(name: String): InteropDefaults = book.serveTyped(name)

    override suspend fun required(name: String): InteropRequired = book.serveTyped(name)

    override suspend fun types(name: String): InteropTypes = book.serveTyped(name)

    override suspend fun empty(name: String): InteropEmpty = book.serveTyped(name)

    override suspend fun oneof(name: String): InteropOneof = book.serveTyped(name)

    override suspend fun evolved(name: String): InteropEvolved = book.serveTyped(name)

    override suspend fun evolvedV2(name: String): InteropEvolvedV2 = book.serveTyped(name)
}
