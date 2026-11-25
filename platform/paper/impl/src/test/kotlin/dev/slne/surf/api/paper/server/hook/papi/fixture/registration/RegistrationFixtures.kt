package dev.slne.surf.api.paper.server.hook.papi.fixture.registration

import dev.slne.surf.api.paper.hook.papi.PapiExpansion
import dev.slne.surf.api.paper.hook.papi.Placeholder
import org.bukkit.OfflinePlayer
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import java.util.*

@PapiExpansion(
    "registered",
    placeholders = [BeanPlaceholders::class, PlainPlaceholders::class, ObjectPlaceholders::class],
)
class RegisteredExpansion {

    @Placeholder("own")
    fun own(player: OfflinePlayer?): String = "own"
}

@Component
class BeanPlaceholders {
    val id = UUID.randomUUID().toString()

    @Placeholder("bean")
    fun bean(player: OfflinePlayer?): String = id
}

class PlainPlaceholders(private val beanPlaceholders: BeanPlaceholders) : DisposableBean {
    var isDestroyed = false
        private set

    @Placeholder("plain")
    fun plain(player: OfflinePlayer?): String = "plain:${beanPlaceholders.id}"

    override fun destroy() {
        isDestroyed = true
    }
}

object ObjectPlaceholders {

    @Placeholder("object")
    fun value(player: OfflinePlayer?): String = "object"
}
