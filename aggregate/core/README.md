# surf-api-core

The coordinate `dev.slne.surf.api:surf-api-core` that downstream projects depend on, for the
platform-independent API.

It carries no code and re-exports `surf-api-core-api` and `surf-api-config-api` through `api`, so
the modules behind it can be split or renamed without breaking downstream builds.
