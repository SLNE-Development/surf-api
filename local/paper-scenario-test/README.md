# surf-api-paper-scenario-test

Scenario tests with real player connections against a real Paper server. Bots built on
MCProtocolLib join the offline-mode server and act like players. The probe plugin of
`local/paper-scenario-probe` reports what the server sees. The plugin of
`local/paper-scenario-owner` owns temporary inventory sessions that must outlive their owner.
The server comes from
`local/paper-server-harness`, which the Paper shared-bean boot test also uses.

Local only, because it downloads and boots a real server. Missing inputs, a checksum mismatch or a
machine without Docker make the tests skip, so check that the output says `PASSED`.

## Running it

```shell
./gradlew :surf-api-paper-scenario-test:paperScenarioTest
```

Gradle properties:

- `-Psurf.bootTest.jvmArgs="<args>"`: whitespace-separated JVM arguments for the test JVM and the
  server, for example `-Djavax.net.ssl.trustStoreType=Windows-ROOT` behind a TLS-intercepting
  proxy
- `-Psurf.paperServerTest.serverJar=<path>`: runs this server jar, such as a locally built
  SurfCanvas jar, instead of the pinned Paper jar. The test prints which jar each run starts

The server directory is `build/paper-server-test/server`. Each test class starts on a fresh world;
the console output of every run is in `harness-console.log` there.

## What a scenario has

- `ScenarioServer`: the running server, its brokers and its bots. `connect(name)` joins a bot and
  waits for the server's join event. `stop()`, `kill()` and `start()` stop the server normally,
  kill the process without saving, and start it again on the same world.
- `Bot`: one player connection. It selects a hotbar slot, right-clicks a block to open a chest or
  crafting table, left-clicks a slot of the open container or its own inventory (which picks an
  item up onto the cursor), closes the container, waits for its death, respawns and disconnects.
- `Probe`: sends requests to the probe plugin and waits for its replies and events. `inventory`
  reports the hotbar slot, the cursor, the open view and every slot; `items` reports the dropped
  items around a position. `awaitInventory` polls until a report matches, because bot actions
  reach the server asynchronously.

Assert on what the probe reports, never on what a bot believes.

## Adding a scenario

1. Write a test class in `src/paperTest/kotlin` like `BotHarnessScenarioTest`: start a
   `ScenarioServer` in `@BeforeAll` with `TestInstance.Lifecycle.PER_CLASS` and close it in
   `@AfterAll`. Booting takes most of a minute, so group scenarios that can share a server.
2. Give every test bots with names of its own (3 to 16 letters, digits or underscores), so tests do
   not depend on their order.
3. When the scenario needs the server to do something the probe cannot yet do, add a bean to the
   probe plugin that implements `ProbeAction`, and a method on `Probe` that calls it. The action
   runs on the server thread; switch to the owner of a player, block or entity through
   `PaperDispatchers`, so the action also works on SurfCanvas.
4. When it needs a player action that `Bot` lacks, send the packet from a new `Bot` method and
   track in `Bot.PacketHandler` only what later packets need.
