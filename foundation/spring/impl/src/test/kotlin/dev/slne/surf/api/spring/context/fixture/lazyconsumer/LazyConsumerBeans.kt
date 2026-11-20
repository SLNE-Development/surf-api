package dev.slne.surf.api.spring.context.fixture.lazyconsumer

import dev.slne.surf.api.spring.context.fixture.Apple
import dev.slne.surf.api.spring.context.fixture.Greeting
import dev.slne.surf.api.spring.context.fixture.Plum
import dev.slne.surf.api.spring.context.fixture.Repository
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class LazyGreetingUser(
    val greeting: ObjectProvider<Greeting>,
    val plums: Repository<Plum>,
    val apples: Repository<Apple>?,
)
