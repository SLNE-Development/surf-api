package dev.slne.standalone.failing

import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.stereotype.Component

@Component
class FailingOnEnable : SurfLifecycle {

    override suspend fun onEnable() {
        error("enable failed")
    }
}
