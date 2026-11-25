package dev.slne.surf.api.spring.context.fixture.boot.beans

import dev.slne.surf.api.spring.context.fixture.Greeting
import dev.slne.surf.api.spring.context.fixture.boot.RootMarker
import dev.slne.surf.api.spring.context.fixture.boot.Ticker
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service

@Service
@ConditionalOnBean(RootMarker::class)
class NeedsRootMarker

@Service
@ConditionalOnBean(Greeting::class)
class NeedsGreeting

@Service
@ConditionalOnSingleCandidate(RootMarker::class)
class NeedsSingleRootMarker

@Configuration
class BeanConditionConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun rootMarker(): RootMarker = RootMarker()

    @Bean
    @ConditionalOnMissingBean
    fun ticker(): Ticker = object : Ticker {}
}
