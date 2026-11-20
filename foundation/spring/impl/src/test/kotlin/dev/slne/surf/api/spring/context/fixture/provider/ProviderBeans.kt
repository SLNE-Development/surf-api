package dev.slne.surf.api.spring.context.fixture.provider

import dev.slne.surf.api.spring.context.fixture.Apple
import dev.slne.surf.api.spring.context.fixture.Greeting
import dev.slne.surf.api.spring.context.fixture.Helper
import dev.slne.surf.api.spring.context.fixture.Journal
import dev.slne.surf.api.spring.context.fixture.Pear
import dev.slne.surf.api.spring.context.fixture.Repository
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service

@Service
class ProviderGreeting(private val helper: ProviderHelper) : Greeting, InitializingBean, DisposableBean {
    override fun greet() = "hello from provider"
    override fun afterPropertiesSet() = Journal.record("provider.init")
    override fun destroy() = Journal.record("provider.destroy")
}

@Service
class ProviderHelper : Helper

@Service
class AppleRepository : Repository<Apple> {
    override fun describe() = "apples"
}

@Service
class PearRepository : Repository<Pear> {
    override fun describe() = "pears"
}
