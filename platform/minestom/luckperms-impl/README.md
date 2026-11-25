# surf-api-minestom-luckperms-impl

The LuckPerms feature of a surf Minestom server: LuckPerms, built from `vendor/LuckPerms`.

A server project switches it on with `withLuckPerms()` in `surfMinestomServer { }`.

The module is included only once `vendor/LuckPerms` exists. Create it with `git submodule update
--init --recursive`, then run `./gradlew applyPatches` in `vendor/`.
