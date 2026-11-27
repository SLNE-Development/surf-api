package dev.slne.surf.api.paper.sharedbean.test

import dev.slne.surf.api.paper.server.harness.BrokerContainers
import dev.slne.surf.api.paper.server.harness.PaperServer
import dev.slne.surf.api.paper.server.harness.PaperServerInputs
import dev.slne.surf.api.paper.server.harness.ServerDirectory
import dev.slne.surf.api.paper.sharedbean.consumer.SharedBeanReport
import dev.slne.surf.api.paper.sharedbean.provider.SharedGreeting
import dev.slne.surf.api.paper.sharedbean.provider.SharedGreetingImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * Boots a real Paper server with surf-api and two plugins of its own, and proves that a
 * `@PluginApi` bean exported by one of them reaches the other through surf-api's Spring contexts.
 *
 * The unit tests of `surf-api-spring-impl` build plugin contexts inside one JVM class loader. What
 * they cannot exercise is the thing the design rests on: real `PaperPluginClassLoader`s, built by a
 * real server out of real plugin jars, contexts built in Paper's bootstrap phase, and surf-api
 * sitting beside the plugins rather than above them.
 *
 * ### What is asserted
 *
 * The consuming plugin takes the providing plugin's interface as a **constructor parameter** and
 * logs one report line derived from the injected instance:
 *
 * - it carries [SharedGreeting.GREETING], which only the providing plugin's implementation can
 *   produce, so the consumer made a real interface call on the injected object
 * - it says `sameLoader=true`, so the interface the consumer casts to and the one the instance
 *   implements are the same `Class`
 * - the remaining fields cover what [EXPECTED_FIELDS] lists: private beans stay private, the plugin
 *   instance resolves, the context class loader, `@ConditionalOnPlugin`, CGLIB configuration
 *   classes and the absence of JVM shutdown hooks
 * - `scopeSurvivesLoad` says that a coroutine launched in `onLoad` outlived the load phase and
 *   reached the server thread, which the enable hook waited for
 * - `surfConfig` is read through the `ConfigHandle` surf-api registered for the consumer's
 *   `@SurfConfig` class, from a file in a subfolder of its data folder
 * - `commandMessage` says that the message of an exception from `SurfCommandUtil` renders
 *   through the Brigadier message conversion of the Paper api as the component it was created
 *   from
 * - the suspending command logs [EXPECTED_SCOPE_COMMAND_FIELDS]: its default scope is the
 *   consumer's own, and it starts on the server thread
 * - `redis` and `redisson` say that the consumer's injected `RedisApi` wrote a sync value and read
 *   it back from Redis, and that the injected `RedissonClient` proxy forwarded a command, both
 *   after surf-api connected the plugin
 * - the NATS command logs [EXPECTED_NATS_COMMAND_FIELDS]: a call through a service proxy reached
 *   the consumer's own `@NatsService` over the broker, a published event reached its
 *   `@NatsListener`, and an `ItemStack` survived the surf-nats serializer round trip on the server
 * - the inventory command logs [EXPECTED_INVENTORY_COMMAND_FIELDS]: insertion plans on the server's
 *   own inventories predict what fits, alone and for several items together, commits follow each
 *   insertion policy, drops land with the right owner and never despawn, a player keeps armor,
 *   crafting grid and, unless included, the offhand; removal plans report what is missing, alone
 *   and for several items together, ignore items that are not similar, commits follow each removal
 *   policy, a player keeps worn armor and the offhand unless included, and planning or committing
 *   off the owning thread throws; the suspending variants plan and commit from an async thread on a
 *   plain, a block and an entity inventory, and a cancelled commit leaves the inventory unchanged.
 *   The chest minecart stands in for a player: both switch to the thread of the holding entity,
 *   and the detached test player is not added to a world, so the probe cannot switch to its thread
 * - after `stop`, the disable hook logs `disableJoin=returned`: it cancelled and joined a
 *   coroutine that was suspended on server thread work while that thread was blocked in the hook
 *
 * Redis and NATS run in Docker containers the test starts, and the server reads their addresses
 * from the environment variables of surf-api's configuration. A machine without Docker skips the
 * test.
 *
 * After the server stopped, the phase lines of both plugins must appear in [EXPECTED_PHASES]
 * order: bootstrap, load and enable provider first, disable consumer first, and the export
 * destroyed once, by its provider.
 *
 * ### Why it skips rather than fails
 *
 * Its inputs are a 55 MB server jar and three third-party plugin jars from two hosts. A missing
 * network, a moved release or a changed checksum is not a defect in this repository, and a test
 * that goes red for one of those teaches people to ignore it, so each case aborts with the reason.
 * Everything downstream of a successful assembly — a server that will not start, a plugin that will
 * not load, a missing line — fails, with the tail of the server log attached.
 *
 * ### Running it
 *
 * ```
 * ./gradlew :surf-api-paper-shared-bean-test:paperSharedBeanBootTest
 * ```
 *
 * `-Psurf.bootTest.jvmArgs="<args>"` adds whitespace-separated JVM arguments to the test JVM, which
 * downloads, and to the server process, for example a trust-store setting for a machine behind a
 * TLS-intercepting proxy. `-Psurf.paperServerTest.serverJar=<path>` runs another server jar instead
 * of the pinned Paper jar.
 *
 * It lives in a source set of its own and belongs to no lifecycle task, so neither `check`, nor
 * `test`, nor CI reaches it. The server assembly, downloads, process control and brokers come from
 * `surf-api-paper-server-harness`, which the bot-driven scenario tests share.
 */
class PaperSharedBeanBootTest {

    private var startedBrokers: BrokerContainers? = null

    @AfterEach
    fun stopBrokers() {
        startedBrokers?.close()
    }

    @Test
    fun `a shared bean crosses from one plugin to another on a real Paper server`() {
        val inputs = PaperServerInputs.fromSystemProperties()
        val directory = ServerDirectory.assemble(
            inputs.workDir.resolve("server"),
            inputs.serverJar(),
            inputs.thirdPartyPlugins() + inputs.pluginJars(),
            properties = mapOf("motd" to "surf-api shared-bean boot test"),
        )

        val brokers = BrokerContainers.start().also { startedBrokers = it }
        val server = PaperServer(
            directory = directory,
            javaLauncher = inputs.javaLauncher,
            jvmArgs = inputs.jvmArgs,
            environment = brokers.environment,
            interesting = INTERESTING,
        )

        val line = try {
            server.start()
            server.awaitStarted()

            // A plugin whose plugin context cannot be built does not load at all, which the log
            // says; its report line then never comes.
            val reportLine = server.awaitLine("the consuming plugin's report line") { logged ->
                SharedBeanReport.MARKER in logged
            }

            // The listener bean reacts to the end of startup; the command beans are run from the
            // console. All prove that surf-api registered the beans in the enable phase.
            server.awaitLine(LISTENER_LINE)
            server.sendConsoleCommand(SharedBeanReport.COMMAND)
            server.awaitLine(COMMAND_LINE)
            server.sendConsoleCommand(SharedBeanReport.SCOPE_COMMAND)
            server.awaitLine(SCOPE_COMMAND_LINE)
            server.sendConsoleCommand(SharedBeanReport.NATS_COMMAND)
            server.awaitLine(NATS_COMMAND_LINE)
            server.sendConsoleCommand(SharedBeanReport.INVENTORY_COMMAND)
            server.awaitLine(INVENTORY_COMMAND_LINE)

            reportLine
        } finally {
            server.stop()
        }

        if (SharedGreeting.GREETING !in line) {
            server.fail(
                "The consumer logged its line, but not the greeting only the provider's " +
                    "implementation can produce, so it did not call across the plugin " +
                    "boundary:\n  | $line",
            )
        }

        val missing = EXPECTED_FIELDS.filterNot { it in line }

        if (missing.isNotEmpty()) {
            server.fail("The consumer's report line lacks $missing:\n  | $line")
        }

        // The suspending command's own line: it is logged after the report, so it is not part of
        // it.
        val scopeLine = server.linesContaining(listOf(SCOPE_COMMAND_LINE)).last()
        val missingScopeFields = EXPECTED_SCOPE_COMMAND_FIELDS.filterNot { it in scopeLine }

        if (missingScopeFields.isNotEmpty()) {
            server.fail("The suspending command's line lacks $missingScopeFields:\n  | $scopeLine")
        }

        // The NATS round trips run from the console after the enable phase, so they log a line of
        // their own as well.
        val natsLine = server.linesContaining(listOf(NATS_COMMAND_LINE)).last()
        val missingNatsFields = EXPECTED_NATS_COMMAND_FIELDS.filterNot { it in natsLine }

        if (missingNatsFields.isNotEmpty()) {
            server.fail("The NATS command's line lacks $missingNatsFields:\n  | $natsLine")
        }

        val inventoryLine = server.linesContaining(listOf(INVENTORY_COMMAND_LINE)).last()
        val inventoryFields = inventoryLine.substringAfter(INVENTORY_COMMAND_LINE).split(' ')
        val missingInventoryFields = EXPECTED_INVENTORY_COMMAND_FIELDS.filterNot { field ->
            field in inventoryFields
        }

        if (missingInventoryFields.isNotEmpty()) {
            server.fail(
                "The inventory command's line lacks $missingInventoryFields:\n  | $inventoryLine",
            )
        }

        // Logged by the consumer's disable hook, so it can only appear once the server stopped.
        server.awaitLine(DISABLE_JOIN_MARKER)

        if (server.linesContaining(listOf(DISABLE_JOIN_LINE)).isEmpty()) {
            val seen = server.linesContaining(listOf(DISABLE_JOIN_MARKER))
            server.fail(
                "The disable hook did not get its coroutine joined while the server thread was " +
                    "blocked:\n" + seen.joinToString("\n") { "  | $it" },
            )
        }

        val failures = server.linesContaining(FAILURE_MARKERS)

        if (failures.isNotEmpty()) {
            server.fail(
                "A surf plugin's context failed:\n" + failures.joinToString("\n") { "  | $it" },
            )
        }

        val phases = server.phases()

        if (phases != EXPECTED_PHASES) {
            server.fail(
                "The plugins ran their lifecycle phases out of order.\n" +
                    "Expected: $EXPECTED_PHASES\nActual:   $phases",
            )
        }
    }

    /**
     * The phases both test plugins logged, as `provider:load` and the like, in log order.
     */
    private fun PaperServer.phases(): List<String> {
        return lines()
            .mapNotNull { PHASE_LINE.find(it) }
            .map { "${it.groupValues[1]}:${it.groupValues[2]}" }
    }

    private companion object {

        /**
         * What makes a log line worth quoting in a failure.
         *
         * The three plugin names and the two markers, so a reader can see which plugin contexts got
         * as far as running their beans; and the exception types a plugin context fails with when
         * its dependencies cannot be satisfied.
         */
        val INTERESTING = listOf(
            "surf-paper-api",
            "SurfSharedBeanProvider",
            "SurfSharedBeanConsumer",
            SharedBeanReport.MARKER,
            SharedGreetingImpl.PROVIDER_MARKER,
            "UnsatisfiedDependencyException",
            "PluginContextException",
        )

        /**
         * The fields of the consumer's report line as they read when every expectation holds.
         *
         * The provider's internals stay invisible, `Plugin` and the concrete plugin class both
         * resolve to the consumer itself, the context class loader is the plugin's during bean
         * creation, only the LuckPerms adapter exists because PlaceholderAPI is absent,
         * configuration classes get their CGLIB subclass, no context registered a JVM shutdown
         * hook, the root's `Server` is the one the plugin runs on, and the plugin's logger bean is
         * named like its own logger.
         */
        val EXPECTED_FIELDS = listOf(
            "sameLoader=true",
            "privateVisible=false",
            "pluginIsSelf=true",
            "ownPluginIsSelf=true",
            "pluginIdentity=true",
            "proxyEquals=true",
            "tccl=true",
            "probes=[LuckPermsProbe]",
            "configProxy=true",
            "luckPermsBean=true",
            "surfEvent=true",
            "shutdownHooks=0",
            "logImpl=SLF4JLocationAwareLog",
            "scopeSurvivesLoad=true",
            "surfConfig=configured",
            "redis=ok",
            "redisson=ok",
            "serverBean=true",
            "pluginLogger=true",
            "commandMessage=ok",
        )

        /**
         * The fields of the suspending command's line: its job descends from the consumer's own
         * coroutine scope, and it started on the server thread.
         */
        val EXPECTED_SCOPE_COMMAND_FIELDS = listOf(
            "commandScope=consumer",
            "commandThread=main",
        )

        /**
         * What the consumer's listener bean logs when the server finished starting.
         */
        val LISTENER_LINE = "${SharedBeanReport.SPIKE_MARKER} listener=serverLoad"

        /**
         * What the consumer's command bean logs when the console runs it.
         */
        val COMMAND_LINE = "${SharedBeanReport.SPIKE_MARKER} command=executed"

        /**
         * What the consumer's suspending command bean logs when the console runs it.
         */
        val SCOPE_COMMAND_LINE = "${SharedBeanReport.SPIKE_MARKER} commandScope="

        /**
         * What the consumer's NATS command bean logs when the console runs it, whatever the
         * outcome.
         */
        val NATS_COMMAND_LINE = "${SharedBeanReport.SPIKE_MARKER} nats="

        /**
         * The fields of the NATS command's line: a service call through a service proxy and an
         * event to the consumer's own listener both went through the broker and came back, and an
         * item stack survived the serializer round trip on the server.
         */
        val EXPECTED_NATS_COMMAND_FIELDS = listOf(
            "nats=ok",
            "natsEvent=ok",
            "natsItemStack=ok",
        )

        /**
         * What the consumer's inventory command bean logs when the console runs it, whatever the
         * outcome.
         */
        val INVENTORY_COMMAND_LINE = "${SharedBeanReport.SPIKE_MARKER} inventory "

        /**
         * The fields of the inventory command's line, worked out by hand from each scenario.
         *
         * An insertion plan reads `fits/leftover`, or `fits/type:leftover,...` per item; a commit
         * adds `inserted/leftover` and what the inventory holds afterwards, and a drop what lies
         * on the ground. A removal plan reads `isAvailable/missing` in the same way, and its
         * commit adds `removed/missing`. The off-thread fields name the exception that planning
         * or committing threw on `Dispatchers.Default`. The async fields read
         * `fits/inserted/isAvailable/removed` for inserting 10 dirt and removing 4 from
         * `Dispatchers.Default`, or `cancelled/dirt` for a commit cancelled before it ran.
         */
        val EXPECTED_INVENTORY_COMMAND_FIELDS = listOf(
            "insertEmpty=true/0",
            "insertPartial=false/42",
            "insertNearlyFull=false/6",
            "insertMultiStack=false/5",
            "insertMaxStack16=false/6",
            "insertMaxStack99=false/9",
            "insertCommit99=891/0/slot:99",
            "insertCommit=true/0/200/0/dirt:450",
            "insertCommitNotFitting=REJECT:0/640/dirt:0,PARTIAL:576/64/dirt:576",
            "insertReject=true/0/0/128/dirt:0",
            "insertPlayer=false/8/32/0/dirt:64/armor:AIR,AIR,AIR,DIAMOND_HELMET/offhand:AIR/" +
                "crafting:AIR,AIR,AIR,AIR,AIR",
            "insertSeveral=alone:true,true/false/DIRT:0,COBBLESTONE:64",
            "insertSeveralMap=false/DIRT:0,COBBLESTONE:64",
            "insertSeveralCommit=DIRT:64/0,COBBLESTONE:0/64/64/64/dirt:64/cobblestone:0",
            "insertSimilarMerged=1/30/0",
            "insertPolicyPartial=true/0/64/64/dirt:64",
            "insertPolicyDropPlayer=true/0/0/100/dropped:100/owner:ok/unlimited:true",
            "insertPolicyDropBlock=true/0/0/30/dropped:30/owner:ok/unlimited:true",
            "insertDropNoLocation=IllegalStateException/dirt:0",
            "insertOffhand=false/40/true/0/40/0/offhand:DIRT:40",
            "insertOffThreadPlan=IllegalStateException",
            "insertOffThreadCommit=IllegalStateException",
            "insertOffThreadPlayer=IllegalStateException",
            "removeSpread=true/0/false/52",
            "removeCommit=true/0/100/0/dirt:48",
            "removeSeveral=false/DIRT:0,COBBLESTONE:10",
            "removeSeveralMap=true/DIRT:0,COBBLESTONE:0",
            "removeSeveralCommit=DIRT:30/0,COBBLESTONE:10/10/40/10/dirt:34/cobblestone:0",
            "removeSeveralReject=true/0/0/40/dirt:64/cobblestone:5",
            "removeSimilarMerged=1/30/0",
            "removeNotSimilar=false/10/10/10/dirt:64",
            "removePolicyReject=true/0/0/50/dirt:30",
            "removePolicyPartial=true/0/30/20/dirt:0",
            "removeArmor=false/1/true/0/2/0/helmets:0",
            "removeArmorUntouched=1/1/head:DIAMOND_HELMET",
            "removeOffhand=false/5/true/0/15/0/offhand:AIR/dirt:0",
            "removeOffhandUntouched=10/5/offhand:DIRT:5",
            "removeOffThreadPlan=IllegalStateException",
            "removeOffThreadCommit=IllegalStateException",
            "removeOffThreadPlayer=IllegalStateException",
            "asyncPlain=true/10/true/4/dirt:6",
            "asyncPlainOnTickThread=true/10/true/4/dirt:6",
            "asyncBlock=true/10/true/4/dirt:6",
            "asyncEntity=true/10/true/4/dirt:6",
            "asyncBlockingStillThrows=IllegalStateException",
            "asyncInsertCancelled=true/dirt:0",
            "asyncRemoveCancelled=true/dirt:10",
            "asyncCancelledOnOwner=true/dirt:0",
            "asyncOutsidePlugin=IllegalStateException",
        )

        /**
         * The prefix of the line the consumer's disable hook logs for its coroutine join, whatever
         * the outcome.
         */
        val DISABLE_JOIN_MARKER = "${SharedBeanReport.SPIKE_MARKER} disableJoin="

        /**
         * The disable hook's line when its coroutine, suspended on server thread work, was
         * cancelled and joined while that thread was blocked in the hook.
         */
        val DISABLE_JOIN_LINE = "${DISABLE_JOIN_MARKER}returned"

        /**
         * Log lines that mean a surf plugin's context failed somewhere, even if the report
         * appeared.
         */
        val FAILURE_MARKERS = listOf("Failed to run bootstrapper", "PluginContextException")

        /**
         * A phase line of either test plugin; group 1 is the plugin role, group 2 the phase.
         */
        val PHASE_LINE = Regex(
            """${SharedBeanReport.SPIKE_MARKER} (provider|consumer) phase=(\w+)""",
        )

        /**
         * The phases in the order the design promises: bootstrap, load and enable go provider
         * first, disable goes consumer first, and the export is destroyed once, by its provider,
         * last.
         */
        val EXPECTED_PHASES = listOf(
            "provider:bootstrap", "consumer:bootstrap",
            "provider:load", "consumer:load",
            "provider:enable", "consumer:enable",
            "consumer:disable", "provider:disable",
            "provider:destroy",
        )
    }
}
