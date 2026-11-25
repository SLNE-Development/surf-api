# surf-api-standalone

The coordinate `dev.slne.surf.api:surf-api-standalone` that downstream projects depend on, for plain
JVM services. The bootstrap creates the root context itself, so the API cannot be separated from its
implementation.

It carries no code and re-exports `surf-api-standalone-impl` through `api`, so the modules behind it
can be split or renamed without breaking downstream builds.
