package dev.slne.surf.microservice.reliability.support

import io.github.amadeusitgroup.testcontainers.nats.NatsContainer
import org.testcontainers.mariadb.MariaDBContainer
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Lazily started, JVM-wide containers shared by all tests.
 */
object Containers {
    val postgres: PostgreSQLContainer by lazy { PostgreSQLContainer("postgres:17").also { it.start() } }
    val mariadb: MariaDBContainer by lazy { MariaDBContainer("mariadb:11").also { it.start() } }
    val nats: NatsContainer by lazy { NatsContainer("nats:2-alpine").withJetStream().also { it.start() } }
}
