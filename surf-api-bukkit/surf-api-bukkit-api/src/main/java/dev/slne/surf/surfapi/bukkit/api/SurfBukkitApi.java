package dev.slne.surf.surfapi.bukkit.api;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboardBuilder;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the API for SurfBukkit.
 */
@ApiStatus.NonExtendable
public interface SurfBukkitApi extends SurfCoreApi {

    /**
     * Retrieves the specific SurfBukkitPacketApi instance.
     *
     * @return the SurfBukkitPacketApi instance
     */
    @Override
    SurfBukkitPacketApi getPacketApi();

    /**
     * Retrieves the {@link ScoreboardLibrary} instance.
     *
     * @return the {@link ScoreboardLibrary} instance
     */
    ScoreboardLibrary getScoreboardLibrary();

    /**
     * Creates a SurfScoreboardBuilder with the given title.
     *
     * @param title the title of the scoreboard
     * @return a SurfScoreboardBuilder with the given title
     */
    SurfScoreboardBuilder createScoreboard(@NotNull Component title);

    /**
     * Sends a player to a specified server.
     *
     * @param player The player to send.
     * @param server The name of the server to send the player to.
     */
    default void sendPlayerToServer(@NotNull Player player, String server) {
        sendPlayerToServer(player.getUniqueId(), server);
    }

    /**
     * Retrieves the instance of SurfBukkitApi.
     *
     * @return the instance of SurfBukkitApi
     * @throws NullPointerException if the SurfBukkitApi instance has not been initialized yet
     */
    @Contract(pure = true)
    static SurfBukkitApi get() {
        return SurfBukkitApiAccess.getInstance();
    }
}
