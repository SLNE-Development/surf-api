package dev.slne.surf.api.spring.context.fixture.consumer

import dev.slne.surf.api.spring.context.fixture.*
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class GreetingConsumer(
    val greeting: Greeting,
    val helper: ObjectProvider<Helper>,
    val apples: Repository<Apple>,
    val pears: Repository<Pear>,
)
