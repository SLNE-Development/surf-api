# surf-api-spring-api

The Spring contracts that plugin beans are written against.

- `SurfLifecycle` and `SurfBootstrapLifecycle`, the lifecycle hooks of a bean
- `@PluginApi`, `@ConditionalOnPlugin`, `@BootstrapComponent` and `@SurfApiBean`
- `SurfContexts` and the `surfBean` delegate that surf-api's static entry points read through

Spring's `spring-context` travels with this module. See the `surf-api-components` skill and
`docs/spring-migration.md`.
