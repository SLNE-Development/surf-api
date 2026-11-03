package dev.slne.surf.nats.types

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import kotlin.test.Test
import kotlin.test.assertEquals

class SoundSerializerTest {

    private val name = Key.key("minecraft", "entity.experience_orb.pickup")
    private val nameHex = "0a280a266d696e6563726166743a656e746974792e657870657269656e63655f6f72622e7069636b7570"

    @Test
    fun `descriptor is the surf Sound message`() {
        assertMessageDescriptor(
            SoundSerializer.descriptor,
            "surf.types.v1.Sound",
            "name" to 1, "source" to 2, "volume" to 3, "pitch" to 4, "seed" to 5,
        )
    }

    @Test
    fun `top-level wire bytes with seed`() {
        val value = Sound.sound().type(name).source(Sound.Source.PLAYER).volume(1f).pitch(0.5f).seed(42).build()
        val hex = nameHex + "1008" + "1d0000803f" + "250000003f" + "282a"

        assertEquals(hex, encodeHex(SoundSerializer, value))
        assertEquals(value, decodeHex(SoundSerializer, hex))
    }

    @Test
    fun `sound without seed omits the field`() {
        val value = Sound.sound(name, Sound.Source.MASTER, 1f, 1f)
        val hex = nameHex + "1001" + "1d0000803f" + "250000803f"

        assertEquals(hex, encodeHex(SoundSerializer, value))
        assertEquals(value, decodeHex(SoundSerializer, hex))
    }

    @Test
    fun `every source round trips`() {
        for (source in Sound.Source.entries) {
            val value = Sound.sound(name, source, 1f, 1f)

            assertEquals(value, decodeHex(SoundSerializer, encodeHex(SoundSerializer, value)))
        }
    }

    @Test
    fun `unspecified and unknown sources become master`() {
        val master = Sound.sound(name, Sound.Source.MASTER, 1f, 1f)

        assertEquals(master, decodeHex(SoundSerializer, nameHex + "1000" + "1d0000803f" + "250000803f"))
        assertEquals(master, decodeHex(SoundSerializer, nameHex + "1063" + "1d0000803f" + "250000803f"))
    }

    @Test
    fun `absent fields are missing`() {
        assertMissingField("surf.types.v1.Sound", "name", "source", "volume", "pitch") {
            decodeHex(SoundSerializer, "")
        }
    }
}
