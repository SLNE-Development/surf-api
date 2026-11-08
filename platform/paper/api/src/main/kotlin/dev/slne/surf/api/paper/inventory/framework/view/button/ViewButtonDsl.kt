package dev.slne.surf.api.paper.inventory.framework.view.button

import dev.slne.surf.api.core.inventory.framework.internal.ViewButtonSession
import dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfViewContext
import dev.slne.surf.api.paper.inventory.framework.view.state.lazyState
import me.devnatan.inventoryframework.context.Context

/**
 * Declares a button that cycles through an arbitrary number of states, starting at [initial].
 *
 * Must be called inside a `surfView { }` or `paginatedSurfView { }` block; place the returned
 * handle into a slot with [button][me.devnatan.inventoryframework.component.BukkitItemComponentBuilder.button].
 * Every viewer gets their own copy of the button's value.
 *
 * ```kotlin
 * surfView("Einstellungen") {
 *     val difficulty = statefulButton(Difficulty.EASY) {
 *         state(Difficulty.EASY)   { item(Material.LIME_DYE)   { displayName { primary("Einfach") } } }
 *         state(Difficulty.NORMAL) { item(Material.YELLOW_DYE) { displayName { primary("Normal") } } }
 *         state(Difficulty.HARD)   { item(Material.RED_DYE)    { displayName { primary("Schwer") } } }
 *
 *         onChange { from, to -> player.sendMessage("$from -> $to") }
 *         onCloseChanged { _, current -> repository.save(player, current) }
 *     }
 *
 *     onFirstRender { slot(1, 4) { button(difficulty) } }
 * }
 * ```
 *
 * @param T the type identifying the button's states
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param initial the state every viewer starts on; must be one of the declared states
 * @param block configuration block applied to the [ViewButtonBuilder]
 * @return a [ViewButtonHandle] to place into a slot and to read the value from
 * @see computedStatefulButton
 * @see toggleButton
 * @see tripleButton
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> statefulButton(
    initial: T,
    block: ViewButtonBuilder<T>.() -> Unit
): ViewButtonHandle<T> = computedStatefulButton({ initial }, block)

/**
 * Declares a button that cycles through an arbitrary number of states, resolving its starting
 * state per viewer.
 *
 * Use this over [statefulButton] whenever the button reflects something already stored for the
 * player, so that "changed" on close means "changed relative to what the player had".
 *
 * ```kotlin
 * val difficulty = computedStatefulButton({ context -> repository.difficultyOf(context.player) }) {
 *     state(Difficulty.EASY)   { item(Material.LIME_DYE) }
 *     state(Difficulty.NORMAL) { item(Material.YELLOW_DYE) }
 *     state(Difficulty.HARD)   { item(Material.RED_DYE) }
 *
 *     onCloseChanged { _, current -> repository.save(player, current) }
 * }
 * ```
 *
 * @param T the type identifying the button's states
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param initial resolves the state a viewer starts on; called once per view session
 * @param block configuration block applied to the [ViewButtonBuilder]
 * @return a [ViewButtonHandle] to place into a slot and to read the value from
 * @see statefulButton
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> computedStatefulButton(
    initial: (Context) -> T,
    block: ViewButtonBuilder<T>.() -> Unit
): ViewButtonHandle<T> = registerButton(ViewButtonBuilder<T>().apply(block).build(), initial)

/**
 * Declares an on/off button starting at [initial].
 *
 * A specialisation of [statefulButton] for the common two-state case, with a fixed `off -> on`
 * cycle and `whenOn` / `whenOff` blocks instead of `state(...)`.
 *
 * ```kotlin
 * surfView("Einstellungen") {
 *     val sound = toggleButton(initial = true) {
 *         whenOn  { item(Material.NOTE_BLOCK) { displayName { primary("Sound: an") } } }
 *         whenOff { item(Material.BARRIER)    { displayName { primary("Sound: aus") } } }
 *
 *         onToggle { enabled -> player.sendMessage("Sound ${if (enabled) "an" else "aus"}") }
 *         onCloseChanged { _, current -> repository.saveSound(player, current) }
 *     }
 *
 *     onFirstRender { slot(1, 1) { button(sound) } }
 * }
 * ```
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param initial whether the button starts out on; defaults to `false`
 * @param block configuration block applied to the [ViewToggleButtonBuilder]
 * @return a [ViewButtonHandle] to place into a slot and to read the value from
 * @see computedToggleButton
 * @see statefulButton
 */
context(ctx: AbstractSurfViewContext<*>)
fun toggleButton(
    initial: Boolean = false,
    block: ViewToggleButtonBuilder.() -> Unit
): ViewButtonHandle<Boolean> = computedToggleButton({ initial }, block)

/**
 * Declares an on/off button that resolves whether it starts out on per viewer.
 *
 * ```kotlin
 * val sound = computedToggleButton({ context -> settings.soundEnabled(context.player) }) {
 *     whenOn  { item(Material.NOTE_BLOCK) }
 *     whenOff { item(Material.BARRIER) }
 *
 *     onCloseChanged { _, current -> settings.setSoundEnabled(player, current) }
 * }
 * ```
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param initial resolves whether a viewer starts out on; called once per view session
 * @param block configuration block applied to the [ViewToggleButtonBuilder]
 * @return a [ViewButtonHandle] to place into a slot and to read the value from
 * @see toggleButton
 */
context(ctx: AbstractSurfViewContext<*>)
fun computedToggleButton(
    initial: (Context) -> Boolean,
    block: ViewToggleButtonBuilder.() -> Unit
): ViewButtonHandle<Boolean> {
    val delegate = ViewButtonBuilder<Boolean>()
    ViewToggleButtonBuilder(delegate).apply(block)

    return registerButton(delegate.build(), initial)
}

/**
 * Declares a button with exactly three states, starting at [initial].
 *
 * Behaves exactly like [statefulButton] but rejects a configuration that does not declare three
 * states, so a missing or surplus `state(...)` block fails when the view is built instead of
 * silently shipping a two- or four-state button.
 *
 * ```kotlin
 * val visibility = tripleButton(Visibility.ALL) {
 *     state(Visibility.ALL)     { item(Material.LIME_DYE)   { displayName { primary("Alle") } } }
 *     state(Visibility.FRIENDS) { item(Material.YELLOW_DYE) { displayName { primary("Freunde") } } }
 *     state(Visibility.NONE)    { item(Material.RED_DYE)    { displayName { primary("Niemand") } } }
 *
 *     onCloseChanged { _, current -> repository.save(player, current) }
 * }
 * ```
 *
 * @param T the type identifying the button's states
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param initial the state every viewer starts on; must be one of the three declared states
 * @param block configuration block applied to the [ViewButtonBuilder]
 * @return a [ViewButtonHandle] to place into a slot and to read the value from
 * @throws IllegalArgumentException if [block] does not declare exactly three states
 * @see computedTripleButton
 * @see statefulButton
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> tripleButton(
    initial: T,
    block: ViewButtonBuilder<T>.() -> Unit
): ViewButtonHandle<T> = computedTripleButton({ initial }, block)

/**
 * Declares a button with exactly three states that resolves its starting state per viewer.
 *
 * @param T the type identifying the button's states
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param initial resolves the state a viewer starts on; called once per view session
 * @param block configuration block applied to the [ViewButtonBuilder]
 * @return a [ViewButtonHandle] to place into a slot and to read the value from
 * @throws IllegalArgumentException if [block] does not declare exactly three states
 * @see tripleButton
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> computedTripleButton(
    initial: (Context) -> T,
    block: ViewButtonBuilder<T>.() -> Unit
): ViewButtonHandle<T> {
    val spec = ViewButtonBuilder<T>().apply(block).build()

    require(spec.values.size == 3) {
        "A triple button must declare exactly three states, but ${spec.values.size} " +
                "were declared: ${spec.values}"
    }

    return registerButton(spec, initial)
}

/**
 * Allocates the per-session state for [spec] and registers its close hook on the view context.
 *
 * The session is a lazy state, so [initial] is resolved once per view session on first access and
 * the value survives re-renders — which is what lets the close hook compare against it.
 */
context(ctx: AbstractSurfViewContext<*>)
private fun <T> registerButton(
    spec: ViewButtonSpec<T>,
    initial: (Context) -> T
): ViewButtonHandle<T> {
    val sessionState = lazyState { context -> ViewButtonSession(spec.values, initial(context)) }
    val handle = ViewButtonHandle(sessionState, spec)

    ctx.registerButtonCloseHandler { close -> handle.handleClose(close) }

    return handle
}
