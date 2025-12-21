package de.slne.redis.example

import de.slne.redis.event.RedisEvent
import kotlinx.serialization.Serializable

/**
 * Example event: Player join event
 */
@Serializable
data class PlayerJoinEvent(
    val playerName: String,
    val playerId: String,
    val serverName: String
) : RedisEvent()

/**
 * Example event: Player leave event
 */
@Serializable
data class PlayerLeaveEvent(
    val playerName: String,
    val playerId: String,
    val serverName: String
) : RedisEvent()

/**
 * Example event: Chat message event
 */
@Serializable
data class ChatMessageEvent(
    val playerName: String,
    val message: String,
    val serverName: String
) : RedisEvent()
