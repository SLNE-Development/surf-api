# surf-api-common-api

The shared base that every other domain compiles against. It has no runtime wiring.

- `logger` and the helpers in `util` (coroutines, Caffeine, dates, reflection, var handles)
- collection views, algorithms such as the voxel line tracer, math helpers and the weighted random
  selector
- the small caps table and the hidden-class invoker generator that `surf-api-reflection-api` and
  surf-nats share

`surf-api-core-api` re-exports it.
