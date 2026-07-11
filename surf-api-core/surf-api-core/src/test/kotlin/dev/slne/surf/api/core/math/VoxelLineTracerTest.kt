package dev.slne.surf.api.core.math

import org.spongepowered.math.vector.Vector3d
import kotlin.test.Test
import kotlin.test.assertEquals

class VoxelLineTracerTest {
    @Test
    fun `fractional endpoints terminate in their containing voxels`() {
        assertEquals(
            listOf(
                Vector3d(0.0, 0.0, 0.0),
                Vector3d(1.0, 0.0, 0.0),
                Vector3d(2.0, 0.0, 0.0),
                Vector3d(3.0, 0.0, 0.0),
            ),
            VoxelLineTracer.trace(
                Vector3d(0.5, 0.2, 0.8),
                Vector3d(3.2, 0.9, 0.1),
            ).toList()
        )
    }

    @Test
    fun `negative fractional coordinates use floor voxel coordinates`() {
        assertEquals(
            listOf(Vector3d(-1.0, -1.0, -1.0)),
            VoxelLineTracer.trace(
                Vector3d(-0.1, -0.1, -0.1),
                Vector3d(-0.9, -0.9, -0.9),
            ).toList()
        )
    }

    @Test
    fun `direct collection tracing matches lazy tracing`() {
        val start = Vector3d(-2.2, 1.0, 4.8)
        val end = Vector3d(5.9, 3.0, -1.1)

        assertEquals(
            VoxelLineTracer.trace(start, end).toList(),
            VoxelLineTracer.traceTo(start, end, ArrayList())
        )
    }
}
