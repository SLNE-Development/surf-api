# surf-api-paper-nms-v26-3

The NMS bridge implementations for Minecraft 26.3.

- a `@ConditionalOnMinecraftVersion` configuration, which the runtime activates for the running
  server
- the bridge implementations as `@NmsComponent` beans
- the shared sources of `platform/paper/nms/shared-26`, except the files this module overrides

Built against the Paper dev bundle with paperweight. See the `surf-api-nms` skill.
