package dev.slne.surf.api.minestom

import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

@EnvTest
class MinestomEnvSmokeTest {

    @Test
    fun `environment boots and creates a flat instance`(env: Env) {
        val instance = env.createFlatInstance()
        assertNotNull(instance)
        env.destroyInstance(instance)
    }
}
