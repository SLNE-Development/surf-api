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
- **4.0 is the breaking release:** until 4.0.0 ships, write no compatibility shims (`@IntroducedAt`,
  `DeprecationLevel.HIDDEN` overloads, `@Deprecated` bridges, relocation POMs). Remove a public
  declaration outright and record it in `MIGRATION-4.0.md`. The global binary-compatibility rules apply
  again from 4.0.x on.
- **Opt-ins:** the root build opts every module into `@InternalSurfApi` and `@PublishedSurfApi`, and
  `infra/nats` into `InternalNatsApi`; never write `@OptIn` for a marker the module's build already opts
  into.
- **Static bean access:** an internal static accessor is a companion `@InternalSurfApi val instance by
  surfBean<T>()`, without `@JvmStatic` and without a private top-level backing property. Code that is
  itself a bean injects the dependency instead.
- **Dead internal code:** an unused declaration in an impl module or an internal package is deleted, not
  kept; public API is removed only deliberately and recorded in `MIGRATION-4.0.md`.
- **Consumers:** only Kotlin projects built with Gradle are supported. Write no Java-facing API
  (`@JvmOverloads`, `@JvmStatic` or facades only Java needs); `@JvmField`/`@JvmName` stay where they
  shape the bytecode Kotlin relies on. `*-api` modules declare third-party libraries `compileOnlyApi`,
  because the surf-api runtime jar provides them.
- **Service lookup:** surf-api's own services, Redis and NATS included, are beans of the root context,
  reached statically through `instance by surfBean<T>()`. `ServiceLoader` is reserved for
  `ContextContributor`, which must be found before any context exists.
- **Golden files:** the JS tests compare the protoc plugin output with
  `infra/nats/surf-nats-core/src/jsTest/protoc/golden`. Keep those files exactly as generated.

## Agent skills

### Issue tracker

Issues and specs live in the GitHub Issues of SLNE-Development/surf-api, handled with `gh`. See
`docs/agents/issue-tracker.md`.

### Triage labels

The five triage roles map to the repository's `status: …` and `resolution: …` labels. See
`docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repository root. See `docs/agents/domain.md`.
