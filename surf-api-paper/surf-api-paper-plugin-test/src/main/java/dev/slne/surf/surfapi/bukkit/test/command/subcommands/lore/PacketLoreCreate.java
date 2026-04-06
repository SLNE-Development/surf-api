package dev.slne.surf.api.paper.test.command.subcommands.lore;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import dev.slne.surf.api.paper.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.api.core.api.messages.Colors;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;

public class PacketLoreCreate extends CommandAPICommand {

    private static final Set<NamespacedKey> KEYS = new HashSet<>();

    public PacketLoreCreate(String commandName) {
        super(commandName);

        withArguments(new NamespacedKeyArgument("key"));

        executes((commandSender, commandArguments) -> {
            NamespacedKey key = commandArguments.getUnchecked("key");

            assert key != null;

            KEYS.add(key);

            SurfBukkitPacketApi.getInstance()
                .registerPacketLoreListener(key, (loreToDisplay, pdc, stack) -> {
                    loreToDisplay.add(Component.text("Hello, world!", Colors.AQUA));
                    loreToDisplay.add(Component.text("This is a test!", Colors.AQUA));
                    loreToDisplay.add(Component.text("This is a test!", Colors.PRIMARY));
                    loreToDisplay.add(Component.text("This is a test!", Colors.SECONDARY));
                });
        });
    }

    public static Set<NamespacedKey> getKeys() {
        return KEYS;
    }
}
