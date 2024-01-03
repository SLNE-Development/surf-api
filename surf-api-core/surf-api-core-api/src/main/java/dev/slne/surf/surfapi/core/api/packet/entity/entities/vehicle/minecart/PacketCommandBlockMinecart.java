package dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import net.kyori.adventure.text.Component;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CanBeSpawned
public interface PacketCommandBlockMinecart extends PacketAbstractMinecart<PacketCommandBlockMinecart>, Spawnable {

    int COMMAND_INDEX = 14, LAST_OUTPUT_INDEX = 15;

    @Language("RegExp")
    String COMMAND_PATTERN_STRING = "^(/?\\w+(:\\w+)?(\\s.*)?)?$";

    java.util.regex.Pattern COMMAND_PATTERN = java.util.regex.Pattern.compile(COMMAND_PATTERN_STRING);

    /**
     * Gets the command that this CommandBlock will run when powered.
     * This will never return null.  If the CommandBlock does not have a
     * command, an empty String will be returned instead.
     *
     * @return Command that this CommandBlock will run when activated.
     */
    @CommandPattern
    @NotNull
    String command();

    /**
     * Sets the command that this CommandBlock will run when powered.
     * Setting the command to null is the same as setting it to an empty
     * String.
     *
     * @param command Command that this CommandBlock will run when activated.
     */
    void command(@CommandPattern @Nullable String command);

    /**
     * Gets the last output from this command block.
     *
     * @return the last output
     */
    @NotNull
    Component lastOutput();

    /**
     * Sets the last output from this command block.
     *
     * @param lastOutput the last output
     */
    void lastOutput(@Nullable Component lastOutput);

    @Pattern(COMMAND_PATTERN_STRING)
    @interface CommandPattern {
    }
}
