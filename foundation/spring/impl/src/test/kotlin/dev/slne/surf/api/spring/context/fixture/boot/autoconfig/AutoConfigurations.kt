package dev.slne.surf.api.spring.context.fixture.boot.autoconfig

import dev.slne.surf.api.spring.context.fixture.boot.Named
import dev.slne.surf.api.spring.context.fixture.boot.Ticker
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

// Two pairs whose second configuration backs off when the first one ran before it. The importers
// in the subpackages list them in opposite orders.

@AutoConfiguration
class EarlierAutoConfiguration {

    @Bean
    fun earlierNamed(): Named = Named("earlier")
}

@AutoConfiguration(after = [EarlierAutoConfiguration::class])
class LaterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun laterNamed(): Named = Named("later")
}

@AutoConfiguration
@AutoConfigureBefore(SecondTickerAutoConfiguration::class)
class FirstTickerAutoConfiguration {

    @Bean
    fun firstTicker(): Ticker = NamedTicker("first")
}

@AutoConfiguration
class SecondTickerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun secondTicker(): Ticker = NamedTicker("second")
}

data class NamedTicker(val name: String) : Ticker
