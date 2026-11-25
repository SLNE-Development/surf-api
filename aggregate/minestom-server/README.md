# surf-api-minestom-server

The coordinate `dev.slne.surf.api:surf-api-minestom-server` that downstream projects depend on, for
projects that build a runnable Minestom server.

It carries no code and re-exports `surf-api-minestom-impl` through `api`, so the modules behind it
can be split or renamed without breaking downstream builds.
