package dev.slne.surf.surfapi.velocity.api;

import dev.slne.surf.surfapi.core.api.SurfCoreApiAccess;
import org.jetbrains.annotations.ApiStatus;

/**
 * The SurfVelocityApiAccess class provides static access to the SurfVelocityApi instance.
 *
 * <p>
 * This class extends the SurfCoreApiAccess class, which provides a static access point to the SurfCoreApi instance.
 * It allows setting and retrieving the SurfCoreApi instance.
 * </p>
 *
 * <p>
 * Example usage:
 * {@snippet :
 * SurfVelocityApi api = SurfVelocityApiAccess.getInstance();
 * }
 * </p>
 *
 * @see SurfCoreApiAccess
 */
@ApiStatus.Internal
@ApiStatus.NonExtendable
public class SurfVelocityApiAccess extends SurfCoreApiAccess {

    /**
     * Sets the instance of the SurfVelocityApi.
     * <p>
     * This method sets the instance of the SurfVelocityApi by invoking the setInstance method of the SurfCoreApiAccess class.
     * This allows accessing the SurfVelocityApi instance statically.
     * <p>
     * Example usage:
     * {@snippet :
     * SurfVelocityApiAccess.setInstance(api);
     * }
     *
     * @param instance the SurfVelocityApi instance to set
     */
    @ApiStatus.Internal
    public static void setInstance(SurfVelocityApi instance) {
        SurfCoreApiAccess.setInstance(instance);
    }

    /**
     * Retrieves the instance of SurfVelocityApi.
     *
     * @return the SurfVelocityApi instance
     */
    @ApiStatus.Internal
    public static SurfVelocityApi getInstance() {
        return (SurfVelocityApi) SurfCoreApiAccess.getInstance();
    }
}
