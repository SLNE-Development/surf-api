package dev.slne.surf.api.spring.context.fixture.lazyprovider

import dev.slne.surf.api.spring.context.fixture.Greeting
import dev.slne.surf.api.spring.context.fixture.Journal
import dev.slne.surf.api.spring.context.fixture.Plum
import dev.slne.surf.api.spring.context.fixture.Repository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * Created only when someone asks for it, and records its creation.
 */
@Lazy
@Service
class LazyGreeting : Greeting {
    init {
        Journal.record("lazy.created")
    }

    override fun greet() = "hello, lazily"
}

/**
 * A repository whose class does not fix its type argument, so only the declared type of the
 * exporting bean method says which entities it holds.
 */
class GenericRepository<T : Any>(private val description: String) : Repository<T> {
    override fun describe() = description
}

@Configuration(proxyBeanMethods = false)
class GenericExports {

    @Bean
    fun plums(): Repository<Plum> = GenericRepository("plums")
}
