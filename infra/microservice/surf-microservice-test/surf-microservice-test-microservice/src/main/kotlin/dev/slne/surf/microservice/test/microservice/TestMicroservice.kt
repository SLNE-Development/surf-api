package dev.slne.surf.microservice.test.microservice

import com.google.auto.service.AutoService
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.test.microservice.database.tables.MessagesTable
import dev.slne.surf.microservice.test.microservice.handler.TestRabbitHandler
import dev.slne.surf.rabbitmq.api.ServerRabbitMQApi
import dev.slne.surf.surfapi.core.api.util.logger
import kotlin.io.path.Path

@AutoService(Microservice::class)
class TestMicroservice : Microservice() {
    private val serverApi = ServerRabbitMQApi.create(1, "surf-test")
    private lateinit var databaseApi: DatabaseApi

    private val log = logger()

    override suspend fun onBootstrap(args: List<String>) {
        log.atInfo().log("Running onBootstrap")

        serverApi.registerRequestHandler(TestRabbitHandler)
        serverApi.freezeAndConnect()

        databaseApi = DatabaseApi.create(Path("data/database"))

        suspendTransaction {
            SchemaUtils.create(MessagesTable)
        }

        testCommand()
    }

    override suspend fun onDisable() {
        log.atInfo().log("Running onDisable")

        databaseApi.shutdown()
    }
}