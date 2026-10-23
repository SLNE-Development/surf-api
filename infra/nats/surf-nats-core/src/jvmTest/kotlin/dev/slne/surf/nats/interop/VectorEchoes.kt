package dev.slne.surf.nats.interop

import dev.slne.surf.nats.test.proto.v1.Color
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
import java.time.Instant
import java.util.*
import kotlin.time.Duration

internal class VectorEchoes(private val book: VectorBook) : InteropService {
    override suspend fun echoScalars(
        unsignedSmall: Int,
        unsignedLarge: Long,
        signedLarge: Long,
        small: Int,
        large: Long,
        fixedSmall: Int,
        ratio: Double,
        flag: Boolean,
    ): InteropScalars = book.echo(
        InteropScalars(unsignedSmall, unsignedLarge, signedLarge, small, large, fixedSmall, ratio, flag)
    )

    override suspend fun echoMaps(signedByName: Map<String, Long>, namesByFixed: Map<Int, String>): InteropMaps =
        book.echo(InteropMaps(signedByName, namesByFixed))

    override suspend fun echoPacked(packedValues: List<Int>, unpackedValues: List<Int>): InteropPacked =
        book.echo(InteropPacked(packedValues, unpackedValues))

    override suspend fun echoColors(
        color: Color,
        maybeColor: Color?,
        colors: List<Color>,
        colorsByName: Map<String, Color>,
    ): ColorHolder = book.echo(ColorHolder(color, maybeColor, colors, colorsByName))

    override suspend fun echoNullables(
        text: String?,
        number: Int?,
        data: ByteArray?,
        scalars: InteropScalars?,
        uuid: UUID?,
        color: Color?,
    ): InteropNullables = book.echo(InteropNullables(text, number, data, scalars, uuid, color))

    override suspend fun echoDefaults(
        number: Int,
        text: String,
        flag: Boolean,
        ratio: Double,
        color: Color,
        data: ByteArray,
        unsignedSmall: Int,
    ): InteropDefaults = book.echo(InteropDefaults(number, text, flag, ratio, color, data, unsignedSmall))

    override suspend fun echoRequired(name: String, count: Int?): InteropRequired =
        book.echo(InteropRequired(name, count))

    override suspend fun echoTypes(uuid: UUID, timestamp: Instant, duration: Duration): InteropTypes =
        book.echo(InteropTypes(uuid, timestamp, duration))

    override suspend fun echoEmpty(): InteropEmpty = book.echo(InteropEmpty())

    override suspend fun echoOneof(value: InteropOneof.Value?): InteropOneof = book.echo(InteropOneof(value))

    override suspend fun echoEvolved(name: String, level: Int): InteropEvolved = book.echo(InteropEvolved(name, level))

    override suspend fun echoEvolvedV2(name: String, level: Int, nickname: String?, rank: Int): InteropEvolvedV2 =
        book.echo(InteropEvolvedV2(name, level, nickname, rank))
}
