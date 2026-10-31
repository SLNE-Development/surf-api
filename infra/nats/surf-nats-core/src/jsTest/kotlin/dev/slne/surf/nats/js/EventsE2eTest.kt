package dev.slne.surf.nats.js

import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.js.api.EventContext
import dev.slne.surf.nats.js.api.ListenOptions
import dev.slne.surf.nats.js.event.NatsRegistration
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.fixtures.ChatMessage
import dev.slne.surf.nats.js.fixtures.ChatMessageSchema
import dev.slne.surf.nats.js.fixtures.PlayerJoined
import dev.slne.surf.nats.js.fixtures.PlayerJoinedSchema
import dev.slne.surf.nats.js.fixtures.Uuid
import dev.slne.surf.nats.js.fixtures.fields
import dev.slne.surf.nats.protocol.ProtocolHeaders
import js.objects.unsafeJso
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import dev.slne.surf.nats.js.fixtures.v2.PlayerJoinedSchema as PlayerJoinedV2Schema

class EventsE2eTest {

    // Absent options stay absent keys, as TypeScript callers pass them
    private fun listenOptions(recipients: String? = null, ignore: Boolean? = null): ListenOptions = when {
        recipients != null && ignore != null -> ListenOptions(recipients = recipients, ignoreFromThisClient = ignore)
        recipients != null -> ListenOptions(recipients = recipients)
        ignore != null -> ListenOptions(ignoreFromThisClient = ignore)
        else -> ListenOptions()
    }

    private val playerJoinedSubject = "surf.evt.surf.natsjs.test.v1.PlayerJoined"

    private fun playerJoined(name: String? = null): PlayerJoined = unsafeJso {
        if (name != null) this.name = name
        id = unsafeJso<Uuid> { value = bytesOfSize(16) }
    }

    private fun chat(text: String): ChatMessage = unsafeJso { this.text = text }

    @Test
    fun publishWritesTheMessageFullNameSubjectAndTheSurfHeaders() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("events-publisher"))
        try {
            val received = mutableListOf<Msg>()
            raw.subscribe(playerJoinedSubject) { received += it }
            raw.flush()

            client.publish(PlayerJoinedSchema, playerJoined("Steve")).await()

            eventually { received.size == 1 }
            val message = received.single()
            assertEquals(ProtocolHeaders.PROTOBUF_CONTENT_TYPE, header(message, ProtocolHeaders.CONTENT_TYPE))
            assertEquals(client.origin, header(message, ProtocolHeaders.ORIGIN))
            assertEquals(NatsConstants.VERSION, header(message, ProtocolHeaders.SENDER_VERSION))
            assertTrue(client.origin.startsWith("${client.name}@"))
            assertEquals("Steve", raw.decode(PlayerJoinedSchema, message.data).fields<PlayerJoined>().name)
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun majorVersionsOfAnEventUseDistinctSubjects() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("events-versions"))
        try {
            val v1 = mutableListOf<Msg>()
            val v2 = mutableListOf<Msg>()
            raw.subscribe(playerJoinedSubject) { v1 += it }
            raw.subscribe("surf.evt.surf.natsjs.test.v2.PlayerJoined") { v2 += it }
            raw.flush()

            client.publish(PlayerJoinedV2Schema, unsafeJso<PlayerJoined> { name = "Steve" }).await()

            eventually { v2.size == 1 }
            delay(100.milliseconds)
            assertTrue(v1.isEmpty(), "a v2 event must not reach v1 listeners: $v1")
            assertEquals("Steve", raw.decode(PlayerJoinedV2Schema, v2.single().data).fields<PlayerJoined>().name)
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun anyInstanceDeliversOncePerClientNameAndAllInstancesToEveryInstance() = e2eTest {
        val name = uniqueName("events-listener")
        val first = TestNats.client(name)
        val second = TestNats.client(name)
        val publisher = TestNats.client(uniqueName("events-publisher"))
        try {
            val any = mutableListOf<String>()
            val all = mutableListOf<String>()
            for (client in listOf(first, second)) {
                client.listen(
                    ChatMessageSchema,
                    { event, _ -> any.add("${client.origin}:${event.fields<ChatMessage>().text}") },
                    listenOptions("ANY_INSTANCE"),
                ).await()
                client.listen(
                    ChatMessageSchema,
                    { event, _ -> all.add("${client.origin}:${event.fields<ChatMessage>().text}") },
                    listenOptions("ALL_INSTANCES"),
                ).await()
            }

            publisher.publish(ChatMessageSchema, chat("hello")).await()

            eventually { any.size == 1 && all.size == 2 }
            delay(300.milliseconds)
            assertEquals(1, any.size, "ANY_INSTANCE deliveries: $any")
            assertEquals(setOf("${first.origin}:hello", "${second.origin}:hello"), all.toSet())
        } finally {
            first.close().await()
            second.close().await()
            publisher.close().await()
        }
    }

    @Test
    fun anyInstanceListenersShareTheQueueGroupOfTheirClientName() = e2eTest {
        val name = uniqueName("events-listener")
        val first = TestNats.client(name)
        val second = TestNats.client(name)
        val other = TestNats.client(uniqueName("events-other"))
        val publisher = TestNats.client(uniqueName("events-publisher"))
        val raw = rawNats()
        try {
            val group = mutableListOf<String>()
            val otherClient = mutableListOf<String>()
            for (client in listOf(first, second)) {
                client.listen(ChatMessageSchema, { _, _ -> group.add(client.origin) }, listenOptions("ANY_INSTANCE")).await()
            }
            other.listen(ChatMessageSchema, { _, _ -> otherClient.add(other.origin) }, listenOptions("ANY_INSTANCE")).await()
            // A raw member of the queue group named like the client: shares the events with first and second
            raw.subscribe("surf.evt.surf.natsjs.test.v1.ChatMessage", queue = name) { group.add("raw") }
            raw.flush()

            repeat(3) { publisher.publish(ChatMessageSchema, chat("hello-$it")).await() }

            eventually { group.size == 3 && otherClient.size == 3 }
            delay(300.milliseconds)
            assertEquals(3, group.size, "one delivery per event in the queue group '$name': $group")
            assertEquals(3, otherClient.size, "a client with another name has its own queue group: $otherClient")
        } finally {
            first.close().await()
            second.close().await()
            other.close().await()
            publisher.close().await()
            raw.close()
        }
    }

    @Test
    fun ignoreFromThisClientIsRejectedForAnyInstance() = e2eTest {
        val client = TestNats.client(uniqueName("events-any-ignore"))
        val publisher = TestNats.client(uniqueName("events-publisher"))
        try {
            val error = assertFailsWith<IllegalArgumentException> {
                client.listen(ChatMessageSchema, { _, _ -> }, listenOptions("ANY_INSTANCE", ignore = true)).await()
            }
            assertTrue(error.message!!.startsWith("ignoreFromThisClient requires recipients = ALL_INSTANCES"), error.message)

            // Nothing was subscribed: the queue group of the client stays empty
            val received = mutableListOf<String?>()
            publisher.listen(ChatMessageSchema, { event, _ -> received.add(event.fields<ChatMessage>().text) }, listenOptions()).await()
            client.publish(ChatMessageSchema, chat("after")).await()
            eventually { received.size == 1 }
        } finally {
            client.close().await()
            publisher.close().await()
        }
    }

    @Test
    fun omittedRecipientsListenOnAllInstances() = e2eTest {
        val name = uniqueName("events-listener")
        val first = TestNats.client(name)
        val second = TestNats.client(name)
        val publisher = TestNats.client(uniqueName("events-publisher"))
        try {
            val received = mutableListOf<String>()
            for (client in listOf(first, second)) {
                // An options object without recipients, as TypeScript callers pass it
                val options = ListenOptions()
                client.listen(ChatMessageSchema, { _, _ -> received.add(client.origin) }, options).await()
            }

            publisher.publish(ChatMessageSchema, chat("hello")).await()

            eventually { received.size == 2 }
            assertEquals(setOf(first.origin, second.origin), received.toSet())
        } finally {
            first.close().await()
            second.close().await()
            publisher.close().await()
        }
    }

    @Test
    fun ignoreFromThisClientAndEventContext() = e2eTest {
        val client = TestNats.client(uniqueName("events-self"))
        val other = TestNats.client(uniqueName("events-other"))
        try {
            val ignored = mutableListOf<EventContext>()
            val own = mutableListOf<EventContext>()
            val foreign = mutableListOf<EventContext>()
            client.listen(ChatMessageSchema, { _, context -> ignored.add(context) }, listenOptions(ignore = true)).await()
            client.listen(ChatMessageSchema, { _, context -> own.add(context) }, listenOptions()).await()
            other.listen(ChatMessageSchema, { _, context -> foreign.add(context) }, listenOptions()).await()

            client.publish(ChatMessageSchema, chat("self")).await()

            eventually { own.size == 1 && foreign.size == 1 }
            delay(200.milliseconds)
            assertTrue(ignored.isEmpty())
            assertTrue(own.single().isFromThisClient)
            assertEquals(client.origin, own.single().origin)
            assertFalse(foreign.single().isFromThisClient)
            assertEquals(client.origin, foreign.single().origin)
        } finally {
            client.close().await()
            other.close().await()
        }
    }

    @Test
    fun undecodableEventsAreDroppedAndRegistrationsClose() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("events-listener"))
        try {
            val received = mutableListOf<String?>()
            val registration = client.listen(
                PlayerJoinedSchema,
                { event, _ -> received.add(event.fields<PlayerJoined>().name) },
                listenOptions(),
            ).await()
            val subject = playerJoinedSubject

            // name is required; a missing content type is rejected like on the JVM
            raw.publish(subject, raw.encode(PlayerJoinedSchema, playerJoined()), raw.jvmHeaders())
            raw.publish(subject, raw.encode(PlayerJoinedSchema, playerJoined("no-content-type")))
            raw.publish(subject, raw.encode(PlayerJoinedSchema, playerJoined("Alex")), raw.jvmHeaders())

            eventually { received.isNotEmpty() }
            delay(200.milliseconds)
            assertEquals(listOf<String?>("Alex"), received)

            registration.close()
            assertTrue(registration.isClosed)
            raw.publish(subject, raw.encode(PlayerJoinedSchema, playerJoined("late")), raw.jvmHeaders())
            raw.flush()
            delay(200.milliseconds)
            assertEquals(listOf<String?>("Alex"), received)
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun listenerFailuresAndRejectedPromisesAreLoggedAndDoNotStopTheListener() = e2eTest {
        val client = TestNats.client(uniqueName("events-failing"))
        try {
            val received = mutableListOf<String?>()
            client.listen(ChatMessageSchema, { _, _ -> throw IllegalStateException("handler failed") }, listenOptions()).await()
            client.listen(
                ChatMessageSchema,
                { _, _ -> kotlin.js.Promise.reject(IllegalStateException("async handler failed")) },
                listenOptions(),
            ).await()
            client.listen(ChatMessageSchema, { event, _ -> received.add(event.fields<ChatMessage>().text) }, listenOptions()).await()

            client.publish(ChatMessageSchema, chat("one")).await()
            client.publish(ChatMessageSchema, chat("two")).await()

            eventually { received.size == 2 }
            assertEquals(listOf<String?>("one", "two"), received)
        } finally {
            client.close().await()
        }
    }

    @Test
    fun publishValidatesBeforeSending() = e2eTest {
        val client = TestNats.client(uniqueName("events-invalid"))
        try {
            val missingName = assertFailsWith<IllegalArgumentException> {
                client.publish(PlayerJoinedSchema, unsafeJso<PlayerJoined> { }).await()
            }
            assertTrue(missingName.message!!.contains("'name'"), missingName.message)

            val tooLarge = assertFails {
                client.publish(ChatMessageSchema, chat("x".repeat(1_100_000))).await()
            }
            assertTrue(tooLarge.message!!.contains("exceeding the NATS max payload"), tooLarge.message)
        } finally {
            client.close().await()
        }
        assertFailsWith<IllegalStateException> {
            client.publish(ChatMessageSchema, chat("x")).await()
        }
        assertTrue(client.isClosed)
    }

    @Test
    fun listenersOfOneClientShareTheSubscriptionOfTheirQueueGroup() = e2eTest {
        val client = TestNats.client(uniqueName("events-shared"))
        val publisher = TestNats.client(uniqueName("events-publisher"))
        try {
            val first = mutableListOf<String?>()
            val second = mutableListOf<String?>()
            client.listen(
                ChatMessageSchema,
                { event, _ -> first.add(event.fields<ChatMessage>().text) },
                listenOptions("ANY_INSTANCE"),
            ).await()
            val registration = client.listen(
                ChatMessageSchema,
                { event, _ -> second.add(event.fields<ChatMessage>().text) },
                listenOptions("ANY_INSTANCE"),
            ).await()

            repeat(3) { publisher.publish(ChatMessageSchema, chat("hello-$it")).await() }

            // Both listeners see every event the queue group delivers to this client
            eventually { first.size == 3 && second.size == 3 }
            assertEquals(listOf<String?>("hello-0", "hello-1", "hello-2"), first)
            assertEquals(first, second)

            // Closing one registration keeps the shared subscription for the other
            registration.close()
            publisher.publish(ChatMessageSchema, chat("after")).await()
            eventually { first.size == 4 }
            delay(100.milliseconds)
            assertEquals(3, second.size)
        } finally {
            client.close().await()
            publisher.close().await()
        }
    }

    @Test
    fun aHandlerMayCloseItsRegistrationWhileTheEventIsDispatched() = e2eTest {
        val client = TestNats.client(uniqueName("events-reentrant"))
        val publisher = TestNats.client(uniqueName("events-publisher"))
        try {
            val once = mutableListOf<String?>()
            val every = mutableListOf<String?>()
            var registration: NatsRegistration? = null
            registration = client.listen(
                ChatMessageSchema,
                { event, _ ->
                    once.add(event.fields<ChatMessage>().text)
                    registration!!.close()
                },
                listenOptions(),
            ).await()
            client.listen(ChatMessageSchema, { event, _ -> every.add(event.fields<ChatMessage>().text) }, listenOptions()).await()

            publisher.publish(ChatMessageSchema, chat("one")).await()
            publisher.publish(ChatMessageSchema, chat("two")).await()

            eventually { every.size == 2 }
            delay(100.milliseconds)
            assertEquals(listOf<String?>("one"), once)
            assertEquals(listOf<String?>("one", "two"), every)
            assertTrue(registration!!.isClosed)
        } finally {
            client.close().await()
            publisher.close().await()
        }
    }
}
