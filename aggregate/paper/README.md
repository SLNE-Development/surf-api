# surf-api-paper

The coordinate `dev.slne.surf.api:surf-api-paper` that downstream projects depend on, for Paper
plugins.

It carries no code and re-exports `surf-api-paper-api` through `api`, so the modules behind it can
be split or renamed without breaking downstream builds.
