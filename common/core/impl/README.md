# surf-api-core-impl

The platform-independent runtime shared by the Paper, Velocity, Minestom and standalone runtimes.

- `SurfApiCoreImpl` and `CoreInstance`
- the implementations of the event bus with its hidden-class invokers, the action bar, player
  lookup, messages and `SurfLuckPerms`
- the context contributor that registers the reflection proxies
- the shared Spring wiring and packet listeners

Part of every platform runtime; Paper, Velocity and Minestom shade it into their plugin or server
jar.
