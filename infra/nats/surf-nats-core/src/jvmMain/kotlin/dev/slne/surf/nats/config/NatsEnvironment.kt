package dev.slne.surf.nats.config

import dev.slne.surf.api.core.environment.env
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.runtime.ConnectRules
import kotlin.io.path.Path
import kotlin.io.path.exists

@InternalNatsApi
internal object NatsEnvironment {

    val SURF_NATS_SERVERS by env.optional(
        converter = ConnectRules::parseServers,
    )

    val SURF_NATS_TOKEN by env.optional(sensitive = true)
    val SURF_NATS_USERNAME by env.optional()
    val SURF_NATS_PASSWORD by env.optional(sensitive = true)
    val SURF_NATS_CREDENTIALS_FILE by env.optional(
        converter = { Path(it) },
        validation = {
            require("Credentials file path does not exist") { it.exists() }
        }
    )

    val SURF_NATS_CONNECTION_NAME by env.optional()
}