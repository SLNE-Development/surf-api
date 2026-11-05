package dev.slne.surf.microservice.database

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.database.integration.DatabaseHolder

/**
 * The database of the running microservice.
 *
 * Available from `onBootstrap` until the end of `onDisable`.
 *
 * @throws IllegalStateException if the database integration is not running
 */
val Microservice.database: R2dbcDatabase
    get() = databaseApi.database

/**
 * The [DatabaseApi] that owns [database], for example to run additional migrations.
 *
 * Available from `onBootstrap` until the end of `onDisable`. The database integration closes it; do not call
 * [DatabaseApi.shutdown].
 *
 * @throws IllegalStateException if the database integration is not running
 */
@Suppress("UnusedReceiverParameter")
val Microservice.databaseApi: DatabaseApi
    get() = DatabaseHolder.api
