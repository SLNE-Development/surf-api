package dev.slne.surf.api.spring.context.fixture.badexport

import dev.slne.surf.api.spring.context.fixture.Clock
import org.springframework.stereotype.Service

@Service
class LifecycleClock : Clock {
    override fun start() {}
    override fun stop() {}
    override fun isRunning() = false
}
