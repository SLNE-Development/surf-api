package dev.slne.standalone.fixture

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.event.SurfSyncEvent
import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.api.spring.context.ContextContributor
import dev.slne.surf.api.spring.context.ContextPhaseListener
import dev.slne.surf.api.spring.context.PluginDescriptor
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The calls the fixture application received, in order.
 */
object ApplicationEvents {
    val recorded: MutableList<String> = CopyOnWriteArrayList()

    /**
     * The [FixtureEvent] sources that [FixtureEventListener] received, in order.
     */
    val handled: MutableList<String> = CopyOnWriteArrayList()
}

@ConfigSerializable
@SurfConfig("application.yml")
data class ApplicationConfig(val greeting: String = "hello")

/**
 * A bean that [RecordingContributor] registers.
 */
class ContributedBean

/**
 * Listed in the test resources' `META-INF/services`, as a surf-api module lists its contributor.
 */
class RecordingContributor : ContextContributor {

    override fun contribute(context: AnnotationConfigApplicationContext, plugin: PluginDescriptor) {
        ApplicationEvents.recorded += "contribute:${plugin.name}"
        context.registerBean<ContributedBean>()
    }
}

@Component
class ApplicationService(
    val config: ConfigHandle<ApplicationConfig>,
    val contributed: ContributedBean,
) : SurfLifecycle, ContextPhaseListener {

    override suspend fun onEnable() {
        ApplicationEvents.recorded += "onEnable"
    }

    override suspend fun afterEnable() {
        ApplicationEvents.recorded += "afterEnable"
    }

    override suspend fun beforeDisable() {
        ApplicationEvents.recorded += "beforeDisable"
    }

    override suspend fun onDisable() {
        ApplicationEvents.recorded += "onDisable"
    }
}

data class FixtureEvent(val source: String) : SurfSyncEvent()

/**
 * A bean with a `@SurfEventHandler` method, which surf-api registers without a call of its own.
 */
@Component
class FixtureEventListener {

    @SurfEventHandler
    fun onFixtureEvent(event: FixtureEvent) {
        ApplicationEvents.handled += event.source
    }
}

/**
 * Fires a [FixtureEvent] from its enable hook, so a test can see that the listeners are registered
 * before the hooks run.
 */
@Component
class FixtureEventPublisher : SurfLifecycle {

    override suspend fun onEnable() {
        FixtureEvent(ON_ENABLE_SOURCE).call()
    }

    companion object {
        const val ON_ENABLE_SOURCE = "onEnable"
    }
}
