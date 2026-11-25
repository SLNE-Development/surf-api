package dev.slne.surf.api.spring.context.fixture.boot.missingbean

import dev.slne.surf.api.spring.context.fixture.boot.Ticker
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(Ticker::class)
class DefaultTicker : Ticker
