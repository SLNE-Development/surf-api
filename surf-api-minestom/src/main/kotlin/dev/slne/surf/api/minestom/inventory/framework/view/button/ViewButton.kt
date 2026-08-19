package dev.slne.surf.api.minestom.inventory.framework.view.button

import dev.slne.surf.api.minestom.builder.ItemDsl
import dev.slne.surf.api.minestom.builder.ItemDslMarker
import dev.slne.surf.api.minestom.builder.buildItem
import dev.slne.surf.api.minestom.inventory.framework.view.InventoryFrameworkDSL
import me.devnatan.inventoryframework.context.CloseContext
import me.devnatan.inventoryframework.context.SlotClickContext
import me.devnatan.inventoryframework.context.SlotRenderContext
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

/**
 * One state of a stateful button: the value it represents, how it looks, and what happens when the
 * button switches into it.
 *
 * Built through the `state { }` / `whenOn { }` / `whenOff { }` blocks of [ViewButtonBuilder] and
 * [ViewToggleButtonBuilder].
 *
 * @param T the type identifying the button's states
 * @property value the value this state represents
 */
@InventoryFrameworkDSL
class ViewButtonStateScope<T> internal constructor(val value: T) {
    private var itemFactory: (SlotRenderContext.() -> ItemStack)? = null
    private var enterHandler: (SlotClickContext.(from: T) -> Unit)? = null

    /**
     * Renders this state as [material].
     *
     * The item is rebuilt on every render, so [init] may read from the surrounding context.
     *
     * ```kotlin
     * whenOn {
     *     item(Material.LIME_DYE) { displayName { primary("Sound: an") } }
     * }
     * ```
     *
     * @param material the [Material] shown while the button is in this state
     * @param amount the stack size; defaults to `1`
     * @param init customization block applied to the [ItemDsl] building the item
     */
    fun item(
        material: Material,
        amount: Int = 1,
        init: (@ItemDslMarker ItemDsl).() -> Unit = {}
    ) {
        itemFactory = { buildItem(material, amount, init) }
    }

    /**
     * Renders this state with a fully custom factory that receives the [SlotRenderContext].
     *
     * Use this when the appearance depends on the viewer or on other view state.
     *
     * ```kotlin
     * state(Difficulty.HARD) {
     *     renderItem { viewIcon(ViewIconType.SWORD, ViewIconColor.RED) { amount = player.level } }
     * }
     * ```
     *
     * @param factory builds the [ItemStack] shown while the button is in this state
     */
    fun renderItem(factory: @InventoryFrameworkDSL SlotRenderContext.() -> ItemStack) {
        itemFactory = factory
    }

    /**
     * Runs [action] whenever the button switches **into** this state.
     *
     * The previously shown state is passed as `from`. Not called for the state the button starts
     * out in, only for actual transitions.
     *
     * ```kotlin
     * whenOff {
     *     item(Material.GRAY_DYE)
     *     onEnter { player.playSound(...) }
     * }
     * ```
     *
     * @param action the callback invoked with the [SlotClickContext] of the triggering click
     */
    fun onEnter(action: @InventoryFrameworkDSL SlotClickContext.(from: T) -> Unit) {
        enterHandler = action
    }

    internal fun build(): ViewButtonStateSpec<T> {
        val factory = checkNotNull(itemFactory) {
            "Button state '$value' has no appearance. " +
                    "Declare one with item(...) { } or renderItem { }."
        }

        return ViewButtonStateSpec(value, factory, enterHandler)
    }
}

/** Immutable per-state configuration produced by [ViewButtonStateScope]. */
internal class ViewButtonStateSpec<T>(
    val value: T,
    val itemFactory: SlotRenderContext.() -> ItemStack,
    val enterHandler: (SlotClickContext.(from: T) -> Unit)?,
)

/** Immutable button configuration produced by [ViewButtonBuilder]. */
internal class ViewButtonSpec<T>(
    val states: List<ViewButtonStateSpec<T>>,
    val values: List<T>,
    val changeHandler: (SlotClickContext.(from: T, to: T) -> Unit)?,
    val closeChangedHandler: (CloseContext.(initial: T, current: T) -> Unit)?,
    val reverseOnRightClick: Boolean,
)

/**
 * Builder for a button that cycles through an arbitrary number of states.
 *
 * States are cycled in declaration order: a left click moves one state forward and wraps around at
 * the end, a right click moves one state backward unless [reverseOnRightClick] is disabled.
 *
 * ```kotlin
 * val difficulty = statefulButton(Difficulty.EASY) {
 *     state(Difficulty.EASY)   { item(Material.LIME_DYE)   { displayName { primary("Einfach") } } }
 *     state(Difficulty.NORMAL) { item(Material.YELLOW_DYE) { displayName { primary("Normal") } } }
 *     state(Difficulty.HARD)   { item(Material.RED_DYE)    { displayName { primary("Schwer") } } }
 *
 *     onChange { from, to -> player.sendMessage("$from -> $to") }
 *     onCloseChanged { initial, current -> repository.save(player, current) }
 * }
 * ```
 *
 * @param T the type identifying the button's states
 * @see dev.slne.surf.api.minestom.inventory.framework.view.button.statefulButton
 */
@InventoryFrameworkDSL
class ViewButtonBuilder<T> internal constructor() {
    private val scopes = mutableListOf<ViewButtonStateScope<T>>()
    private var changeHandler: (SlotClickContext.(from: T, to: T) -> Unit)? = null
    private var closeChangedHandler: (CloseContext.(initial: T, current: T) -> Unit)? = null
    private var reverseOnRightClickEnabled = true

    /**
     * Declares the state [value] and configures its appearance and enter callback.
     *
     * Declaration order is the cycle order. Declaring the same [value] twice reconfigures the
     * existing state rather than adding a second one.
     *
     * @param value the value this state represents
     * @param block configuration block applied to the state's [ViewButtonStateScope]
     */
    fun state(value: T, block: ViewButtonStateScope<T>.() -> Unit = {}) {
        scopeOf(value).apply(block)
    }

    internal fun scopeOf(value: T): ViewButtonStateScope<T> =
        scopes.firstOrNull { it.value == value }
            ?: ViewButtonStateScope(value).also { scopes.add(it) }

    /**
     * Runs [action] on every state change, regardless of which state was entered.
     *
     * Runs after the per-state [ViewButtonStateScope.onEnter] callback.
     *
     * @param action the callback invoked with the [SlotClickContext] of the triggering click
     */
    fun onChange(action: @InventoryFrameworkDSL SlotClickContext.(from: T, to: T) -> Unit) {
        changeHandler = action
    }

    /**
     * Runs [action] when the view is closed **and** the button ends up on a different state than
     * the one it started out with.
     *
     * This is the hook for persisting what the player configured. It is not called when the button
     * was never used, nor when it was cycled all the way back to its initial state, and it is
     * skipped entirely if the view's own `onClose` callback cancelled the close.
     *
     * ```kotlin
     * onCloseChanged { initial, current ->
     *     settingsRepository.update(player, current)
     * }
     * ```
     *
     * @param action the callback invoked with the [CloseContext], the initial and the final state
     */
    fun onCloseChanged(action: @InventoryFrameworkDSL CloseContext.(initial: T, current: T) -> Unit) {
        closeChangedHandler = action
    }

    /**
     * Controls whether a right click cycles backwards through the states.
     *
     * Enabled by default. Disable it to make every click move forward.
     *
     * @param enabled `true` to cycle backwards on right click
     */
    fun reverseOnRightClick(enabled: Boolean = true) {
        reverseOnRightClickEnabled = enabled
    }

    internal fun build(): ViewButtonSpec<T> {
        check(scopes.isNotEmpty()) {
            "A stateful button must declare at least one state using state(...) { }"
        }

        val states = scopes.map { it.build() }

        return ViewButtonSpec(
            states = states,
            values = states.map { it.value },
            changeHandler = changeHandler,
            closeChangedHandler = closeChangedHandler,
            reverseOnRightClick = reverseOnRightClickEnabled,
        )
    }
}

/**
 * Builder for an on/off button — a [ViewButtonBuilder] specialised to `Boolean` with a fixed
 * `off -> on -> off` cycle, so the declaration order of [whenOn] and [whenOff] does not matter.
 *
 * ```kotlin
 * val sound = toggleButton(initial = true) {
 *     whenOn  { item(Material.NOTE_BLOCK) { displayName { primary("Sound: an") } } }
 *     whenOff { item(Material.BARRIER)    { displayName { primary("Sound: aus") } } }
 *
 *     onToggle { enabled -> player.sendMessage("Sound ${if (enabled) "an" else "aus"}") }
 *     onCloseChanged { _, current -> repository.saveSound(player, current) }
 * }
 * ```
 *
 * @see dev.slne.surf.api.minestom.inventory.framework.view.button.toggleButton
 */
@InventoryFrameworkDSL
class ViewToggleButtonBuilder internal constructor(
    private val delegate: ViewButtonBuilder<Boolean>
) {
    init {
        // Pin the cycle order to off -> on so it does not depend on which block is declared first.
        delegate.state(false)
        delegate.state(true)
    }

    /**
     * Configures how the button looks and behaves while it is **off**.
     *
     * @param block configuration block applied to the off state's [ViewButtonStateScope]
     */
    fun whenOff(block: ViewButtonStateScope<Boolean>.() -> Unit) {
        delegate.state(false, block)
    }

    /**
     * Configures how the button looks and behaves while it is **on**.
     *
     * @param block configuration block applied to the on state's [ViewButtonStateScope]
     */
    fun whenOn(block: ViewButtonStateScope<Boolean>.() -> Unit) {
        delegate.state(true, block)
    }

    /**
     * Runs [action] whenever the button is toggled, passing the new value.
     *
     * @param action the callback invoked with the [SlotClickContext] of the triggering click
     */
    fun onToggle(action: @InventoryFrameworkDSL SlotClickContext.(enabled: Boolean) -> Unit) {
        delegate.onChange { _, to -> action(this, to) }
    }

    /**
     * Runs [action] when the view is closed and the button ends up on the opposite value of the
     * one it started out with.
     *
     * @param action the callback invoked with the [CloseContext], the initial and the final value
     * @see ViewButtonBuilder.onCloseChanged
     */
    fun onCloseChanged(action: @InventoryFrameworkDSL CloseContext.(initial: Boolean, current: Boolean) -> Unit) {
        delegate.onCloseChanged(action)
    }
}
