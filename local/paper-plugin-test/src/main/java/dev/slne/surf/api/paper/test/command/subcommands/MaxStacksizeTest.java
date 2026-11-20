package dev.slne.surf.api.paper.test.command.subcommands;

import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsItemBridge;
import dev.slne.surf.api.spring.SurfContexts;
import org.bukkit.inventory.ItemStack;

public class MaxStacksizeTest extends CommandAPICommand {

    public MaxStacksizeTest(String commandName) {
        super(commandName);

        withArguments(new ItemStackArgument("item"), new IntegerArgument("maxStackSize", 1, 100));

        executes((sender, args) -> {
            final ItemStack item = args.getUnchecked("item");
            final int maxStackSize = args.getUnchecked("maxStackSize");

            SurfContexts.INSTANCE.getRoot().getBean(SurfPaperNmsItemBridge.class).setDefaultMaxStackSize(item.getType().asItemType(), maxStackSize);

            sender.sendMessage(
                    "Set max stack size of " + item.getType().name() + " to " + maxStackSize);
        });
    }
}
