package dev.slne.surf.surfapi.bukkit.test.command.subcommands.lore;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class PacketLoreApply extends CommandAPICommand {

  public PacketLoreApply(String commandName) {
    super(commandName);

    withArguments(new NamespacedKeyArgument("key")
        .replaceSuggestions(ArgumentSuggestions.strings(
            __ -> PacketLoreCreate.getKeys().stream().map(NamespacedKey::asString)
                .toArray(String[]::new))));

    executesPlayer((player, commandArguments) -> {
      NamespacedKey key = commandArguments.getUnchecked("key");

      assert key != null;

      player.getInventory().getItemInMainHand().editMeta(
          itemMeta -> itemMeta.getPersistentDataContainer()
              .set(key, PersistentDataType.BOOLEAN, true));
      player.sendMessage("Applied packet lore!");
    });
  }
}
