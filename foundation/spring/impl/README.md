# surf-api-spring-impl

The context machinery shared by every platform runtime.

- `SurfRootContext`, `PluginContext` and `SurfContextManager`: building, refreshing and closing the
  root and plugin contexts
- `ContextContributor` and `ContextPhaseListener`, the extension points of surf-api modules
- plugin exports and imports, component scanning, lifecycle hook ordering, and the coroutine and
  class loader elements of a plugin

See `docs/spring-migration.md` for how the contexts are wired.
