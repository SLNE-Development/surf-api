// surf-api's cross-plugin event bus: the `SurfEvent` hierarchy with `SurfSyncEvent`,
// `SurfAsyncEvent` and the `SurfCancellableEvent` mixin, the `@SurfEventHandler` annotation,
// `SurfEventPriority`, and the `SurfEventBus` contract together with its `on`/`onAsync` helpers.
//
// It takes the shape the earlier cuts established: not one of these files names a contract still
// inside `surf-api-core-api`, so core re-exports the module with `api(...)` and everything
// compiling against surf-api-core keeps resolving the event declarations unchanged.
//
// All seven files travel together whether or not they want to: `SurfEvent` is a sealed interface,
// so its two direct subclasses have to be compiled in the same module and package as it is.
//
// `core.invoker` did not come along. The event system is not its only user -- Velocity's
// `SuspendingEventHandler` calls `HiddenInvokerUtil.isSuspendFunction` to teach Velocity's own
// `EventManager` about suspend handlers, and `InternalInvokerApi` advertises the bridge to surf-*
// plugins at large -- and the event *api* never touches it; only implementations do. It is a
// general suspend-dispatch bridge and belongs in the shared base.
//
// The implementation stays in `surf-api-core-impl`, where `SurfEventBusImpl` and its hidden-class
// invoker templates already sit. Nothing compiles against them -- an implementation is wired in at
// runtime -- so an `-impl` module of its own would buy no boundary that `checkApiBoundary` does not
// already enforce here, and would have to be threaded into the `surfRuntime` of every aggregate to
// end up on the same classpaths it reaches today. This is the call the messages cut made for the
// pagination implementations, for the same reason.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // `SurfEventBus.INSTANCE` resolves the bus with `surfBean`.
    api(projects.surfApiDiApi)
    // `InternalSurfApi`, which the `surfBean` call opts into.
    api(projects.surfApiAnnotations)
}

description = "surf-api-event-api"
