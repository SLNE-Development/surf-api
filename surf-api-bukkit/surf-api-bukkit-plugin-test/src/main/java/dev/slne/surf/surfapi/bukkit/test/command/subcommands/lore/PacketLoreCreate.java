package dev.slne.surf.surfapi.bukkit.test.command.subcommands.lore;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApiAccess;
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.core.api.messages.Colors;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;

import java.util.HashSet;
import java.util.Set;

public class PacketLoreCreate extends CommandAPICommand {
    private static final Set<NamespacedKey> KEYS = new HashSet<>();

    public PacketLoreCreate(String commandName) {
        super(commandName);

        withArguments(new NamespacedKeyArgument("key"));

        executes((commandSender, commandArguments) -> {
            NamespacedKey key = commandArguments.getUnchecked("key");

            assert key != null;

            KEYS.add(key);


            SurfBukkitPacketApi.get().registerPacketLoreListener(key, loreToDisplay -> {
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
