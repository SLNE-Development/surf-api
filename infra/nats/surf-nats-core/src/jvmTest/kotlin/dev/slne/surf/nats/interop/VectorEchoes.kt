package dev.slne.surf.nats.interop

import dev.slne.surf.nats.test.proto.v1.*

internal class VectorEchoes(private val book: VectorBook) : InteropService {
    override suspend fun echoScalars(request: InteropScalars): InteropScalars = book.echo(request)

    override suspend fun echoMaps(request: InteropMaps): InteropMaps = book.echo(request)

    override suspend fun echoPacked(request: InteropPacked): InteropPacked = book.echo(request)

    override suspend fun echoColors(request: ColorHolder): ColorHolder = book.echo(request)

    override suspend fun echoNullables(request: InteropNullables): InteropNullables {
        return book.echo(
            request,
        )
    }

    override suspend fun echoDefaults(request: InteropDefaults): InteropDefaults {
        return book.echo(request)
    }

    override suspend fun echoRequired(request: InteropRequired): InteropRequired {
        return book.echo(request)
    }

    override suspend fun echoTypes(request: InteropTypes): InteropTypes = book.echo(request)

    override suspend fun echoEmpty(request: InteropEmpty): InteropEmpty = book.echo(request)

    override suspend fun echoOneof(request: InteropOneof): InteropOneof = book.echo(request)

    override suspend fun echoEvolved(request: InteropEvolved): InteropEvolved = book.echo(request)

    override suspend fun echoEvolvedV2(request: InteropEvolvedV2): InteropEvolvedV2 {
        return book.echo(
            request,
        )
    }
}
