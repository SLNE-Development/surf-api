package dev.slne.surf.surfapi.bukkit.api.gui.props

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

/**
 * Base interface for all props in the GUI framework.
 * Props can be scoped as global (shared across all viewers) or viewer-specific (isolated per viewer).
 */
sealed interface Prop<T> {
    /**
     * Gets the value of this prop for the given context.
     */
    fun get(context: PropContext): T
    
    /**
     * The name of this prop.
     */
    val name: String
}

/**
 * Context for prop evaluation, contains information about the current viewer.
 */
data class PropContext(
    val viewerId: UUID,
    val viewer: Player?
)

/**
 * Defines the scope of a prop.
 */
enum class PropScope {
    /** Shared across all viewers */
    GLOBAL,
    /** Isolated per viewer */
    VIEWER
}

/**
 * Immutable prop - always available and immutable after initialization.
 */
class ImmutableProp<T>(
    override val name: String,
    private val value: T,
    private val scope: PropScope = PropScope.VIEWER
) : Prop<T> {
    override fun get(context: PropContext): T = value
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}

/**
 * Mutable prop - always available and mutable.
 */
class MutableProp<T>(
    override val name: String,
    initialValue: T,
    private val scope: PropScope = PropScope.VIEWER
) : Prop<T> {
    private val globalValue = AtomicReference(initialValue)
    private val viewerValues = mutableMapOf<UUID, T>()
    
    override fun get(context: PropContext): T {
        return when (scope) {
            PropScope.GLOBAL -> globalValue.get()
            PropScope.VIEWER -> viewerValues.getOrPut(context.viewerId) { globalValue.get() }
        }
    }
    
    fun set(context: PropContext, value: T) {
        when (scope) {
            PropScope.GLOBAL -> globalValue.set(value)
            PropScope.VIEWER -> viewerValues[context.viewerId] = value
        }
    }
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = globalValue.get()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        globalValue.set(value)
    }
}

/**
 * Computed prop - accepts a callback that computes the value.
 */
class ComputedProp<T>(
    override val name: String,
    private val compute: (PropContext) -> T,
    private val scope: PropScope = PropScope.VIEWER
) : Prop<T> {
    override fun get(context: PropContext): T = compute(context)
}

/**
 * Lazy prop - gets available when accessed, using a callback.
 */
class LazyProp<T>(
    override val name: String,
    private val initializer: (PropContext) -> T,
    private val mutable: Boolean = false,
    private val scope: PropScope = PropScope.VIEWER
) : Prop<T> {
    private val globalValue = lazy { initializer(PropContext(UUID.randomUUID(), null)) }
    private val viewerValues = mutableMapOf<UUID, T>()
    private val initialized = mutableSetOf<UUID>()
    
    override fun get(context: PropContext): T {
        return when (scope) {
            PropScope.GLOBAL -> {
                if (mutable && context.viewerId in initialized) {
                    viewerValues.getOrPut(context.viewerId) { globalValue.value }
                } else {
                    globalValue.value
                }
            }
            PropScope.VIEWER -> {
                viewerValues.getOrPut(context.viewerId) {
                    initialized.add(context.viewerId)
                    initializer(context)
                }
            }
        }
    }
    
    fun set(context: PropContext, value: T) {
        if (!mutable) {
            throw UnsupportedOperationException("Cannot set immutable lazy prop")
        }
        viewerValues[context.viewerId] = value
    }
}
