# surf-api-paper-impl

The surf-api plugin for Paper: the runtime that hosts surf-api's root context and every plugin's
contexts.

- the bootstrapper, `PaperSpringRuntime` and the plugin lifecycle
- implementations of the Paper API: commands, inventories, hooks, packets and time skipping
- the shadow jar that bundles Spring, the NATS and Redis integrations and the NMS modules

See the `surf-api-paper` skill and `docs/spring-migration.md`.
