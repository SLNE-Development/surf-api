package dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfBukkitPacketEntityApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfLivingEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.meta.EntityType;
import dev.slne.surf.surfapi.core.api.messages.Colors;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.entity.WrapperEntity;
import me.tofaa.entitylib.meta.mobs.monster.zombie.ZombieMeta;
import me.tofaa.entitylib.meta.mobs.passive.CowMeta;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class CreateHardcoded extends CommandAPICommand {

    public CreateHardcoded(String commandName) {
        super(commandName);

        withSubcommands(
                new CreateHardcodedPlayer("player"),
                new CreateHardcodedCow("cow"),
                new CreateHardcodedZombie("zombie")
        );
    }

    public static class CreateHardcodedPlayer extends CommandAPICommand {
        public CreateHardcodedPlayer(String commandName) {
            super(commandName);

            executesPlayer((player, commandArguments) -> {
                SurfLivingEntity<PlayerMeta> packetPlayer = SurfBukkitPacketEntityApi.get().createEntity(
                        UUID.randomUUID(),
                        EntityType.PLAYER,
                        playerMeta -> {
                            playerMeta.setCustomName(Component.text("Hardcoded Player", Colors.WARNING));
                            playerMeta.setCustomNameVisible(true);
                            playerMeta.setActiveHand(HumanoidArm.LEFT);
                            playerMeta.setHasGlowingEffect(true);
                            playerMeta.setInRiptideSpinAttack(true);
                            playerMeta.setOnFire(true);
                        });

                packetPlayer.getEquipment().setHelmet(new ItemStack.Builder().type(ItemTypes.DIAMOND_HELMET).build());

                packetPlayer.setOnInteract((clickedEntity, interactAction, interactionHand, user, player1) -> {
                    // send all params to player1
                    player1.sendMessage("clickedEntity: " + clickedEntity);
                    player1.sendMessage("interactAction: " + interactAction);
                    player1.sendMessage("interactionHand: " + interactionHand);
                    player1.sendMessage("user: " + user);
                    player1.sendMessage("player1: " + player1);
                });

                packetPlayer.spawn(player.getLocation());
                packetPlayer.addViewer(player);

                player.sendMessage("Created hardcoded player");
            });
        }
    }

    public static class CreateHardcodedCow extends CommandAPICommand {
        public CreateHardcodedCow(String commandName) {
            super(commandName);

            executesPlayer((player, commandArguments) -> {
//                try {
//                    SurfLivingEntity<CowMeta> packetCow = SurfBukkitPacketEntityApi.get().createEntity(
//                            UUID.randomUUID(),
//                            EntityType.COW,
//                            cowMeta -> {
//                                cowMeta.setCustomName(Component.text("Hardcoded Cow", Colors.WARNING));
//                                cowMeta.setCustomNameVisible(true);
//                                cowMeta.setActiveHand(HumanoidArm.LEFT);
//                                cowMeta.setHasGlowingEffect(true);
//                                cowMeta.setOnFire(true);
//                                cowMeta.setBaby(true);
//                            });
//
//                    packetCow.getEquipment().setHelmet(new ItemStack.Builder().type(ItemTypes.DIAMOND_HELMET).build());
//
//                    packetCow.setOnInteract((clickedEntity, interactAction, interactionHand, user, player1) -> {
//                        // send all params to player1
//                        player1.sendMessage("clickedEntity: " + clickedEntity);
//                        player1.sendMessage("interactAction: " + interactAction);
//                        player1.sendMessage("interactionHand: " + interactionHand);
//                        player1.sendMessage("user: " + user);
//                        player1.sendMessage("player1: " + player1);
//                    });
//
//                    packetCow.spawn(player.getLocation());
//                    packetCow.addViewer(player);
//
//                    player.sendMessage("Created hardcoded cow");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

                WrapperEntity entity = EntityLib.createEntity(UUID.randomUUID(), EntityTypes.COW);
                CowMeta meta = (CowMeta) entity.getMeta();

                meta.setCustomName(Component.text("Hardcoded Cow", Colors.WARNING));
                meta.setCustomNameVisible(true);
                meta.setActiveHand(HumanoidArm.LEFT);
                meta.setHasGlowingEffect(true);
                meta.setOnFire(true);

                UUID uuid = player.getUniqueId();
                entity.addViewer(uuid);
                entity.spawn(SpigotConversionUtil.fromBukkitLocation(player.getLocation()));

                EntityLib.sendPacket(uuid, entity.getMeta().createPacket());

                player.sendMessage("Created hardcoded cow");
            });
        }
    }

    public static class CreateHardcodedZombie extends CommandAPICommand {
        public CreateHardcodedZombie(String commandName) {
            super(commandName);

            executesPlayer((player, commandArguments) -> {
                SurfLivingEntity<ZombieMeta> packetZombie = SurfBukkitPacketEntityApi.get().createEntity(
                        UUID.randomUUID(),
                        EntityType.ZOMBIE,
                        zombieMeta -> {
                            zombieMeta.setCustomName(Component.text("Hardcoded Zombie", Colors.WARNING));
                            zombieMeta.setCustomNameVisible(true);
                            zombieMeta.setActiveHand(HumanoidArm.LEFT);
                            zombieMeta.setHasGlowingEffect(true);
                            zombieMeta.setBecomingDrowned(true);
                        });

                packetZombie.getEquipment().setHelmet(new ItemStack.Builder().type(ItemTypes.DIAMOND_HELMET).build());

                packetZombie.setOnInteract((clickedEntity, interactAction, interactionHand, user, player1) -> {
                    // send all params to player1
                    player1.sendMessage("clickedEntity: " + clickedEntity);
                    player1.sendMessage("interactAction: " + interactAction);
                    player1.sendMessage("interactionHand: " + interactionHand);
                    player1.sendMessage("user: " + user);
                    player1.sendMessage("player1: " + player1);
                });

                packetZombie.spawn(player.getLocation());
                packetZombie.addViewer(player);

                player.sendMessage("Created hardcoded zombie");
            });
        }
    }
}
