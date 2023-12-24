package dev.slne.surf.surfapi.bukkit.api.packet.meta;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import me.tofaa.entitylib.meta.types.LivingEntityMeta;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.*;


/**
 * The {@code EntityType} class represents the types of entities that can exist in the game.
 * Each entity type has a unique identifier and can be accessed using the static fields defined in this class.
 */
@SuppressWarnings("unused")
@ApiStatus.NonExtendable
public final class EntityType<T extends EntityMeta> {

    /**
     * A map of all entity types by their name.
     */
    private static final Object2ObjectMap<String, EntityType0<?>> BY_NAME = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    public static final LivingEntityType<SnifferMeta> SNIFFER = createL(SnifferMeta.class, EntityTypes.SNIFFER);
    public static final EntityType0<InteractionMeta> INTERACTION = create(InteractionMeta.class, EntityTypes.INTERACTION);
    public static final EntityType0<BlockDisplayMeta> BLOCK_DISPLAY = create(BlockDisplayMeta.class, EntityTypes.BLOCK_DISPLAY);
    public static final EntityType0<ItemDisplayMeta> ITEM_DISPLAY = create(ItemDisplayMeta.class, EntityTypes.ITEM_DISPLAY);
    public static final EntityType0<TextDisplayMeta> TEXT_DISPLAY = create(TextDisplayMeta.class, EntityTypes.TEXT_DISPLAY);
    public static final EntityType0<AreaEffectCloudMeta> AREA_EFFECT_CLOUD = create(AreaEffectCloudMeta.class, EntityTypes.AREA_EFFECT_CLOUD);
    public static final LivingEntityType<ArmorStandMeta> ARMOR_STAND = createL(ArmorStandMeta.class, EntityTypes.ARMOR_STAND);
    public static final EntityType0<BoatMeta> BOAT = create(BoatMeta.class, EntityTypes.BOAT);
    public static final EntityType0<DragonFireballMeta> DRAGON_FIREBALL = create(DragonFireballMeta.class, EntityTypes.DRAGON_FIREBALL);
    public static final EntityType0<EndCrystalMeta> END_CRYSTAL = create(EndCrystalMeta.class, EntityTypes.END_CRYSTAL);
    public static final LivingEntityType<EnderDragonMeta> ENDER_DRAGON = createL(EnderDragonMeta.class, EntityTypes.ENDER_DRAGON);
    public static final EntityType0<EvokerFangsMeta> EVOKER_FANGS = create(EvokerFangsMeta.class, EntityTypes.EVOKER_FANGS);
    public static final EntityType0<FallingBlockMeta> FALLING_BLOCK = create(FallingBlockMeta.class, EntityTypes.FALLING_BLOCK);
    public static final EntityType0<FireworkRocketMeta> FIREWORK_ROCKET = create(FireworkRocketMeta.class, EntityTypes.FIREWORK_ROCKET);
    public static final EntityType0<FishingHookMeta> FISHING_HOOK = create(FishingHookMeta.class, EntityTypes.FISHING_BOBBER);
    public static final EntityType0<GlowItemFrameMeta> GLOW_ITEM_FRAME = create(GlowItemFrameMeta.class, EntityTypes.GLOW_ITEM_FRAME);
    public static final EntityType0<ItemFrameMeta> ITEM_FRAME = create(ItemFrameMeta.class, EntityTypes.ITEM_FRAME);
    public static final EntityType0<LeashKnotMeta> LEASH_KNOT = create(LeashKnotMeta.class, EntityTypes.LEASH_KNOT);
    public static final EntityType0<LightningBoltMeta> LIGHTNING_BOLT = create(LightningBoltMeta.class, EntityTypes.LIGHTNING_BOLT);
    public static final EntityType0<LlamaSpitMeta> LLAMA_SPIT = create(LlamaSpitMeta.class, EntityTypes.LLAMA_SPIT);
    public static final EntityType0<MarkerMeta> MARKER = create(MarkerMeta.class, EntityTypes.MARKER);
    public static final EntityType0<PaintingMeta> PAINTING = create(PaintingMeta.class, EntityTypes.PAINTING);
    public static final EntityType0<PrimedTntMeta> PRIMED_TNT = create(PrimedTntMeta.class, EntityTypes.PRIMED_TNT);
    public static final EntityType0<WitherSkullMeta> WITHER_SKULL = create(WitherSkullMeta.class, EntityTypes.WITHER_SKULL);
    public static final LivingEntityType<ZoglinMeta> ZOGLIN = createL(ZoglinMeta.class, EntityTypes.ZOGLIN);
    public static final LivingEntityType<WitherMeta> WITHER = createL(WitherMeta.class, EntityTypes.WITHER);
    public static final LivingEntityType<VexMeta> VEX = createL(VexMeta.class, EntityTypes.VEX);
    public static final LivingEntityType<SpiderMeta> SPIDER = createL(SpiderMeta.class, EntityTypes.SPIDER);
    public static final LivingEntityType<SilverfishMeta> SILVERFISH = createL(SilverfishMeta.class, EntityTypes.SILVERFISH);
    public static final LivingEntityType<GuardianMeta> GUARDIAN = createL(GuardianMeta.class, EntityTypes.GUARDIAN);
    public static final LivingEntityType<GiantMeta> GIANT = createL(GiantMeta.class, EntityTypes.GIANT);
    public static final LivingEntityType<EndermiteMeta> ENDERMITE = createL(EndermiteMeta.class, EntityTypes.ENDERMITE);
    public static final LivingEntityType<ElderGuardianMeta> ELDER_GUARDIAN = createL(ElderGuardianMeta.class, EntityTypes.ELDER_GUARDIAN);
    public static final LivingEntityType<CreeperMeta> CREEPER = createL(CreeperMeta.class, EntityTypes.CREEPER);
    public static final LivingEntityType<CaveSpiderMeta> CAVE_SPIDER = createL(CaveSpiderMeta.class, EntityTypes.CAVE_SPIDER);
    public static final LivingEntityType<BlazeMeta> BLAZE = createL(BlazeMeta.class, EntityTypes.BLAZE);
    public static final LivingEntityType<PiglinMeta> PIGLIN = createL(PiglinMeta.class, EntityTypes.PIGLIN);
    public static final LivingEntityType<PiglinBruteMeta> PIGLIN_BRUTE = createL(PiglinBruteMeta.class, EntityTypes.PIGLIN_BRUTE);
    public static final LivingEntityType<EvokerMeta> EVOKER = createL(EvokerMeta.class, EntityTypes.EVOKER);
    public static final LivingEntityType<IllusionerMeta> ILLUSIONER = createL(IllusionerMeta.class, EntityTypes.ILLUSIONER);
    public static final LivingEntityType<PillagerMeta> PILLAGER = createL(PillagerMeta.class, EntityTypes.PILLAGER);
    public static final LivingEntityType<RavagerMeta> RAVAGER = createL(RavagerMeta.class, EntityTypes.RAVAGER);
    public static final LivingEntityType<VindicatorMeta> VINDICATOR = createL(VindicatorMeta.class, EntityTypes.VINDICATOR);
    public static final LivingEntityType<WitchMeta> WITCH = createL(WitchMeta.class, EntityTypes.WITCH);
    public static final LivingEntityType<SkeletonMeta> SKELETON = createL(SkeletonMeta.class, EntityTypes.SKELETON);
    public static final LivingEntityType<StrayMeta> STRAY = createL(StrayMeta.class, EntityTypes.STRAY);
    public static final LivingEntityType<WitherSkeletonMeta> WITHER_SKELETON = createL(WitherSkeletonMeta.class, EntityTypes.WITHER_SKELETON);
    public static final LivingEntityType<DrownedMeta> DROWNED = createL(DrownedMeta.class, EntityTypes.DROWNED);
    public static final LivingEntityType<HuskMeta> HUSK = createL(HuskMeta.class, EntityTypes.HUSK);
    public static final LivingEntityType<ZombieMeta> ZOMBIE = createL(ZombieMeta.class, EntityTypes.ZOMBIE);
    public static final LivingEntityType<ZombieVillagerMeta> ZOMBIE_VILLAGER = createL(ZombieVillagerMeta.class, EntityTypes.ZOMBIE_VILLAGER);
    public static final LivingEntityType<ZombifiedPiglinMeta> ZOMBIFIED_PIGLIN = createL(ZombifiedPiglinMeta.class, EntityTypes.ZOMBIFIED_PIGLIN);
    public static final LivingEntityType<AxolotlMeta> AXOLOTL = createL(AxolotlMeta.class, EntityTypes.AXOLOTL);
    public static final LivingEntityType<CodMeta> COD = createL(CodMeta.class, EntityTypes.COD);
    public static final LivingEntityType<DolphinMeta> DOLPHIN = createL(DolphinMeta.class, EntityTypes.DOLPHIN);
    public static final LivingEntityType<GlowSquidMeta> GLOW_SQUID = createL(GlowSquidMeta.class, EntityTypes.GLOW_SQUID);
    public static final LivingEntityType<PufferFishMeta> PUFFERFISH = createL(PufferFishMeta.class, EntityTypes.PUFFERFISH);
    public static final LivingEntityType<SalmonMeta> SALMON = createL(SalmonMeta.class, EntityTypes.SALMON);
    public static final LivingEntityType<TropicalFishMeta> TROPICAL_FISH = createL(TropicalFishMeta.class, EntityTypes.TROPICAL_FISH);
    public static final EntityType0<ArrowMeta> ARROW = create(ArrowMeta.class, EntityTypes.ARROW);
    public static final LivingEntityType<VillagerMeta> VILLAGER = createL(VillagerMeta.class, EntityTypes.VILLAGER);
    public static final LivingEntityType<WanderingTraderMeta> WANDERING_TRADER = createL(WanderingTraderMeta.class, EntityTypes.WANDERING_TRADER);
    public static final EntityType0<ChestMinecartMeta> CHEST_MINECART = create(ChestMinecartMeta.class, EntityTypes.CHEST_MINECART);
    public static final EntityType0<CommandBlockMinecartMeta> COMMAND_BLOCK_MINECART = create(CommandBlockMinecartMeta.class, EntityTypes.COMMAND_BLOCK_MINECART);
    public static final EntityType0<FurnaceMinecartMeta> FURNACE_MINECART = create(FurnaceMinecartMeta.class, EntityTypes.FURNACE_MINECART);
    public static final EntityType0<FurnaceMinecartMeta> HOPPER_MINECART = create(FurnaceMinecartMeta.class, EntityTypes.HOPPER_MINECART);
    public static final EntityType0<SpawnerMinecartMeta> SPAWNER_MINECART = create(SpawnerMinecartMeta.class, EntityTypes.SPAWNER_MINECART);
    public static final EntityType0<TntMinecartMeta> TNT_MINECART = create(TntMinecartMeta.class, EntityTypes.TNT_MINECART);
    public static final LivingEntityType<PlayerMeta> PLAYER = createL(PlayerMeta.class, EntityTypes.PLAYER);
    public static final EntityType0<ThrownExpBottleMeta> THROWN_EXP_BOTTLE = create(ThrownExpBottleMeta.class, EntityTypes.THROWN_EXP_BOTTLE);
    public static final EntityType0<ThrownEggMeta> EGG = create(ThrownEggMeta.class, EntityTypes.EGG);
    public static final EntityType0<ThrownTridentMeta> TRIDENT = create(ThrownTridentMeta.class, EntityTypes.TRIDENT);
    public static final EntityType0<ThrownTridentMeta> POTION = create(ThrownTridentMeta.class, EntityTypes.POTION);
    public static final EntityType0<SmallFireballMeta> SMALL_FIREBALL = create(SmallFireballMeta.class, EntityTypes.SMALL_FIREBALL);
    public static final LivingEntityType<PigMeta> PIG = createL(PigMeta.class, EntityTypes.PIG);
    public static final LivingEntityType<CowMeta> COW = createL(CowMeta.class, EntityTypes.COW);
    public static final LivingEntityType<ChickenMeta> CHICKEN = createL(ChickenMeta.class, EntityTypes.CHICKEN);
    public static final LivingEntityType<BeeMeta> BEE = createL(BeeMeta.class, EntityTypes.BEE);
    public static final LivingEntityType<TurtleMeta> TURTLE = createL(TurtleMeta.class, EntityTypes.TURTLE);
    public static final LivingEntityType<DonkeyMeta> DONKEY = createL(DonkeyMeta.class, EntityTypes.DONKEY);
    public static final LivingEntityType<SheepMeta> SHEEP = createL(SheepMeta.class, EntityTypes.SHEEP);
    public static final LivingEntityType<RabbitMeta> RABBIT = createL(RabbitMeta.class, EntityTypes.RABBIT);
    public static final LivingEntityType<PolarBearMeta> POLAR_BEAR = createL(PolarBearMeta.class, EntityTypes.POLAR_BEAR);
    public static final LivingEntityType<OcelotMeta> OCELOT = createL(OcelotMeta.class, EntityTypes.OCELOT);
    public static final LivingEntityType<PandaMeta> PANDA = createL(PandaMeta.class, EntityTypes.PANDA);
    public static final LivingEntityType<StriderMeta> STRIDER = createL(StriderMeta.class, EntityTypes.STRIDER);
    public static final LivingEntityType<FoxMeta> FOX = createL(FoxMeta.class, EntityTypes.FOX);
    public static final LivingEntityType<FrogMeta> FROG = createL(FrogMeta.class, EntityTypes.FROG);
    public static final LivingEntityType<GoatMeta> GOAT = createL(GoatMeta.class, EntityTypes.GOAT);
    public static final LivingEntityType<HoglinMeta> HOGLIN = createL(HoglinMeta.class, EntityTypes.HOGLIN);
    public static final LivingEntityType<CatMeta> CAT = createL(CatMeta.class, EntityTypes.CAT);
    public static final LivingEntityType<ParrotMeta> PARROT = createL(ParrotMeta.class, EntityTypes.PARROT);
    public static final LivingEntityType<WolfMeta> WOLF = createL(WolfMeta.class, EntityTypes.WOLF);
    public static final LivingEntityType<HorseMeta> HORSE = createL(HorseMeta.class, EntityTypes.HORSE);
    public static final LivingEntityType<LlamaMeta> LLAMA = createL(LlamaMeta.class, EntityTypes.LLAMA);
    public static final LivingEntityType<MuleMeta> MULE = createL(MuleMeta.class, EntityTypes.MULE);
    public static final LivingEntityType<SkeletonHorseMeta> SKELETON_HORSE = createL(SkeletonHorseMeta.class, EntityTypes.SKELETON_HORSE);
    public static final LivingEntityType<ZombieHorseMeta> ZOMBIE_HORSE = createL(ZombieHorseMeta.class, EntityTypes.ZOMBIE_HORSE);
    public static final LivingEntityType<SlimeMeta> SLIME = createL(SlimeMeta.class, EntityTypes.SLIME);
    public static final LivingEntityType<MagmaCubeMeta> MAGMA_CUBE = createL(MagmaCubeMeta.class, EntityTypes.MAGMA_CUBE);
    public static final EntityType0<ShulkerBulletMeta> SHULKER_BULLET = create(ShulkerBulletMeta.class, EntityTypes.SHULKER_BULLET);
    public static final EntityType0<TraderLlamaMeta> TRADER_LLAMA = create(TraderLlamaMeta.class, EntityTypes.TRADER_LLAMA);
    public static final LivingEntityType<BatMeta> BAT = createL(BatMeta.class, EntityTypes.BAT);
    public static final LivingEntityType<IronGolemMeta> IRON_GOLEM = createL(IronGolemMeta.class, EntityTypes.IRON_GOLEM);
    public static final LivingEntityType<ShulkerMeta> SHULKER = createL(ShulkerMeta.class, EntityTypes.SHULKER);
    public static final LivingEntityType<SnowGolemMeta> SNOW_GOLEM = createL(SnowGolemMeta.class, EntityTypes.SNOW_GOLEM);

    // ---------------------------------------------------------------------------------------------------------------//

    /**
     * Creates a new entity type with the given meta class and type.
     */
    @Contract(value = "_, _ -> new", pure = true)
    private static <T extends EntityMeta> @NotNull EntityType0<T> create(Class<T> metaClass, com.github.retrooper.packetevents.protocol.entity.type.EntityType type) {
        final EntityType0<T> newEntityType = new EntityType0<>(metaClass, type);
        BY_NAME.put(type.getName().getKey(), newEntityType);
        return newEntityType;
    }

    /**
     * Creates a new entity type with the given meta class and type.
     */
    @Contract(value = "_, _ -> new", pure = true)
    private static <T extends LivingEntityMeta> @NotNull LivingEntityType<T> createL(Class<T> metaClass, com.github.retrooper.packetevents.protocol.entity.type.EntityType type) {
        final LivingEntityType<T> newEntityType = new LivingEntityType<>(metaClass, type);
        BY_NAME.put(type.getName().getKey(), newEntityType);
        return newEntityType;
    }

    // ---------------------------------------------------------------------------------------------------------------//

    /**
     * Returns the entity type with the given name.
     *
     * @param name the name of the entity type
     * @param <T>  the type of the entity
     * @return the entity type with the given name
     */
    @SuppressWarnings("unchecked")
    public static <T extends EntityMeta> EntityType0<T> getByName(String name) {
        return (EntityType0<T>) BY_NAME.get(name.contains(":") ? name.substring(name.indexOf(":") + 1) : name);
    }

    @SuppressWarnings("unchecked")
    public static <T extends LivingEntityMeta> LivingEntityType<T> getLivingByName(String name) {
        final EntityType0<?> type0 = getByName(name);
        checkState(type0 instanceof LivingEntityType, "EntityType is not a LivingEntityType");

        try {
            return (LivingEntityType<T>) type0;
        } catch (ClassCastException e) {
            throw new IllegalStateException("The given entity type is not the same as the requested type", e);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//

    public static sealed class EntityType0<M extends EntityMeta> permits LivingEntityType {
        /**
         * The meta class associated with this entity type.
         */
        private final Class<M> metaClass;
        /**
         * The type of the entity.
         */
        private final com.github.retrooper.packetevents.protocol.entity.type.EntityType type;

        /**
         * Creates a new entity type with the given meta class and type.
         *
         * @param metaClass the meta class associated with this entity type
         * @param type      the type of the entity
         */
        @ApiStatus.Internal
        @Contract(pure = true)
        private EntityType0(Class<M> metaClass, com.github.retrooper.packetevents.protocol.entity.type.EntityType type) {
            this.metaClass = metaClass;
            this.type = type;
        }

        /**
         * Returns the meta class associated with this entity type.
         *
         * @return the meta class associated with this entity type
         */
        @Contract(pure = true)
        public Class<M> getMetaClass() {
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

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("metaClass", metaClass)
                    .add("type", type.getName().toString())
                    .toString();
        }
    }

    public static final class LivingEntityType<M extends LivingEntityMeta> extends EntityType0<M> {

        /**
         * Creates a new entity type with the given meta class and type.
         *
         * @param metaClass the meta class associated with this entity type
         * @param type      the type of the entity
         */
        private LivingEntityType(Class<M> metaClass, com.github.retrooper.packetevents.protocol.entity.type.EntityType type) {
            super(metaClass, type);
        }
    }
}
