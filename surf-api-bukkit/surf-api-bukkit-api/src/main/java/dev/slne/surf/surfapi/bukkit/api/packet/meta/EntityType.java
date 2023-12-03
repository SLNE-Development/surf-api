package dev.slne.surf.surfapi.bukkit.api.packet.meta;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.mobs.DonkeyMeta;
import me.tofaa.entitylib.meta.mobs.*;
import me.tofaa.entitylib.meta.mobs.cuboid.MagmaCubeMeta;
import me.tofaa.entitylib.meta.mobs.cuboid.SlimeMeta;
import me.tofaa.entitylib.meta.mobs.golem.IronGolemMeta;
import me.tofaa.entitylib.meta.mobs.golem.ShulkerMeta;
import me.tofaa.entitylib.meta.mobs.golem.SnowGolemMeta;
import me.tofaa.entitylib.meta.mobs.horse.*;
import me.tofaa.entitylib.meta.mobs.minecart.*;
import me.tofaa.entitylib.meta.mobs.monster.*;
import me.tofaa.entitylib.meta.mobs.monster.piglin.PiglinBruteMeta;
import me.tofaa.entitylib.meta.mobs.monster.piglin.PiglinMeta;
import me.tofaa.entitylib.meta.mobs.monster.raider.*;
import me.tofaa.entitylib.meta.mobs.monster.skeleton.SkeletonMeta;
import me.tofaa.entitylib.meta.mobs.monster.skeleton.StrayMeta;
import me.tofaa.entitylib.meta.mobs.monster.skeleton.WitherSkeletonMeta;
import me.tofaa.entitylib.meta.mobs.monster.zombie.*;
import me.tofaa.entitylib.meta.mobs.passive.*;
import me.tofaa.entitylib.meta.mobs.tameable.CatMeta;
import me.tofaa.entitylib.meta.mobs.tameable.ParrotMeta;
import me.tofaa.entitylib.meta.mobs.tameable.WolfMeta;
import me.tofaa.entitylib.meta.mobs.villager.VillagerMeta;
import me.tofaa.entitylib.meta.mobs.villager.WanderingTraderMeta;
import me.tofaa.entitylib.meta.mobs.water.*;
import me.tofaa.entitylib.meta.other.*;
import me.tofaa.entitylib.meta.projectile.*;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * The {@code EntityType} class represents the types of entities that can exist in the game.
 * Each entity type has a unique identifier and can be accessed using the static fields defined in this class.
 */
@SuppressWarnings("unused")
@ApiStatus.NonExtendable
public final class EntityType<T extends EntityMeta> {

    public static final EntityType<SnifferMeta> SNIFFER = create(SnifferMeta.class, EntityTypes.SNIFFER);
    public static final EntityType<InteractionMeta> INTERACTION = create(InteractionMeta.class, EntityTypes.INTERACTION);
    public static final EntityType<BlockDisplayMeta> BLOCK_DISPLAY = create(BlockDisplayMeta.class, EntityTypes.BLOCK_DISPLAY);
    public static final EntityType<ItemDisplayMeta> ITEM_DISPLAY = create(ItemDisplayMeta.class, EntityTypes.ITEM_DISPLAY);
    public static final EntityType<TextDisplayMeta> TEXT_DISPLAY = create(TextDisplayMeta.class, EntityTypes.TEXT_DISPLAY);
    public static final EntityType<AreaEffectCloudMeta> AREA_EFFECT_CLOUD = create(AreaEffectCloudMeta.class, EntityTypes.AREA_EFFECT_CLOUD);
    public static final EntityType<ArmorStandMeta> ARMOR_STAND = create(ArmorStandMeta.class, EntityTypes.ARMOR_STAND);
    public static final EntityType<BoatMeta> BOAT = create(BoatMeta.class, EntityTypes.BOAT);
    public static final EntityType<DragonFireballMeta> DRAGON_FIREBALL = create(DragonFireballMeta.class, EntityTypes.DRAGON_FIREBALL);
    public static final EntityType<EndCrystalMeta> END_CRYSTAL = create(EndCrystalMeta.class, EntityTypes.END_CRYSTAL);
    public static final EntityType<EnderDragonMeta> ENDER_DRAGON = create(EnderDragonMeta.class, EntityTypes.ENDER_DRAGON);
    public static final EntityType<EvokerFangsMeta> EVOKER_FANGS = create(EvokerFangsMeta.class, EntityTypes.EVOKER_FANGS);
    public static final EntityType<FallingBlockMeta> FALLING_BLOCK = create(FallingBlockMeta.class, EntityTypes.FALLING_BLOCK);
    public static final EntityType<FireworkRocketMeta> FIREWORK_ROCKET = create(FireworkRocketMeta.class, EntityTypes.FIREWORK_ROCKET);
    public static final EntityType<FishingHookMeta> FISHING_HOOK = create(FishingHookMeta.class, EntityTypes.FISHING_BOBBER);
    public static final EntityType<GlowItemFrameMeta> GLOW_ITEM_FRAME = create(GlowItemFrameMeta.class, EntityTypes.GLOW_ITEM_FRAME);
    public static final EntityType<ItemFrameMeta> ITEM_FRAME = create(ItemFrameMeta.class, EntityTypes.ITEM_FRAME);
    public static final EntityType<LeashKnotMeta> LEASH_KNOT = create(LeashKnotMeta.class, EntityTypes.LEASH_KNOT);
    public static final EntityType<LightningBoltMeta> LIGHTNING_BOLT = create(LightningBoltMeta.class, EntityTypes.LIGHTNING_BOLT);
    public static final EntityType<LlamaSpitMeta> LLAMA_SPIT = create(LlamaSpitMeta.class, EntityTypes.LLAMA_SPIT);
    public static final EntityType<MarkerMeta> MARKER = create(MarkerMeta.class, EntityTypes.MARKER);
    public static final EntityType<PaintingMeta> PAINTING = create(PaintingMeta.class, EntityTypes.PAINTING);
    public static final EntityType<PrimedTntMeta> PRIMED_TNT = create(PrimedTntMeta.class, EntityTypes.PRIMED_TNT);
    public static final EntityType<WitherSkullMeta> WITHER_SKULL = create(WitherSkullMeta.class, EntityTypes.WITHER_SKULL);
    public static final EntityType<ZoglinMeta> ZOGLIN = create(ZoglinMeta.class, EntityTypes.ZOGLIN);
    public static final EntityType<WitherMeta> WITHER = create(WitherMeta.class, EntityTypes.WITHER);
    public static final EntityType<VexMeta> VEX = create(VexMeta.class, EntityTypes.VEX);
    public static final EntityType<SpiderMeta> SPIDER = create(SpiderMeta.class, EntityTypes.SPIDER);
    public static final EntityType<SilverfishMeta> SILVERFISH = create(SilverfishMeta.class, EntityTypes.SILVERFISH);
    public static final EntityType<GuardianMeta> GUARDIAN = create(GuardianMeta.class, EntityTypes.GUARDIAN);
    public static final EntityType<GiantMeta> GIANT = create(GiantMeta.class, EntityTypes.GIANT);
    public static final EntityType<EndermiteMeta> ENDERMITE = create(EndermiteMeta.class, EntityTypes.ENDERMITE);
    public static final EntityType<ElderGuardianMeta> ELDER_GUARDIAN = create(ElderGuardianMeta.class, EntityTypes.ELDER_GUARDIAN);
    public static final EntityType<CreeperMeta> CREEPER = create(CreeperMeta.class, EntityTypes.CREEPER);
    public static final EntityType<CaveSpiderMeta> CAVE_SPIDER = create(CaveSpiderMeta.class, EntityTypes.CAVE_SPIDER);
    public static final EntityType<BlazeMeta> BLAZE = create(BlazeMeta.class, EntityTypes.BLAZE);
    public static final EntityType<PiglinMeta> PIGLIN = create(PiglinMeta.class, EntityTypes.PIGLIN);
    public static final EntityType<PiglinBruteMeta> PIGLIN_BRUTE = create(PiglinBruteMeta.class, EntityTypes.PIGLIN_BRUTE);
    public static final EntityType<EvokerMeta> EVOKER = create(EvokerMeta.class, EntityTypes.EVOKER);
    public static final EntityType<IllusionerMeta> ILLUSIONER = create(IllusionerMeta.class, EntityTypes.ILLUSIONER);
    public static final EntityType<PillagerMeta> PILLAGER = create(PillagerMeta.class, EntityTypes.PILLAGER);
    public static final EntityType<RavagerMeta> RAVAGER = create(RavagerMeta.class, EntityTypes.RAVAGER);
    public static final EntityType<VindicatorMeta> VINDICATOR = create(VindicatorMeta.class, EntityTypes.VINDICATOR);
    public static final EntityType<WitchMeta> WITCH = create(WitchMeta.class, EntityTypes.WITCH);
    public static final EntityType<SkeletonMeta> SKELETON = create(SkeletonMeta.class, EntityTypes.SKELETON);
    public static final EntityType<StrayMeta> STRAY = create(StrayMeta.class, EntityTypes.STRAY);
    public static final EntityType<WitherSkeletonMeta> WITHER_SKELETON = create(WitherSkeletonMeta.class, EntityTypes.WITHER_SKELETON);
    public static final EntityType<DrownedMeta> DROWNED = create(DrownedMeta.class, EntityTypes.DROWNED);
    public static final EntityType<HuskMeta> HUSK = create(HuskMeta.class, EntityTypes.HUSK);
    public static final EntityType<ZombieMeta> ZOMBIE = create(ZombieMeta.class, EntityTypes.ZOMBIE);
    public static final EntityType<ZombieVillagerMeta> ZOMBIE_VILLAGER = create(ZombieVillagerMeta.class, EntityTypes.ZOMBIE_VILLAGER);
    public static final EntityType<ZombifiedPiglinMeta> ZOMBIFIED_PIGLIN = create(ZombifiedPiglinMeta.class, EntityTypes.ZOMBIFIED_PIGLIN);
    public static final EntityType<AxolotlMeta> AXOLOTL = create(AxolotlMeta.class, EntityTypes.AXOLOTL);
    public static final EntityType<CodMeta> COD = create(CodMeta.class, EntityTypes.COD);
    public static final EntityType<DolphinMeta> DOLPHIN = create(DolphinMeta.class, EntityTypes.DOLPHIN);
    public static final EntityType<GlowSquidMeta> GLOW_SQUID = create(GlowSquidMeta.class, EntityTypes.GLOW_SQUID);
    public static final EntityType<PufferFishMeta> PUFFERFISH = create(PufferFishMeta.class, EntityTypes.PUFFERFISH);
    public static final EntityType<SalmonMeta> SALMON = create(SalmonMeta.class, EntityTypes.SALMON);
    public static final EntityType<TropicalFishMeta> TROPICAL_FISH = create(TropicalFishMeta.class, EntityTypes.TROPICAL_FISH);
    public static final EntityType<ArrowMeta> ARROW = create(ArrowMeta.class, EntityTypes.ARROW);
    public static final EntityType<VillagerMeta> VILLAGER = create(VillagerMeta.class, EntityTypes.VILLAGER);
    public static final EntityType<WanderingTraderMeta> WANDERING_TRADER = create(WanderingTraderMeta.class, EntityTypes.WANDERING_TRADER);
    public static final EntityType<ChestMinecartMeta> CHEST_MINECART = create(ChestMinecartMeta.class, EntityTypes.CHEST_MINECART);
    public static final EntityType<CommandBlockMinecartMeta> COMMAND_BLOCK_MINECART = create(CommandBlockMinecartMeta.class, EntityTypes.COMMAND_BLOCK_MINECART);
    public static final EntityType<FurnaceMinecartMeta> FURNACE_MINECART = create(FurnaceMinecartMeta.class, EntityTypes.FURNACE_MINECART);
    public static final EntityType<FurnaceMinecartMeta> HOPPER_MINECART = create(FurnaceMinecartMeta.class, EntityTypes.HOPPER_MINECART);
    public static final EntityType<SpawnerMinecartMeta> SPAWNER_MINECART = create(SpawnerMinecartMeta.class, EntityTypes.SPAWNER_MINECART);
    public static final EntityType<TntMinecartMeta> TNT_MINECART = create(TntMinecartMeta.class, EntityTypes.TNT_MINECART);
    public static final EntityType<PlayerMeta> PLAYER = create(PlayerMeta.class, EntityTypes.PLAYER);
    public static final EntityType<ThrownExpBottleMeta> THROWN_EXP_BOTTLE = create(ThrownExpBottleMeta.class, EntityTypes.THROWN_EXP_BOTTLE);
    public static final EntityType<ThrownEggMeta> EGG = create(ThrownEggMeta.class, EntityTypes.EGG);
    public static final EntityType<ThrownTridentMeta> TRIDENT = create(ThrownTridentMeta.class, EntityTypes.TRIDENT);
    public static final EntityType<ThrownTridentMeta> POTION = create(ThrownTridentMeta.class, EntityTypes.POTION);
    public static final EntityType<SmallFireballMeta> SMALL_FIREBALL = create(SmallFireballMeta.class, EntityTypes.SMALL_FIREBALL);
    public static final EntityType<PigMeta> PIG = create(PigMeta.class, EntityTypes.PIG);
    public static final EntityType<CowMeta> COW = create(CowMeta.class, EntityTypes.COW);
    public static final EntityType<ChickenMeta> CHICKEN = create(ChickenMeta.class, EntityTypes.CHICKEN);
    public static final EntityType<BeeMeta> BEE = create(BeeMeta.class, EntityTypes.BEE);
    public static final EntityType<TurtleMeta> TURTLE = create(TurtleMeta.class, EntityTypes.TURTLE);
    public static final EntityType<DonkeyMeta> DONKEY = create(DonkeyMeta.class, EntityTypes.DONKEY);
    public static final EntityType<SheepMeta> SHEEP = create(SheepMeta.class, EntityTypes.SHEEP);
    public static final EntityType<RabbitMeta> RABBIT = create(RabbitMeta.class, EntityTypes.RABBIT);
    public static final EntityType<PolarBearMeta> POLAR_BEAR = create(PolarBearMeta.class, EntityTypes.POLAR_BEAR);
    public static final EntityType<OcelotMeta> OCELOT = create(OcelotMeta.class, EntityTypes.OCELOT);
    public static final EntityType<PandaMeta> PANDA = create(PandaMeta.class, EntityTypes.PANDA);
    public static final EntityType<StriderMeta> STRIDER = create(StriderMeta.class, EntityTypes.STRIDER);
    public static final EntityType<FoxMeta> FOX = create(FoxMeta.class, EntityTypes.FOX);
    public static final EntityType<FrogMeta> FROG = create(FrogMeta.class, EntityTypes.FROG);
    public static final EntityType<GoatMeta> GOAT = create(GoatMeta.class, EntityTypes.GOAT);
    public static final EntityType<HoglinMeta> HOGLIN = create(HoglinMeta.class, EntityTypes.HOGLIN);
    public static final EntityType<CatMeta> CAT = create(CatMeta.class, EntityTypes.CAT);
    public static final EntityType<ParrotMeta> PARROT = create(ParrotMeta.class, EntityTypes.PARROT);
    public static final EntityType<WolfMeta> WOLF = create(WolfMeta.class, EntityTypes.WOLF);
    public static final EntityType<HorseMeta> HORSE = create(HorseMeta.class, EntityTypes.HORSE);
    public static final EntityType<LlamaMeta> LLAMA = create(LlamaMeta.class, EntityTypes.LLAMA);
    public static final EntityType<MuleMeta> MULE = create(MuleMeta.class, EntityTypes.MULE);
    public static final EntityType<SkeletonHorseMeta> SKELETON_HORSE = create(SkeletonHorseMeta.class, EntityTypes.SKELETON_HORSE);
    public static final EntityType<ZombieHorseMeta> ZOMBIE_HORSE = create(ZombieHorseMeta.class, EntityTypes.ZOMBIE_HORSE);
    public static final EntityType<SlimeMeta> SLIME = create(SlimeMeta.class, EntityTypes.SLIME);
    public static final EntityType<MagmaCubeMeta> MAGMA_CUBE = create(MagmaCubeMeta.class, EntityTypes.MAGMA_CUBE);
    public static final EntityType<ShulkerBulletMeta> SHULKER_BULLET = create(ShulkerBulletMeta.class, EntityTypes.SHULKER_BULLET);
    public static final EntityType<TraderLlamaMeta> TRADER_LLAMA = create(TraderLlamaMeta.class, EntityTypes.TRADER_LLAMA);
    public static final EntityType<BatMeta> BAT = create(BatMeta.class, EntityTypes.BAT);
    public static final EntityType<IronGolemMeta> IRON_GOLEM = create(IronGolemMeta.class, EntityTypes.IRON_GOLEM);
    public static final EntityType<ShulkerMeta> SHULKER = create(ShulkerMeta.class, EntityTypes.SHULKER);
    public static final EntityType<SnowGolemMeta> SNOW_GOLEM = create(SnowGolemMeta.class, EntityTypes.SNOW_GOLEM);

    /**
     *
     */
    @Contract(value = "_, _ -> new", pure = true)
    private static <T extends EntityMeta> @NotNull EntityType<T> create(Class<T> metaClass, com.github.retrooper.packetevents.protocol.entity.type.EntityType type) {
        return new EntityType<>(metaClass, type);
    }

    // ------------------------------

    /**
     *
     */
    private final Class<T> metaClass;
    /**
     *
     */
    private final com.github.retrooper.packetevents.protocol.entity.type.EntityType type;

    /**
     *
     */
    @ApiStatus.Internal
    @Contract(pure = true)
    private EntityType(Class<T> metaClass, com.github.retrooper.packetevents.protocol.entity.type.EntityType type) {
        this.metaClass = metaClass;
        this.type = type;
    }

    /**
     * Returns the meta class associated with this entity type.
     *
     * @return the meta class associated with this entity type
     */
    @Contract(pure = true)
    public Class<T> getMetaClass() {
        return metaClass;
    }

    /**
     * Returns the type of the entity.
     *
     * @return the type of the entity
     */
    @Contract(pure = true)
    public com.github.retrooper.packetevents.protocol.entity.type.EntityType getType() {
        return type;
    }
}
