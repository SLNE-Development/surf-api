package dev.slne.surf.nats.codec

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.WireFormat
import kotlinx.serialization.KSerializer
import java.util.*

/**
 * One serializer per [WireFormat] for the same type.
 */
@InternalNatsApi
internal class WireSerializers<T>(private val byFormat: Map<WireFormat, KSerializer<T>>) {

    operator fun get(format: WireFormat): KSerializer<T> = byFormat.getValue(format)

    companion object {
        inline fun <T> of(serializer: (WireFormat) -> KSerializer<T>): WireSerializers<T> {
            return WireSerializers(
                WireFormat.entries.associateWithTo(
                    EnumMap(WireFormat::class.java),
                    serializer
                )
            )
        }
    }
}
