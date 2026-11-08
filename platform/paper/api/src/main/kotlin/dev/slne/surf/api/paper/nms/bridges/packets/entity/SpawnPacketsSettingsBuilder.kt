package dev.slne.surf.api.paper.nms.bridges.packets.entity

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform
import org.bukkit.entity.TextDisplay.TextAlignment
import org.bukkit.inventory.ItemStack
import org.spongepowered.math.imaginary.Quaternionf
import org.spongepowered.math.vector.Vector3f

@DslMarker
annotation class SettingsDsl

@SettingsDsl
sealed class DisplaySettings : Cloneable {
    var pitch: Float = 0f
    var yaw: Float = 0f
    var translation: Vector3f? = null
    var scale: Vector3f? = null
    var leftRotation: Quaternionf? = null
    var rightRotation: Quaternionf? = null
    var billboardConstraints: Billboard = Billboard.FIXED

    abstract fun build(): DisplaySettings
    public override fun clone() = super.clone() as DisplaySettings

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <T : DisplaySettings> buildSettings(builder: T, block: T.() -> Unit): T {
            builder.apply(block)
            return builder.build() as T
        }
    }
}

@SettingsDsl
class ItemDisplaySettings : DisplaySettings() {
    var itemStack: ItemStack = ItemStack.of(Material.AIR)
    var itemDisplayTransform: ItemDisplayTransform = ItemDisplayTransform.NONE

    override fun build(): ItemDisplaySettings = this

    companion object {
        fun create(block: ItemDisplaySettings.() -> Unit) =
            buildSettings(ItemDisplaySettings(), block)
    }
}

@SettingsDsl
class TextDisplaySettings : DisplaySettings() {
    var text: Component = Component.empty()
    var lineWidth: Int = 200
    var backgroundColor: TextColor = TextColor.color(0x40000000)
    var textAlignment: TextAlignment = TextAlignment.CENTER

    override fun build(): TextDisplaySettings = this

    companion object {
        fun create(block: TextDisplaySettings.() -> Unit) =
            buildSettings(TextDisplaySettings(), block)
    }
}

@SettingsDsl
class BlockDisplaySettings : DisplaySettings() {
    var blockData: BlockData = Material.AIR.createBlockData()

    override fun build(): BlockDisplaySettings = this
    override fun clone() = super.clone() as BlockDisplaySettings

    companion object {
        fun create(block: BlockDisplaySettings.() -> Unit) =
            buildSettings(BlockDisplaySettings(), block)

        operator fun invoke(block: BlockDisplaySettings.() -> Unit): BlockDisplaySettings {
            return create(block)
        }
    }
}

@SettingsDsl
class SignBlockUpdateSettings {
    var frontText: SignText = SignText.empty()
    var backText: SignText = SignText.empty()

    fun setText(text: SignText, front: Boolean) {
        if (front) frontText = text else backText = text
    }

    override fun toString(): String =
        "SignBlockUpdateSettings(frontText=$frontText, backText=$backText)"

    @SettingsDsl
    class SignText {
        var line1: Component = Component.empty()
        var line2: Component = Component.empty()
        var line3: Component = Component.empty()
        var line4: Component = Component.empty()

        override fun toString(): String =
            "SignText(line1=$line1, line2=$line2, line3=$line3, line4=$line4)"

        companion object {
            fun empty(): SignText = SignText()

            fun create(block: SignText.() -> Unit): SignText {
                return SignText().apply(block)
            }
        }
    }

    companion object {
        fun create(block: SignBlockUpdateSettings.() -> Unit): SignBlockUpdateSettings {
            return SignBlockUpdateSettings().apply(block)
        }
    }
}