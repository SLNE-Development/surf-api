package dev.slne.surf.api.spring.context.fixture.boot.nounique

import dev.slne.surf.api.spring.context.fixture.boot.Ticker
import org.springframework.stereotype.Service

@Service
class FastTicker : Ticker

@Service
class SlowTicker : Ticker

@Service
class TickerUser(val ticker: Ticker)
