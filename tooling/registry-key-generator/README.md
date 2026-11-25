# surf-api-registry-key-generator

Regenerates the key objects of `foundation/registry-keys` from the vanilla data of `mcVersion`.

- `generateRegistryKeys` downloads the server jar through piston-meta into `build/minecraft`, runs
  the vanilla data generator and writes the Kotlin key objects into
  `foundation/registry-keys/src/main/kotlin`

Local only: never published and excluded from CI builds. After a Minecraft version bump, run
`./gradlew :surf-api-registry-key-generator:generateRegistryKeys` and commit the result.
