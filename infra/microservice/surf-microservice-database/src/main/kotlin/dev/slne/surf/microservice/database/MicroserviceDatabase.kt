package dev.slne.surf.microservice.database

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.database.integration.DatabaseHolder

/**
 * Gets the database of the running microservice.
 *
 * Available from `onBootstrap` until the end of `onDisable`, after the migrations ran. Pass it to Exposed's
 * `suspendTransaction`:
 *
 * ```kotlin
 * val count = suspendTransaction(db = database) {
 *     PunishmentTable.selectAll().count()
 * }
 * ```
 *
 * @throws IllegalStateException if the database integration is not running
 */
public val Microservice.database: R2dbcDatabase
    get() = databaseApi.database

/**
 * Gets the [DatabaseApi] that owns [database], for example to run additional migrations.
 *
 * Available from `onBootstrap` until the end of `onDisable`. The database integration shuts it down, so do not call
 * [DatabaseApi.shutdown].
 *
 * @throws IllegalStateException if the database integration is not running
 */
@Suppress("UnusedReceiverParameter")
public val Microservice.databaseApi: DatabaseApi
    get() = DatabaseHolder.api
