package dev.slne.surf.api.spring.context.fixture.phase

import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.api.spring.context.ContextPhaseListener
import dev.slne.surf.api.spring.context.fixture.Journal
import org.springframework.stereotype.Service

/**
 * A plugin hook whose phases the listener has to surround.
 */
@Service
class PhaseHook : SurfLifecycle {
    override suspend fun onEnable() = Journal.record("enable:hook")
    override suspend fun onDisable() = Journal.record("disable:hook")
}

/**
 * Records when the context calls it relative to the hook.
 */
@Service
class PhaseRecorder : ContextPhaseListener {
    override suspend fun afterEnable() = Journal.record("afterEnable")
    override suspend fun beforeDisable() = Journal.record("beforeDisable")
}
