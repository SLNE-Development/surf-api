package dev.slne.surf.surfapi.bukkit.test.command.subcommands.gui;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class InventoryFrameworkTest extends CommandAPICommand {

  public InventoryFrameworkTest(String commandName) {
    super(commandName);

    executesPlayer((player, commandArguments) -> {
      final ChestGui testGui = new ChestGui(1, "Test");
      testGui.setOnGlobalClick(event -> event.setCancelled(true));

      final StaticPane pane = new StaticPane(0, 0, 9, 1);
      pane.fillWith(new ItemStack(Material.DIAMOND), event -> {
        event.setCancelled(true);
        event.getWhoClicked().sendMessage("You clicked on a diamond!");
      });

      testGui.addPane(pane);

      testGui.show(player);
    });
  }
}
