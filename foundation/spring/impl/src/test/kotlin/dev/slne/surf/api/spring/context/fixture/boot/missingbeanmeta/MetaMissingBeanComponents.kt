package dev.slne.surf.api.spring.context.fixture.boot.missingbeanmeta

import dev.slne.surf.api.spring.context.fixture.boot.Ticker
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnMissingBean(Ticker::class)
annotation class UnlessTickerExists

@Service
@UnlessTickerExists
class MetaDefaultTicker : Ticker
