package dev.slne.minestom.example.plugin

import dev.slne.surf.api.minestom.player.event.PlayerJoinEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The plugin's event nodes. surf-api attaches every [EventNode] bean to the plugin's event node
 * when the plugin is enabled and removes it when the plugin is disabled.
 */
@Configuration(proxyBeanMethods = false)
class ExampleEvents {

    @Bean
    fun greetOnJoin(greeting: Greeting): EventNode<Event> {
        return EventNode.all("example-greeting").addListener(PlayerJoinEvent::class.java) { event ->
            event.player.sendMessage(Component.text(greeting.text, NamedTextColor.GREEN))
        }
    }
}
