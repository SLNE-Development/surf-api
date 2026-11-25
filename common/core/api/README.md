# surf-api-core-api

The platform-independent contracts of surf-api and the module most code compiles against.

- `SurfApiCore` and `PlayerLookupService`
- PacketEvents extensions
- re-exports of the common domains (`common`, `messages`, `event`, `nbt`, `reflection`, `command`,
  `luckperms`, `serialization`), the registry keys and the Spring contracts

Downstream projects depend on the aggregate `surf-api-core` instead of this module.
