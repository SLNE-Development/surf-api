package dev.slne.surf.api.spring.context.fixture.lifecycle

import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.api.spring.context.fixture.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

/**
 * Declared after its dependency's dependent on purpose; initialization order must still win.
 */
@Service
class Dependent(private val base: Base) : SurfLifecycle {
    override suspend fun onLoad() = Journal.record("load:dependent")
    override suspend fun onEnable() = Journal.record("enable:dependent")

    /**
     * Times out on purpose: a hook's own cancellation must not cost the others their onDisable.
     */
    override suspend fun onDisable() {
        Journal.record("disable:dependent")
        withTimeout(1) { delay(1_000) }
    }
}

@Service
class Base : SurfLifecycle {
    override suspend fun onLoad() {
        delay(1)
        withContext(Dispatchers.IO) {
            Journal.record("load:base:tccl=${Thread.currentThread().contextClassLoader?.name}")
        }
    }

    override suspend fun onEnable() = Journal.record("enable:base")
    override suspend fun onDisable(): Unit = error("disable of base fails on purpose")
}

@Service
@Order(1)
class Early : SurfLifecycle {
    override suspend fun onLoad() = Journal.record("load:early")
    override suspend fun onEnable() = Journal.record("enable:early")
    override suspend fun onDisable() = Journal.record("disable:early")
}
