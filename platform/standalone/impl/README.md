# surf-api-standalone-impl

The runtime for plain JVM services without a Minecraft server.

- `SurfApiStandaloneBootstrap`, which creates surf-api's root context, and `adopt` for a Spring
  Boot context
- `SurfStandaloneInstance` and the standalone implementations of the core API

The aggregate `surf-api-standalone` re-exports the whole module. See the `surf-api-standalone`
skill.
