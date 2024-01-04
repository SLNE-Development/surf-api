package dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfBukkitPacketEntityApi;
import dev.slne.surf.surfapi.core.api.messages.Colors;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketEntityApi;
import dev.slne.surf.surfapi.core.api.packet.entity.BillboardConstraints;
import dev.slne.surf.surfapi.core.api.packet.entity.Brightness;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.display.PacketBlockDisplay;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.PacketPlayer;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketCow;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie.PacketZombie;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import net.kyori.adventure.text.Component;
import org.spongepowered.math.vector.Vector3f;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class CreateHardcoded extends CommandAPICommand {

    public CreateHardcoded(String commandName) {
        super(commandName);

        withSubcommands(
                new CreateHardcodedPlayer("player"),
                new CreateHardcodedCow("cow"),
                new CreateHardcodedZombie("zombie"),
                new CreateHardcodedBlockDisplay("blockdisplay"),
                new CreateHardcodedCowRaw("cowraw")
        );
    }

    public static class CreateHardcodedPlayer extends CommandAPICommand {
        public CreateHardcodedPlayer(String commandName) {
            super(commandName);

            executesPlayer((player, commandArguments) -> {
                PacketPlayer packetPlayer = SurfBukkitPacketEntityApi.get().spawnEntity(
                        PacketPlayer.class,
                        UUID.randomUUID(),
                        playerMeta -> {
                            playerMeta.displayName(Component.text("Hardcoded Player", Colors.WARNING));
                            playerMeta.activeHand(HumanoidArm.LEFT);
                            playerMeta.glowingEffect(true);
                            playerMeta.inRiptideAttack(true);
                            playerMeta.onFire(true);
                        });

//                packetPlayer.getEquipment().setHelmet(new ItemStack.Builder().type(ItemTypes.DIAMOND_HELMET).build());
//
//                packetPlayer.setOnInteract((clickedEntity, interactAction, interactionHand, user, player1) -> {
//                    // send all params to player1
//                    player1.sendMessage("clickedEntity: " + clickedEntity);
//                    player1.sendMessage("interactAction: " + interactAction);
//                    player1.sendMessage("interactionHand: " + interactionHand);
//                    player1.sendMessage("user: " + user);
//                    player1.sendMessage("player1: " + player1);
//                });

                packetPlayer.spawn(SpigotConversionUtil.fromBukkitLocation(player.getLocation()));
                packetPlayer.addViewer(player.getUniqueId());

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

//                WrapperEntity entity = EntityLib.createEntity(UUID.randomUUID(), EntityTypes.COW);
//                CowMeta meta = (CowMeta) entity.getMeta();
//
//                meta.setCustomName(Component.text("Hardcoded Cow", Colors.WARNING));
//                meta.setCustomNameVisible(true);
//                meta.setActiveHand(HumanoidArm.LEFT);
//                meta.setHasGlowingEffect(true);
//                meta.setOnFire(true);
//
//                UUID uuid = player.getUniqueId();
//                entity.addViewer(uuid);
//                entity.spawn(SpigotConversionUtil.fromBukkitLocation(player.getLocation()));
//
//                EntityLib.sendPacket(uuid, entity.getMeta().createPacket());

                player.sendMessage("Created hardcoded cow");
            });
        }
    }

    public static class CreateHardcodedZombie extends CommandAPICommand {
        public CreateHardcodedZombie(String commandName) {
            super(commandName);

            executesPlayer((player, commandArguments) -> {
                PacketZombie<?> packetZombie = SurfBukkitPacketEntityApi.get().spawnEntity(
                        PacketZombie.class,
                        UUID.randomUUID(),
                        zombieMeta -> {
                            zombieMeta.displayName(Component.text("Hardcoded Zombie", Colors.WARNING));
                            zombieMeta.activeHand(HumanoidArm.LEFT);
                            zombieMeta.glowingEffect(true);
                            zombieMeta.becomingDrowned(true);
                        });

//                packetZombie.getEquipment().setHelmet(new ItemStack.Builder().type(ItemTypes.DIAMOND_HELMET).build());
//
//                packetZombie.setOnInteract((clickedEntity, interactAction, interactionHand, user, player1) -> {
//                    // send all params to player1
//                    player1.sendMessage("clickedEntity: " + clickedEntity);
//                    player1.sendMessage("interactAction: " + interactAction);
//                    player1.sendMessage("interactionHand: " + interactionHand);
//                    player1.sendMessage("user: " + user);
//                    player1.sendMessage("player1: " + player1);
//                });

//                packetZombie.spawn(player.getLocation());
//                packetZombie.addViewer(player);
                boolean success2 = packetZombie.addViewer(player.getUniqueId());
                boolean success = packetZombie.spawn(SpigotConversionUtil.fromBukkitLocation(player.getLocation()));

                player.sendMessage("Created hardcoded zombie: " + success + " " + success2);
            });
        }
    }

    public static class CreateHardcodedBlockDisplay extends CommandAPICommand {

        public CreateHardcodedBlockDisplay(String commandName) {
            super(commandName);

            executesPlayer((player, commandArguments) -> {
                PacketBlockDisplay packetBlockDisplay = SurfCorePacketEntityApi.get().spawnEntity(PacketBlockDisplay.class, UUID.randomUUID(), blockDisplay -> {
                    blockDisplay.blockState(StateTypes.STONE);
                    blockDisplay.brightness(new Brightness(1, 1));
                    blockDisplay.billboardConstraints(BillboardConstraints.CENTER);
                    blockDisplay.scale(Vector3f.createRandomDirection(new Random()));
                    blockDisplay.glowingEffect(true);
                    blockDisplay.glowColorOverride(Color.ORANGE);
                });

                packetBlockDisplay.addViewer(player.getUniqueId());
                packetBlockDisplay.spawn(SpigotConversionUtil.fromBukkitLocation(player.getLocation()));
            });
        }
    }

    public static class CreateHardcodedCowRaw extends CommandAPICommand {

        public CreateHardcodedCowRaw(String commandName) {
            super(commandName);

            executesPlayer((player, commandArguments) -> {
                List<EntityData> entityData = List.of(new EntityData(PacketCow.IS_BABY_INDEX, EntityDataTypes.BOOLEAN, true));

                int entityID = SpigotReflectionUtil.generateEntityId();
                WrapperPlayServerSpawnLivingEntity spawnPacket = new WrapperPlayServerSpawnLivingEntity( // removed in 1.18.2
                        entityID,
                        UUID.randomUUID(),
                        EntityTypes.COW,
                        SpigotConversionUtil.fromBukkitLocation(player.getLocation()),
                        1,
                        Vector3d.zero(),
                        entityData
                );

                WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(
                        entityID,
                        Optional.of(UUID.randomUUID()),
                        EntityTypes.COW,
                        SpigotConversionUtil.fromBukkitLocation(player.getLocation()).getPosition(),
                        player.getPitch(),
                        player.getYaw(),
                        1,
                        1,
                        Optional.empty()
                );

                WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(
                        entityID,
                        entityData
                );

                System.err.println(spawnPacket.getPacketId());
                System.err.println(packet.getPacketId());

//                PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);

                player.sendMessage("Created hardcoded cow");
            });
        }
    }
}
