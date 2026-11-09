# Action bar, sidebar and scoreboard

`actionbar/` plus `common/core/impl/.../impl/actionbar/ActionBarServiceImpl.kt`, and the Paper
consumers under `platform/paper/api/.../sidebar/` and `.../scoreboard/`. Every `Component` here is
Adventure's text component.

## `ActionBarService`

One function, reached through the `Audience` extensions rather than directly:

```kotlin
fun sendActionBar(
    scope: CoroutineScope,
    audience: Audience,
    duration: Duration,
    interval: Duration = 1.seconds,
    fadeOut: Boolean = false,
    autoCancelPlayer: Boolean = true,
    computation: () -> Component,
    afterTick: (() -> Unit)? = null,
    onFinish: ((ActionBarFinishReason) -> Unit)? = null,
): Job
```

Two `Audience` extensions wrap it, both returning the `Job`:

```kotlin
// trailing lambda, the common case
player.sendActionBar(scope, 5.seconds) { info("Loading...") }

// with callbacks - textComputation is not last, so name it
player.sendActionBar(
    scope = scope,
    duration = 5.seconds,
    textComputation = { info("Loading...") },
    onFinish = { reason -> /* … */ },
)
```

`textComputation` is a `SurfComponentBuilder.() -> Unit`; the service-level `computation` is a plain
`() -> Component`. The service itself is a bean: `ActionBarService.instance` reads a private
`by surfBean<ActionBarService>()`, satisfied by the `@Service` `ActionBarServiceImpl` in
`surf-api-core-impl`, so it is reachable from common code on every platform.

### What it guarantees

- `require(duration.isPositive())` and `require(interval.isPositive())` - both throw
  `IllegalArgumentException` immediately, before any job is launched.
- The loop recomputes the component every tick and sleeps `interval - elapsed`, so a slow
  `computation` eats into the interval instead of drifting.
- A `computation` that throws is logged at `WARNING` with the cause, at most every 900 ms, and that
  tick sends nothing. The loop keeps going. Same for `afterTick`.
- `autoCancelPlayer = true` (default): if the audience has a UUID pointer and the core says it is a
  player, the job ends as soon as `SurfApiCore.getPlayer(uuid)` returns `null`.
- `onFinish` runs exactly once from `Job.invokeOnCompletion`, with
  `COMPLETED` (duration elapsed), `AUTO_CANCELLED` (player left), `CANCELLED` (the job was
  cancelled) or `FAILED` (anything else threw). A throwing `onFinish` is logged, not propagated.
- `fadeOut = false` (default) sends `Component.empty()` once the loop ends normally, so the bar
  disappears at once. `fadeOut = true` leaves the last component to fade on the client. Either way
  the clear is behind an `ensureActive()`, so a **cancelled** job never clears the bar - cancel and
  the last frame hangs there for its natural ~3 seconds.

### What it does not guarantee: overlap

There is no registry, no per-audience lock, no deduplication. Two calls for one audience produce two
independent jobs writing to the same single action bar slot every interval, and the player sees
whichever ticked last. When the first one finishes with `fadeOut = false` it clears the slot,
blanking the second job's output until its next tick.

The service hands you a `Job` and that is the whole concurrency story. If a feature can start a
second action bar for the same player, store the job and cancel it first:

```kotlin
private val jobs = ConcurrentHashMap<UUID, Job>()

fun showCountdown(player: Player, scope: CoroutineScope) {
    jobs.remove(player.uniqueId)?.cancel()
    jobs[player.uniqueId] = player.sendActionBar(scope, 10.seconds) {
        info("Teleport in ")
        variableValue(remaining(player))
        info("s")
    }.also { job ->
        job.invokeOnCompletion { jobs.remove(player.uniqueId, job) }
    }
}
```

For a single frame with no repetition, use `Audience.sendActionBar { }` from
`messages/adventure/audience-extension.kt` - no scope, no job, nothing to cancel.

## Paper sidebar

`platform/paper/api/.../sidebar/` is a packet-based sidebar and the main consumer of this layer.
Three entry points, each taking the owning plugin: `sidebar(plugin) { }` (manual viewers, manual
updates), `autoUpdatingSidebar(plugin) { }` (manual viewers, `updateInterval`) and
`globalSidebar(plugin) { }` (every online player).

`SidebarBuilder` takes either a `Component` or a builder block for everything:

| Component form | Builder form |
| --- | --- |
| `title(title)` | `buildTitle { }` |
| `title { viewer -> … }` (suspend, per viewer) | `buildViewerTitle { viewer -> }` |
| `sharedTitle { }` (suspend, once per update) | `buildSharedTitle { }` |
| `line(text, score = null)` | `buildLine(score = null) { }` |
| `line { viewer -> }` | `buildViewerLine { viewer -> }` |
| `lines { viewer -> List<SidebarLine> }` | `buildViewerLines { viewer -> line(...) }` |
| `sharedLine { }`, `sharedLines { }` | `buildSharedLine { }`, `buildSharedLines { }` |
| `gradientLine(text, start, end)` | `buildGradientLine(start, end) { }` |

plus `maxLines` (1..15, extra lines discarded), `emptyLine()`, `separator()` (a white-to-gray
gradient rule) and `animatedLine(frames)`. `SidebarLine(text, score = null)` is the line type;
`score` is the content shown on the right.

**The threading contract**, verbatim from `SurfSidebar`: titles and lines are rendered
asynchronously on a background dispatcher for each viewer on every update, renderers may suspend, and
code that needs the viewer's region thread must switch with
`withContext(plugin.entityDispatcher(viewer))`. Rendering for one viewer never overlaps - extra
update requests while a render is in flight are coalesced into one follow-up - and only changed
content is sent. A failing renderer is logged and that viewer keeps its previous content. The sidebar
closes itself when the plugin disables and drops viewers when they leave.

Building a component off the main thread is fine: components are immutable and the builder is local
to the lambda. Reading `player.location`, inventory contents or any world state is not - that is what
the `withContext` is for.

A player must only ever see one sidebar at a time, including the legacy scoreboard below: they share
the single sidebar display slot.

## The deprecated scoreboard

`platform/paper/api/.../scoreboard/SurfScoreboardBuilder` and friends are `@Deprecated` in favour of
the sidebar, with the mapping spelled out in the deprecation message: `addLine` → `line(text)`,
`addUpdatableLine` → `sharedLine { }`, `addViewerLine` → `line { viewer -> }`,
`addViewerComponent` → `buildViewerLines { viewer -> }`, `addAnimatedLine` → `animatedLine(frames)`,
`addGradientLine` → `gradientLine(text, start, end)`, `addLineSeparator` → `separator()`.

The behavioural difference that matters when porting: the old `addViewerLine` ran on the *viewer's
entity scheduler thread*, so it could read game state directly. The new `line { viewer -> }` runs on
a background dispatcher and must switch threads itself. Code moved across without adding the
`withContext` will read world state off-thread.

Both builders accept `SurfComponentBuilder` blocks (`buildLine`, `buildUpdatableLine`), so the text
side of a port is mechanical.
