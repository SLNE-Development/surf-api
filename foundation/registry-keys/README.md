# surf-api-registry-keys

Generated key objects for blocks, items, sounds and vanilla advancements.

- `BlockTypeKeys`, `ItemTypeKeys`, `SoundKeys` and `VanillaAdvancementKeys` in
  `dev.slne.surf.api.core.generated`: Kotlin `object`s whose keys are `@JvmField`s

The objects change only with `mcVersion`. `tooling/registry-key-generator` regenerates them; do not
edit them by hand. The module has no ABI dump because its surface is generated and would change
with every Minecraft version. `surf-api-core-api` re-exports it.
