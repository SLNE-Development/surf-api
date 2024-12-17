package dev.slne.surf.surfapi.velocity.api;

import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import dev.slne.surf.surfapi.velocity.api.packet.SurfVelocityPacketApi;
import java.util.concurrent.ExecutorService;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

/**
 * Represents the API for SurfVelocity.
 */
@ApiStatus.NonExtendable
public interface SurfVelocityApi extends SurfCoreApi {

  @Contract(pure = true)
  static SurfVelocityApi get() {
    return (SurfVelocityApi) SurfCoreApi.getInstance();
  }

  /**
   * Retrieves the specific SurfVelocityPacketApi instance.
   *
   * @return the SurfVelocityPacketApi instance
   */
  @Override
  SurfVelocityPacketApi getPacketApi();

  /**
   * Retrieves the ExecutorService instance used by the SurfVelocityApi.
   *
   * @return the ExecutorService instance
   */
  ExecutorService getExecutorService();
}
