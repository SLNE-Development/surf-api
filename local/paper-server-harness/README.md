# surf-api-paper-server-harness

Assembles and runs a real Paper server for the local Paper tests: the shared-bean boot test and the
bot-driven scenario tests.

- `PaperServerInputs`: reads what the build's `registerPaperServerTest` task hands over, downloads
  the pinned jars and checks their sha256, and picks the server jar
- `ServerDirectory`: lays out a server directory with exactly the given plugins, a free port and
  offline mode, keeping or deleting the world
- `PaperServer`: starts, stops and kills the server process, restarts it on the same directory,
  sends console commands and waits for log lines of the current run
- `BrokerContainers`: Redis and NATS in Docker, which surf-api's plugin connects to

A missing input skips the test instead of failing it. Local only.
