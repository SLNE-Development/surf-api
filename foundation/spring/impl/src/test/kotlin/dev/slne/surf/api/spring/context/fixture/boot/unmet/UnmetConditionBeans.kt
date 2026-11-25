package dev.slne.surf.api.spring.context.fixture.boot.unmet

import dev.slne.surf.api.spring.context.fixture.boot.HIDDEN_TYPE
import dev.slne.surf.api.spring.context.fixture.boot.Ticker
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service

@Configuration
class UnmetConditionConfiguration {

    @Bean
    @ConditionalOnClass(name = [HIDDEN_TYPE])
    fun hiddenTicker(): Ticker = object : Ticker {}
}

@Service
class TickerConsumer(val ticker: Ticker)
