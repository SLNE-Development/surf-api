# surf-api-luckperms-api

Helpers for reading LuckPerms data about a player or an audience.

- `SurfLuckPerms` and the `User`/`UUID`/`Audience` extensions

LuckPerms itself is `compileOnlyApi`: consumers compile against it, and the platform provides it at
runtime. `SurfLuckPermsImpl` lives in `surf-api-core-impl`.
