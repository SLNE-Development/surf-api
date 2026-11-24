package dev.slne.surf.api.spring.context.fixture.deferred

import dev.slne.surf.api.spring.SurfBootstrapLifecycle
import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.api.spring.context.fixture.Journal
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Service

/**
 * Lives in the bootstrap context; its own load hook runs before those of the full context.
 */
@Service
class BootstrapBean : SurfLifecycle, SurfBootstrapLifecycle {
    init {
        Journal.record("created:bootstrap")
    }

    override suspend fun onBootstrap() = Journal.record("hook:bootstrap")
    override suspend fun onLoad() = Journal.record("hook:load:bootstrap")
}

/**
 * Lives in the full context and injects the bootstrap bean from the context above.
 */
@Service
class LateBean(private val bootstrapBean: BootstrapBean) : SurfLifecycle,
    SmartInitializingSingleton {
    init {
        Journal.record("created:late")
    }

    override fun afterSingletonsInstantiated() = Journal.record("initialized:late")
    override suspend fun onLoad() = Journal.record("hook:load:late")
}
