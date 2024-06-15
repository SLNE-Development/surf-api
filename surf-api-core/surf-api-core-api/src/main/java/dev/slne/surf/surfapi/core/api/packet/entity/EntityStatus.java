package dev.slne.surf.surfapi.core.api.packet.entity;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.arrow.PacketArrow;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketEvokerFangs;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketFireworkRocket;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketFishingHook;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.PacketArmorStand;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.PacketLivingEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.PacketPlayer;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.PacketMob;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketAnimal;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketFox;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketGoat;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketHoglin;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketOcelot;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketRabbit;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketSheep;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketSniffer;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketAbstractHorse;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable.PacketTameableAnimal;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable.PacketWolf;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.villager.PacketVillager;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.golem.PacketIronGolem;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketGuardian;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketWarden;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketZoglin;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.PacketRavager;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.PacketWitch;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie.PacketZombieVillager;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.PacketSquid;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile.PacketThrowEgg;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile.PacketThrowSnowball;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.PacketSpawnerMinecart;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.PacketTntMinecart;

public enum EntityStatus {

  /**
   * Spawns honey block slide particles at the entities feet
   */
  HONEY_PARTICLE(53, PacketEntity.class),

  /**
   * Displays 8 snowballpoof particles at the snowball's location
   */
  SNOWBALL_POOF(3, PacketThrowSnowball.class),

  /**
   * Displays 8 iconcrack particles with the egg as a parameter at the egg's location
   */
  EGG_CRACK(3, PacketThrowEgg.class),

  /**
   * If the caught entity is the connected player, then cause them to be pulled toward the caster of
   * the fishing rod.
   */
  FISHING_HOOK_PULL_PLAYER(31, PacketFishingHook.class),

  /**
   * Spawns tipped arrow particle effects, if the color is not -1.
   */
  ARROW_PARTICLE(0, PacketArrow.class),

  /**
   * Triggers the firework explosion effect (based on the firework info metadata)
   */
  FIREWORK_EXPLODE(17, PacketFireworkRocket.class),

  /**
   * Plays the death sound and death animation
   */
  DEATH(3, PacketLivingEntity.class),

  /**
   * Plays the shield block sound
   */
  SHIELD_BLOCK(29, PacketLivingEntity.class),

  /**
   * Plays the shield break sound
   */
  SHIELD_BREAK(30, PacketLivingEntity.class),

  /**
   * Plays totem of undying animation and sound
   */
  TOTEM_OF_UNDYING(35, PacketLivingEntity.class),

  /**
   * Spawns portal particles when teleporting due to consumption of a chorus fruit or being an
   * endermen
   */
  PORTAL_PARTICLES(46, PacketLivingEntity.class),

  /**
   * Plays the equipment break sound (unless silent) and spawns break particles for the item in the
   * main hand
   */
  EQUIPMENT_BREAK_MAIN_HAND(47, PacketLivingEntity.class),

  /**
   * Plays the equipment break sound (unless silent) and spawns break particles for the item in the
   * off hand
   */
  EQUIPMENT_BREAK_OFF_HAND(48, PacketLivingEntity.class),

  /**
   * Plays the equipment break sound (unless silent) and spawns break particles for the item in the
   * head slot
   */
  EQUIPMENT_BREAK_HEAD(49, PacketLivingEntity.class),

  /**
   * Plays the equipment break sound (unless silent) and spawns break particles for the item in the
   * chest slot
   */
  EQUIPMENT_BREAK_CHEST(50, PacketLivingEntity.class),

  /**
   * Plays the equipment break sound (unless silent) and spawns break particles for the item in the
   * legs slot
   */
  EQUIPMENT_BREAK_LEGS(51, PacketLivingEntity.class),

  /**
   * Plays the equipment break sound (unless silent) and spawns break particles for the item in the
   * feet slot
   */
  EQUIPMENT_BREAK_FEET(52, PacketLivingEntity.class),

  /**
   * Spawns honey block fall particles at the entities feet
   */
  HONEY_BLOCK_FALL(54, PacketLivingEntity.class),

  /**
   * Swaps the hand items of the entity
   */
  SWAP_HAND_ITEMS(55, PacketLivingEntity.class),

  /**
   * Spawns death smoke particles
   */
  DEATH_SMOKE(56, PacketLivingEntity.class),

  /**
   * Marks item use as finished (finished eating, finished drinking, etc.).
   * <p>
   * This status is not required if you want more control on the server side, this basically
   * finishes the interaction on the client side to decrease the food quantity, arrow quantity, ...
   * <p>
   * But you can trigger these changes manually through other packets or prevent those changes on
   * the client.
   * <p>
   * Examples:
   * <ul>
   * <li>Create an 'Infinity Bow' without the first arrow in your inventory constantly changing in quantity.</li>
   * <li>Create a custom found that is infinite, and prevent the stack quantity from decreasing.</li>
   * <li>...</li>
   * </ul>
   * <b>Note:</b> This works together with the 'Hand data' so this will have to be updated accordingly to 'finish' the interaction manually.
   */
  FINISH_USE_ITEM(9, PacketPlayer.class),

  /**
   * Enables reduced debug screen information
   */
  ENABLE_REDUCED_DEBUG_SCREEN(22, PacketPlayer.class),

  /**
   * Disables reduced debug screen information
   */
  DISABLE_REDUCED_DEBUG_SCREEN(23, PacketPlayer.class),

  /**
   * Set op permission level to 0
   */
  OP_PERMISSION_LEVEL_0(24, PacketPlayer.class),

  /**
   * Set op permission level to 1
   */
  OP_PERMISSION_LEVEL_1(25, PacketPlayer.class),

  /**
   * Set op permission level to 2
   */
  OP_PERMISSION_LEVEL_2(26, PacketPlayer.class),

  /**
   * Set op permission level to 3
   */
  OP_PERMISSION_LEVEL_3(27, PacketPlayer.class),

  /**
   * Set op permission level to 4
   */
  OP_PERMISSION_LEVEL_4(28, PacketPlayer.class),

  /**
   * Spawn cloud particles at the player. Sent to a player whose Bad Omen effect is removed to
   * either start a raid or increase its difficulty.
   */
  RAID_PARTICLES(43, PacketPlayer.class),

  /**
   * Plays the hit sound, and resets a hit cooldown.
   */
  ARMOR_STAND_HIT(32, PacketArmorStand.class),

  /**
   * Spawn explosion particle.
   * <p>
   * Used when:
   * <ul>
   * <li>A silverfish enters a block</li>
   * <li>A silverfish exits a block</li>
   * <li>A mob spawner (or minecart mob spawner) spawns an entity (only with entities that support this status)</li>
   * </ul>
   */
  SPAWN_EXPLOSION_PARTICLE(20, PacketMob.class),

  /**
   * Resets the squid's rotation to {@code 0} radians. Occurs whenever the server calculates that
   * the squid has rotated more than {@code 2 pi} radians.
   */
  SQUID_RESET_ROTATION(19, PacketSquid.class),

  /**
   * Causes several "happy villager" particles to appear; used when the dolphin has been fed and is
   * locating a structure
   */
  DOLPHIN_HAPPY(38, PacketMob.class),

  /**
   * Spawn "love mode" heart particles
   */
  LOVE_PARTICLES(18, PacketAnimal.class),

  /**
   * Spawn smoke particles (taming failed)
   */
  HORSE_SMOKE(6, PacketAbstractHorse.class),

  /**
   * Spawn heart particles (taming succeeded)
   */
  HORSE_HEARTS(7, PacketAbstractHorse.class),

  /**
   * Spawn smoke particles (taming failed)
   */
  OCELOT_SMOKE(40, PacketOcelot.class),

  /**
   * Spawn heart particles (taming succeeded)
   */
  OCELOT_HEARTS(41, PacketOcelot.class),

  /**
   * Causes the rabbit to use its rotated jumping animation, and displays jumping particles.
   */
  RABBIT_JUMP(1, PacketRabbit.class),

  /**
   * Causes the sheep to play the eating grass animation for the next 40 ticks
   */
  SHEEP_EAT_GRASS(10, PacketSheep.class),

  /**
   * Spawns particles based on the item on the fox's mouth (technically its main hand) to indicate
   * them chewing on it
   */
  FOX_EATING(45, PacketFox.class),

  /**
   * Lower head for ramming
   */
  GOAT_LOWER_HEAD(58, PacketGoat.class),

  /**
   * Stop lowering head
   */
  GOAT_STOP_LOWER_HEAD(59, PacketGoat.class),

  /**
   * Spawn smoke particles (taming failed)
   */
  TAMING_FAILED_SMOKE(6, PacketTameableAnimal.class),

  /**
   * Spawn heart particles (taming succeeded)
   */
  TAMING_SUCCEEDED_HEARTS(7, PacketTameableAnimal.class),

  /**
   * Play wolf shaking water animation
   */
  WOLF_SHAKING_WATER(8, PacketWolf.class),

  /**
   * Stop wolf shaking water animation
   */
  WOLF_STOP_SHAKING_WATER(56, PacketWolf.class),

  /**
   * Spawn villager mating heart particles
   */
  VILLAGER_MATING_HEARTS(12, PacketVillager.class),

  /**
   * Spawn villager angry particles
   */
  VILLAGER_ANGRY(13, PacketVillager.class),

  /**
   * Spawn villager happy particles
   */
  VILLAGER_HAPPY(14, PacketVillager.class),

  /**
   * Spawn "splash" particles. Triggered with 1% chance each tick while a raid is active.
   */
  VILLAGER_SPLASH(42, PacketVillager.class),

  /**
   * Plays attack animation and attack sound
   */
  IRON_GOLEM_ATTACK(4, PacketIronGolem.class),

  /**
   * Causes golem to hold out a ~~rose~~ poppy for 400 ticks (20 seconds)
   */
  IRON_GOLEM_HOLD_ROSE_POPPY(11, PacketIronGolem.class),

  /**
   * Puts away golem's poppy
   */
  IRON_GOLEM_STOP_HOLDING_ROSE_POPPY(34, PacketIronGolem.class),

  /**
   * Starts the attack animation, and plays the {@code entity.evocation_fangs.attack} sound.
   */
  EVOKER_FANGS_ATTACK(4, PacketEvokerFangs.class),

  /**
   * Spawns between 10 and 45 witchMagic particles. This status has a .075% chance of happening each
   * tick.
   */
  WITCH_MAGIC_PARTICLES(15, PacketWitch.class),

  /**
   * Starts the attack animation.
   */
  RAVAGER_ATTACK(4, PacketRavager.class),

  /**
   * Marks the ravager as stunned for the next 40 ticks.
   */
  RAVAGER_STUNNED(39, PacketRavager.class),

  /**
   * Plays the {@code entity.zombie_villager.cure} sound effect (unless the entity is silent)
   */
  ZOMBIE_VILLAGER_CURE(16, PacketZombieVillager.class),

  /**
   * Plays the guardian attack sound effect from this entity.
   */
  GUARDIAN_ATTACK(21, PacketGuardian.class),

  /**
   * Causes the TNT to ignite. Does not play a sound; the sound must be played separately.
   */
  TNT_IGNITE(10, PacketTntMinecart.class),

  /**
   * Resets the delay of the spawner to 200 ticks (the default minimum value).
   */
  SPAWNER_RESET_DELAY(1, PacketSpawnerMinecart.class),

  /**
   * Plays the attack animation for 10 ticks and plays the attack sound.
   */
  HOGLIN_ATTACK(4, PacketHoglin.class),

  /**
   * Plays the attack animation for 10 ticks and plays the attack sound.
   */
  ZOGLIN_ATTACK(4, PacketZoglin.class),

//    /**
//     * Spawns heart particles, used upon Allay duplication.
//     */
//    ALLAY_DUPLICATION(18, PacketAllay.class), TODO

  /**
   * Stops the roar animation and performs the attack animation.
   */
  WARDEN_ATTACK(4, PacketWarden.class),

  /**
   * Performs tendril shaking animation for 10 ticks.
   */
  WARDEN_SHAKE_TENDRILS(61, PacketWarden.class),

  /**
   * Performs the sonic boom attack animation (Charge and release). Beam and sound are not
   * included.
   */
  WARDEN_SONIC_BOOM(62, PacketWarden.class),

  /**
   * Plays the digging sound. Only works if the Sniffer has a target, and is in a digging or
   * searching state.
   */
  SNIFFER_DIG(63, PacketSniffer.class);


  private final byte statusId;
  private final Class<PacketEntity<?>> applicableClass;

  <T extends PacketEntity<T>> EntityStatus(int statusId, Class<T> applicableClass) {
    this.statusId = (byte) statusId;
    this.applicableClass = (Class<PacketEntity<?>>) applicableClass;
  }

  public byte getStatusId() {
    return statusId;
  }

  public void checkApplicable(PacketEntity<?> entity) {
    checkState(applicableClass.isAssignableFrom(entity.getClass()),
        "Entity %s is not applicable for status %s", entity.getClass().getSimpleName(), this);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name())
        .add("statusId", statusId)
        .toString();
  }
}
