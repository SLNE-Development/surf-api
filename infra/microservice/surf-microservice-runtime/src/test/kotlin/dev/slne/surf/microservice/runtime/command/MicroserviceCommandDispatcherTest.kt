package dev.slne.surf.microservice.runtime.command

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.microservice.api.command.MicroserviceCommandResult
import dev.slne.surf.microservice.api.command.MicroserviceCommandSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MicroserviceCommandDispatcherTest {
    private val output = RecordingCommandOutput()
    private val commands = mutableListOf<MicroserviceCommand>()
    private val dispatcher = MicroserviceCommandDispatcher { label -> commands.firstOrNull { it.matches(label) } }

    @Test
    fun `runs the command with the tokenized arguments`() = runTest {
        command("echo", "say") { args -> sendLine(args.joinToString("|")) }

        val dispatch = dispatcher.dispatch("SAY \"hello world\" again", MicroserviceCommandSource.CONSOLE, output)

        assertEquals(MicroserviceCommandResult.Success, dispatch.result)
        assertEquals(listOf("hello world|again"), output.lines)
    }

    @Test
    fun `passes the source to the command`() = runTest {
        command("source") { sendLine(source.name) }

        dispatcher.dispatch("source", emptyList(), MicroserviceCommandSource.LOCAL_SOCKET, output)

        assertEquals(listOf("LOCAL_SOCKET"), output.lines)
    }

    @Test
    fun `reports an unknown command with a hint to help`() = runTest {
        val dispatch = dispatcher.dispatch("missing", MicroserviceCommandSource.CONSOLE, output)

        val failure = assertIs<MicroserviceCommandResult.Failure>(dispatch.result)
        assertTrue("missing" in failure.message)
        assertTrue("help" in failure.message)
    }

    @Test
    fun `reports an empty line as a failure`() = runTest {
        val dispatch = dispatcher.dispatch("   ", MicroserviceCommandSource.CONSOLE, output)

        assertIs<MicroserviceCommandResult.Failure>(dispatch.result)
    }

    @Test
    fun `reports an unterminated quote as a failure`() = runTest {
        command("say") { sendLine("ran") }

        val dispatch = dispatcher.dispatch("say \"hello", MicroserviceCommandSource.CONSOLE, output)

        val failure = assertIs<MicroserviceCommandResult.Failure>(dispatch.result)
        assertTrue("unterminated" in failure.message)
        assertEquals(emptyList(), output.lines)
    }

    @Test
    fun `maps a command exception to its message`() = runTest {
        command("fails") { fail("Nothing to do.") }

        val dispatch = dispatcher.dispatch("fails", MicroserviceCommandSource.CONSOLE, output)

        assertEquals(MicroserviceCommandResult.Failure("Nothing to do."), dispatch.result)
    }

    @Test
    fun `maps an unexpected exception to a failure naming the command`() = runTest {
        command("broken") { error("boom") }

        val dispatch = dispatcher.dispatch("broken", MicroserviceCommandSource.CONSOLE, output)

        assertEquals(MicroserviceCommandResult.Failure("Command 'broken' failed: boom"), dispatch.result)
    }

    @Test
    fun `maps an error to a failure so the caller gets a response`() = runTest {
        command("todo") { TODO("later") }

        val dispatch = dispatcher.dispatch("todo", MicroserviceCommandSource.CONSOLE, output)

        assertEquals(
            MicroserviceCommandResult.Failure("Command 'todo' failed: An operation is not implemented: later"),
            dispatch.result,
        )
    }

    @Test
    fun `rethrows a virtual machine error`() = runTest {
        command("oom") { throw OutOfMemoryError("simulated") }

        assertFailsWith<OutOfMemoryError> {
            dispatcher.dispatch("oom", MicroserviceCommandSource.CONSOLE, output)
        }
    }

    @Test
    fun `maps a timeout inside the command to a failure`() = runTest {
        command("slow") { withTimeout(1) { delay(1_000) } }

        val dispatch = dispatcher.dispatch("slow", MicroserviceCommandSource.CONSOLE, output)

        assertIs<MicroserviceCommandResult.Failure>(dispatch.result)
    }

    @Test
    fun `maps a cancellation thrown by the command to a failure`() = runTest {
        command("cancels") { throw CancellationException("gave up") }

        val dispatch = dispatcher.dispatch("cancels", MicroserviceCommandSource.CONSOLE, output)

        assertIs<MicroserviceCommandResult.Failure>(dispatch.result)
    }

    @Test
    fun `propagates the cancellation of the caller`() = runTest {
        command("waits") { awaitCancellation() }
        var dispatch: CommandDispatch? = null
        val job = launch { dispatch = dispatcher.dispatch("waits", MicroserviceCommandSource.CONSOLE, output) }
        runCurrent()

        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertNull(dispatch)
    }

    @Test
    fun `waits for coroutines the command launched`() = runTest {
        command("async") {
            launch {
                delay(100)
                sendLine("child done")
            }
        }

        dispatcher.dispatch("async", MicroserviceCommandSource.CONSOLE, output)

        assertEquals(listOf("child done"), output.lines)
    }

    @Test
    fun `reports a shutdown request without performing it`() = runTest {
        command("exit") { requestShutdown() }
        command("noop") {}

        assertTrue(dispatcher.dispatch("exit", MicroserviceCommandSource.CONSOLE, output).shutdownRequested)
        assertFalse(dispatcher.dispatch("noop", MicroserviceCommandSource.CONSOLE, output).shutdownRequested)
    }

    @Test
    fun `rethrows nothing for a failing command`() = runTest {
        command("throws") { throw IllegalArgumentException() }

        val dispatch = dispatcher.dispatch("throws", MicroserviceCommandSource.CONSOLE, output)

        val failure = assertIs<MicroserviceCommandResult.Failure>(dispatch.result)
        assertTrue(IllegalArgumentException::class.java.name in failure.message)
    }

    private fun command(
        name: String,
        vararg aliases: String,
        block: suspend MicroserviceCommandContext.(List<String>) -> Unit,
    ) {
        commands += object : MicroserviceCommand(name, *aliases) {
            override suspend fun MicroserviceCommandContext.execute(args: List<String>) = block(args)
        }
    }
}
