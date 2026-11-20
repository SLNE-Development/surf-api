# surf-api

surf-api is a Kotlin library for Paper, Velocity, Minestom and standalone JVM services. Plugins, the
surf-api runtime and microservices are wired with Spring application contexts.

- **Navigation:** use ProjectAtlas for the whole repository, `infra/` included. Load the installed
  `projectatlas` skill and follow it: start with `atlas_session_brief`, then file summaries, relations
  and exact slices before broad source reads. The index lives in `.projectatlas/`; run `atlas_init`
  only when it is missing, and `atlas_watch_once` after files changed.
- **Skills:** `.claude/skills/` holds one skill per area of the codebase. Load the matching skill before
  you change code, build scripts or docs in that area.
- **Architecture:** `docs/spring-migration.md` records how contexts, lifecycle, coroutines, NATS, Redis,
  microservices and the database are wired, and why. Read the matching section before you change any of
  that wiring.
- **Consumer-facing changes:** `MIGRATION-4.0.md` is the upgrade guide for downstream projects. When a
  change removes or changes public API, a published coordinate or runtime behaviour, record it there.
- **infra:** `infra/nats`, `infra/redis`, `infra/database-r2dbc` and `infra/microservice` are part of this
  build and follow the same rules as the rest of the repository. Their READMEs document each library.
- **Internal API:** mark declarations that only surf-api's modules use `@InternalSurfApi`; the ABI
  dumps leave them out. A declaration that a public `inline` function references ends up in plugin
  bytecode: mark it `@PublishedSurfApi`, which the dumps keep. `checkInlineInternalApi` enforces this.
  The dump filter skips nested classes, so a companion or `Key` object of an internal class carries
  the marker itself; `checkAbiDumpNesting` enforces this.
- **Golden files:** the JS tests compare the protoc plugin output with
  `infra/nats/surf-nats-core/src/jsTest/protoc/golden`. Keep those files exactly as generated.
