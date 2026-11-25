# surf-api-minestom-bootstrap-impl

The `Launcher-Agent-Class` of a surf Minestom server jar.

- downloads the libraries listed by gremlin into `libraries/` on the first start and appends them
  to the class path
- adds the plugins named in the manifest and starts the delegate agents, Mixin first

Written in Java without dependencies, because it runs before any library is on the class path.
