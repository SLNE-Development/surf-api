package dev.slne.surf.schedulingfixture

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch

// Lives outside dev.slne.surf.api, so a root context can host it as an application.

@Component
class ScheduledApplicationBean {
    val runs = CountDownLatch(2)

    @Scheduled(fixedDelay = 10)
    suspend fun count() {
        runs.countDown()
    }
}
