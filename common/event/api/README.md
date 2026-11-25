# surf-api-event-api

The surf event bus contracts.

- the `SurfEvent` hierarchy: `SurfSyncEvent`, `SurfAsyncEvent` and `SurfCancellableEvent`
- `@SurfEventHandler` and `SurfEventPriority`
- the `SurfEventBus` contract

The bus implementation and its invokers live in `surf-api-core-impl`. See the `surf-api-events`
skill.
