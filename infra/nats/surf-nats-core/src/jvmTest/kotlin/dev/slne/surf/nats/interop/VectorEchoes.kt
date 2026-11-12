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
import dev.slne.surf.nats.test.proto.v1.InteropService
import dev.slne.surf.nats.test.proto.v1.InteropTypes

internal class VectorEchoes(private val book: VectorBook) : InteropService {
    override suspend fun echoScalars(request: InteropScalars): InteropScalars = book.echo(request)

    override suspend fun echoMaps(request: InteropMaps): InteropMaps = book.echo(request)

    override suspend fun echoPacked(request: InteropPacked): InteropPacked = book.echo(request)

    override suspend fun echoColors(request: ColorHolder): ColorHolder = book.echo(request)

    override suspend fun echoNullables(request: InteropNullables): InteropNullables = book.echo(request)

    override suspend fun echoDefaults(request: InteropDefaults): InteropDefaults = book.echo(request)

    override suspend fun echoRequired(request: InteropRequired): InteropRequired = book.echo(request)

    override suspend fun echoTypes(request: InteropTypes): InteropTypes = book.echo(request)

    override suspend fun echoEmpty(request: InteropEmpty): InteropEmpty = book.echo(request)

    override suspend fun echoOneof(request: InteropOneof): InteropOneof = book.echo(request)

    override suspend fun echoEvolved(request: InteropEvolved): InteropEvolved = book.echo(request)

    override suspend fun echoEvolvedV2(request: InteropEvolvedV2): InteropEvolvedV2 = book.echo(request)
}
