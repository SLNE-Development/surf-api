# SURF-API GUI Framework

A modern, React-inspired GUI framework for Minecraft servers, providing component-based architecture with reactive props and lifecycle management.

## Overview

The SURF-API GUI framework offers:

- **Component-Based Architecture**: Build GUIs using reusable, composable components
- **Reactive Props**: Manage state with mutable, immutable, computed, and lazy props
- **React-Like Lifecycle**: Components support `onMount`, `onUnmount`, `onUpdate` hooks
- **Refs System**: Reference and update components programmatically
- **View Navigation**: Navigate between parent/child views with state preservation
- **Update Intervals**: Automatic component updates at configurable intervals

## Core Concepts

### Props

Props are the state management system for GUIs. They come in several flavors:

#### Prop Types

- **ImmutableProp**: Static values that never change
- **MutableProp**: Dynamic values that can be updated
- **ComputedProp**: Values computed from a callback function
- **LazyProp**: Values loaded on first access
- **PaginationProp**: Built-in pagination support for lists

#### Prop Scopes

- **GLOBAL**: Shared across all viewers
- **VIEWER**: Isolated per viewer

```kotlin
val props = props {
    immutable("title", "My GUI", PropScope.GLOBAL)
    mutable("clickCount", 0, PropScope.VIEWER)
    computed("displayText", PropScope.VIEWER) { context ->
        "Clicks: ${mutableProp.get(context)}"
    }
    lazy("expensiveData", scope = PropScope.VIEWER) { context ->
        loadExpensiveData()
    }
    pagination("items", pageSize = 9) {
        listOf(Material.DIAMOND, Material.GOLD_INGOT, ...)
    }
}
```

### Components

Components are the building blocks of GUIs. They render ItemStacks and handle user interactions.

```kotlin
val component = dynamicComponent(
    renderer = { context ->
        buildItem(ItemType.DIAMOND) {
            displayName { text("Click me!") }
        }
    }
) {
    onClick = {
        player.sendMessage("You clicked!")
        update() // Re-render this component
    }
    
    onMount = {
        // Called when component is added to view
    }
    
    onUpdate = {
        // Called when component updates
    }
    
    updateInterval = 5.seconds // Auto-update every 5 seconds
}
```

### Views

Views are the containers that manage components and handle GUI lifecycle.

```kotlin
object MyGuiView : BukkitGuiView() {
    override fun onInit(config: ViewConfig) {
        config.title = text("My GUI")
        config.size = 54 // 6 rows
        config.cancelOnClick = true
    }
    
    override fun onFirstRender(context: RenderContext) {
        // Place components in slots
        context.slot(0, myComponent)
        context.slot(1, anotherComponent)
    }
    
    override fun onOpen(context: ViewContext) {
        // Called when GUI opens for a player
    }
    
    override fun onClose(context: ViewContext) {
        // Called when GUI closes
    }
    
    override fun onUpdate(context: ViewContext) {
        // Called when view updates
    }
    
    override fun onResume(context: ResumeContext) {
        // Called when returning from child view
        val origin = context.origin // The view we came from
    }
}
```

### Refs

Refs allow components to reference and update other components, enabling component communication.

```kotlin
val counterRef = createRef<Component>()
val clickCountProp = MutableProp("count", 0, PropScope.VIEWER)

// Counter display component
val counterDisplay = dynamicComponent(
    renderer = { context ->
        val count = clickCountProp.get(context.propContext)
        buildItem(ItemType.GOLD_BLOCK) {
            displayName { text("Count: $count") }
        }
    }
) {
    ref = counterRef // Attach ref
}

// Button that updates the counter
val incrementButton = component(
    item = buildItem(ItemType.EMERALD) {
        displayName { text("Increment") }
    }
) {
    onClick = {
        val current = clickCountProp.get(propContext)
        clickCountProp.set(propContext, current + 1)
        counterRef.update() // Update the counter display
    }
}
```

### Context

All user actions and lifecycle events receive a context object with:

- **player**: The player interacting with the GUI
- **view**: The current view
- **propContext**: For accessing props
- **Navigation methods**: `navigateTo()`, `navigateBack()`, `close()`
- **update()**: Re-render the view

```kotlin
onClick = { context ->
    val count = context.getProp(myProp)
    context.player.sendMessage("Count: $count")
    context.navigateTo(childView)
    context.update()
}
```

## Navigation

Views can have parent-child relationships for navigation:

```kotlin
object ParentView : BukkitGuiView() {
    override fun onFirstRender(context: RenderContext) {
        context.slot(0, component(
            item = buildItem(ItemType.COMPASS) {
                displayName { text("Go to Child") }
            }
        ) {
            onClick = {
                navigateTo(ChildView, passProps = true)
            }
        })
    }
}

object ChildView : BukkitGuiView() {
    override fun onFirstRender(context: RenderContext) {
        context.slot(0, component(
            item = buildItem(ItemType.ARROW) {
                displayName { text("Back") }
            }
        ) {
            onClick = {
                navigateBack() // Returns to ParentView
            }
        })
    }
}
```

## Lifecycle

Components and views follow a React-like lifecycle:

### Component Lifecycle
1. **onMount**: Component added to view
2. **render**: Component rendered to ItemStack
3. **onUpdate**: Component updated (manual or interval)
4. **onClick**: User clicks component
5. **onUnmount**: Component removed from view

### View Lifecycle
1. **onInit**: View configuration initialized (once)
2. **onOpen**: View opened for player
3. **onFirstRender**: First render for player
4. **onUpdate**: View updated
5. **onResume**: Returning from child view
6. **onClose**: View closed

## Complete Example

```kotlin
object ShopView : BukkitGuiView() {
    private val propsMap = props {
        mutable("coins", 100, PropScope.VIEWER)
        pagination("items", pageSize = 9) {
            listOf(Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT)
        }
    }
    
    private val coinsProp = propsMap["coins"] as MutableProp<Int>
    private val itemsProp = propsMap["items"] as PaginationProp<Material>
    private val coinsRef = createRef<Component>()
    
    override fun onInit(config: ViewConfig) {
        config.title = text("Shop", NamedTextColor.GOLD)
        config.size = 54
    }
    
    override fun onFirstRender(context: RenderContext) {
        // Coins display
        val coinsDisplay = dynamicComponent(
            renderer = { ctx ->
                val coins = coinsProp.get(ctx.propContext)
                buildItem(ItemType.GOLD_INGOT) {
                    displayName { text("Coins: $coins", NamedTextColor.YELLOW) }
                }
            }
        ) {
            ref = coinsRef
        }
        context.slot(4, coinsDisplay)
        
        // Items
        val pagination = itemsProp.get(context.propContext)
        pagination.items.forEachIndexed { index, material ->
            context.slot(9 + index, component(
                item = ItemStack.of(material)
            ) {
                onClick = {
                    val coins = coinsProp.get(propContext)
                    if (coins >= 10) {
                        coinsProp.set(propContext, coins - 10)
                        player.inventory.addItem(ItemStack.of(material))
                        coinsRef.update()
                    }
                }
            })
        }
        
        // Navigation buttons
        if (pagination.hasPreviousPage) {
            context.slot(45, component(
                item = buildItem(ItemType.ARROW) {
                    displayName { text("Previous") }
                }
            ) {
                onClick = {
                    itemsProp.previousPage(propContext)
                    update()
                }
            })
        }
        
        if (pagination.hasNextPage) {
            context.slot(53, component(
                item = buildItem(ItemType.ARROW) {
                    displayName { text("Next") }
                }
            ) {
                onClick = {
                    itemsProp.nextPage(propContext)
                    update()
                }
            })
        }
    }
}

// Open the view
fun openShop(player: Player) {
    ShopView.open(player)
}
```

## Opening Views

```kotlin
// Open a view for a player
MyGuiView.open(player)

// In a command
class OpenGuiCommand : CommandAPICommand("opengui") {
    init {
        playerExecutor { player, _ ->
            MyGuiView.open(player)
        }
    }
}
```

## Best Practices

1. **Use Props for State**: Don't store state in variables; use props for proper viewer isolation
2. **Refs for Communication**: Use refs when components need to update each other
3. **Computed Props for Derived State**: Use computed props instead of storing derived values
4. **Lifecycle Hooks**: Use lifecycle hooks for initialization and cleanup
5. **Update Intervals**: Use sparingly; prefer manual updates via refs
6. **Navigation**: Use parent-child relationships for related views

## Migration from Old Framework

The new framework replaces the old inventory-framework-based system. Key differences:

### Old Way (inventory-framework)
```kotlin
object OldView : View() {
    override fun onInit(config: ViewConfigBuilder) {
        config.title("My GUI")
    }
    
    override fun onFirstRender(render: RenderContext) {
        render.slot(0, item).onClick { /* ... */ }
    }
}
```

### New Way (SURF-API GUI)
```kotlin
object NewView : BukkitGuiView() {
    override fun onInit(config: ViewConfig) {
        config.title = text("My GUI")
    }
    
    override fun onFirstRender(context: RenderContext) {
        context.slot(0, component(item) {
            onClick = { /* ... */ }
        })
    }
}
```

The new framework provides more control, better type safety, and React-like patterns familiar to modern developers.
