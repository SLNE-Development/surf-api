package de.slne.redis.example

import de.slne.redis.event.RedisEvent

/**
 * Example event: Player join event
 */
data class PlayerJoinEvent(
    val playerName: String,
    val playerId: String,
    val serverName: String
) : RedisEvent()

/**
 * Example event: Player leave event
 */
data class PlayerLeaveEvent(
    val playerName: String,
    val playerId: String,
    val serverName: String
) : RedisEvent()

/**
 * Example event: Chat message event
 */
data class ChatMessageEvent(
    val playerName: String,
    val message: String,
    val serverName: String
) : RedisEvent()
