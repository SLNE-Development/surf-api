@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package dev.slne.surf.redis.codec

import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.EncoderException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

inline fun checkEncoding(value: Boolean) {
    contract {
        returns() implies value
    }

    if (!value) {
        throw EncoderException("Check failed.")
    }
}

inline fun checkEncoding(value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }

    if (!value) {
        val message = lazyMessage()
        throw EncoderException(message.toString())
    }
}

inline fun checkDecoding(value: Boolean) {
    contract {
        returns() implies value
    }

    if (!value) {
        throw DecoderException("Check failed.")
    }
}

inline fun checkDecoding(value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }

    if (!value) {
        val message = lazyMessage()
        throw DecoderException(message.toString())
    }
}