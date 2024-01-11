package dev.slne.surf.surfapi.core.api.packet.events;

import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import dev.slne.surf.surfapi.core.api.util.ById;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.ApiStatus;

@SuppressWarnings("unused")
public final class WorldEvent {

    /**
     * Dispenser dispenses
     */
    public static final SimpleType DISPENSER_DISPENSES = simpleType(1_000);

    /**
     * Dispenser fails to dispense
     */
    public static final SimpleType DISPENSER_FAILS_TO_DISPENSE = simpleType(1_001);

    /**
     * Dispenser shoots
     */
    public static final SimpleType DISPENSER_SHOOTS = simpleType(1_002);

    /**
     * Ender eye launched
     */
    public static final SimpleType ENDER_EYE_LAUNCHED = simpleType(1_003);

    /**
     * Firework shot
     */
    public static final SimpleType FIREWORK_SHOT = simpleType(1_004);

    /**
     * Iron door opened
     */
    public static final SimpleType IRON_DOOR_OPENED = simpleType(1_005);

    /**
     * Wooden door opened
     */
    public static final SimpleType WOODEN_DOOR_OPENED = simpleType(1_006);

    /**
     * Wooden trapdoor opened
     */
    public static final SimpleType WOODEN_TRAPDOOR_OPENED = simpleType(1_007);

    /**
     * Fence gate opened
     */
    public static final SimpleType FENCE_GATE_OPENED = simpleType(1_008);

    /**
     * Fire extinguished
     */
    public static final SimpleType FIRE_EXTINGUISHED = simpleType(1_009);

    /**
     * Play record
     */
    public static final PlayRecordType PLAY_RECORD = new PlayRecordType(1_010);

    /**
     * Iron door closed
     */
    public static final SimpleType IRON_DOOR_CLOSED = simpleType(1_011);

    /**
     * Wooden door closed
     */
    public static final SimpleType WOODEN_DOOR_CLOSED = simpleType(1_012);

    /**
     * Wooden trapdoor closed
     */
    public static final SimpleType WOODEN_TRAPDOOR_CLOSED = simpleType(1_013);

    /**
     * Fence gate closed
     */
    public static final SimpleType FENCE_GATE_CLOSED = simpleType(1_014);

    /**
     * Ghast warning
     */
    public static final SimpleType GHAST_WARNING = simpleType(1_015);

    /**
     * Ghast shoots
     */
    public static final SimpleType GHAST_SHOOTS = simpleType(1_016);

    /**
     * Ender dragon shoots
     */
    public static final SimpleType ENDER_DRAGON_SHOOTS = simpleType(1_017);

    /**
     * Blaze shoots
     */
    public static final SimpleType BLAZE_SHOOTS = simpleType(1_018);

    /**
     * Zombie attacks wooden door
     */
    public static final SimpleType ZOMBIE_ATTACKS_WOODEN_DOOR = simpleType(1_019);

    /**
     * Zombie attacks iron door
     */
    public static final SimpleType ZOMBIE_ATTACKS_IRON_DOOR = simpleType(1_020);

    /**
     * Zombie breaks wooden door
     */
    public static final SimpleType ZOMBIE_BREAKS_WOODEN_DOOR = simpleType(1_021);

    /**
     * Wither breaks block
     */
    public static final SimpleType WITHER_BREAKS_BLOCK = simpleType(1_022);

    /**
     * Wither spawns
     */
    public static final SimpleAllowBooleanType WITHER_SPAWNS = simpleAllowBooleanType(1_023);

    /**
     * Wither shoots
     */
    public static final SimpleType WITHER_SHOOTS = simpleType(1_024);

    /**
     * Bat takes off
     */
    public static final SimpleType BAT_TAKES_OFF = simpleType(1_025);

    /**
     * Zombie infects villager
     */
    public static final SimpleType ZOMBIE_INFECTS_VILLAGER = simpleType(1_026);

    /**
     * Zombie converts villager
     */
    public static final SimpleType ZOMBIE_CONVERTS_VILLAGER = simpleType(1_027);

    /**
     * Ender dragon death
     */
    public static final SimpleAllowBooleanType ENDER_DRAGON_DEATH = simpleAllowBooleanType(1_028);

    /**
     * Anvil destroyed
     */
    public static final SimpleType ANVIL_DESTROYED = simpleType(1_029);

    /**
     * Anvil used
     */
    public static final SimpleType ANVIL_USED = simpleType(1_030);

    /**
     * Anvil landed
     */
    public static final SimpleType ANVIL_LANDED = simpleType(1_031);

    /**
     * Portal travel
     */
    public static final SimpleType PORTAL_TRAVEL = simpleType(1_032);

    /**
     * Chorus flower grows
     */
    public static final SimpleType CHORUS_FLOWER_GROWS = simpleType(1_033);

    /**
     * Chorus flower dies
     */
    public static final SimpleType CHORUS_FLOWER_DIES = simpleType(1_034);

    /**
     * Brewing stand brews
     */
    public static final SimpleType BREWING_STAND_BREWS = simpleType(1_035);

    /**
     * Iron trapdoor opened
     */
    public static final SimpleType IRON_TRAPDOOR_OPENED = simpleType(1_036);

    /**
     * Iron trapdoor closed
     */
    public static final SimpleType IRON_TRAPDOOR_CLOSED = simpleType(1_037);

    /**
     * End portal created in overworld
     */
    public static final SimpleAllowBooleanType END_PORTAL_CREATED_IN_OVERWORLD = simpleAllowBooleanType(1_038);

    /**
     * Phantoms bite
     */
    public static final SimpleType PHANTOMS_BITE = simpleType(1_039);

    /**
     * Zombie converts to drowned
     */
    public static final SimpleType ZOMBIE_CONVERTS_TO_DROWNED = simpleType(1_040);

    /**
     * Husk converts to zombie by drowning
     */
    public static final SimpleType HUSK_CONVERTS_TO_ZOMBIE_BY_DROWNING = simpleType(1_041);

    /**
     * Grindstone used
     */
    public static final SimpleType GRINDSTONE_USED = simpleType(1_042);

    /**
     * Book page turned
     */
    public static final SimpleType BOOK_PAGE_TURNED = simpleType(1_043);

    // Particles start //

    /**
     * Composter composts
     */
    public static final SimpleType COMPOSTER_COMPOSTS = simpleType(1_500);

    /**
     * Lava converts block (either water to stone, or removes existing blocks such as torches)
     */
    public static final SimpleType LAVA_CONVERTS_BLOCK = simpleType(1_501);

    /**
     * Redstone torch burns out
     */
    public static final SimpleType REDSTONE_TORCH_BURNS_OUT = simpleType(1_502);

    /**
     * Ender eye placed
     */
    public static final SimpleType ENDER_EYE_PLACED = simpleType(1_503);

    /**
     * Spawns 10 smoke particles, e.g. from a fire
     */
    public static final DirectionType SPAWNS_10_SMOKE_PARTICLES = new DirectionType(2_000);

    /**
     * Block break + block break sound
     */
    public static final BlockStateType BLOCK_BREAK = new BlockStateType(2_001);

    /**
     * Splash potion. Particle effect + glass break sound.
     */
    public static final TextColorType SPLASH_POTION = new TextColorType(2_002);

    /**
     * Eye of Ender entity break animation — particles and sound
     */
    public static final SimpleType EYE_OF_ENDER_BREAK = simpleType(2_003);

    /**
     * Mob spawn particle effect: smoke + flames
     */
    public static final SimpleType MOB_SPAWN = simpleType(2_004);

    /**
     * Bonemeal particles (How many particles to spawn (if set to 0, 15 are spawned).)
     */
    public static final IntType BONEMEAL_PARTICLES = new IntType(2_005);

    /**
     * Dragon breath
     */
    public static final SimpleType DRAGON_BREATH = simpleType(2_006);

    /**
     * Instant splash potion. Particle effect + glass break sound.
     */
    public static final TextColorType INSTANT_SPLASH_POTION = new TextColorType(2_007);

    /**
     * Ender dragon destroys block
     */
    public static final SimpleType ENDER_DRAGON_DESTROY_BLOCK = simpleType(2_008);

    /**
     * 	Wet sponge vaporizes in nether
     */
    public static final SimpleType WET_SPONGE_VAPORIZES_IN_NETHER = simpleType(2_009);

    /**
     * End gateway spawn
     */
    public static final SimpleType END_GATEWAY_SPAWN = simpleType(3_000);

    /**
     * Ender dragon growl
     */
    public static final SimpleType ENDER_DRAGON_GROWL = simpleType(3_001);

    /**
     * Electric spark
     */
    public static final SimpleType ELECTRIC_SPARK = simpleType(3_002);

    /**
     * 	Copper apply wax
     */
    public static final SimpleType COPPER_APPLY_WAX = simpleType(3_003);

    /**
     * Copper remove wax
     */
    public static final SimpleType COPPER_REMOVE_WAX = simpleType(3_004);

    /**
     * Copper scrape oxidation
     */
    public static final SimpleType COPPER_SCRAPE_OXIDATION = simpleType(3_005);


    /**
     * Creates a simple type world event without a value.
     *
     * @param id the id of the event
     * @return the world event
     */
    private static SimpleType simpleType(int id) {
        return new SimpleType(id);
    }

    private static SimpleAllowBooleanType simpleAllowBooleanType(int id) {
        return new SimpleAllowBooleanType(id);
    }

    public abstract static class Type<T> {

        private final int id;

        public Type(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public abstract int encode(T value);
    }

    public abstract static class AllowBooleanType<T> extends Type<T> {
        public AllowBooleanType(int id) {
            super(id);
        }
    }

    public static final class SimpleType extends Type<Void> {

        public SimpleType(int id) {
            super(id);
        }

        @Override
        public int encode(Void value) {
            return 0;
        }
    }

    public static final class SimpleAllowBooleanType extends AllowBooleanType<Void> {
        public SimpleAllowBooleanType(int id) {
            super(id);
        }

        @Override
        public int encode(Void value) {
            return 0;
        }
    }

    public static final class PlayRecordType extends Type<PlayRecordType.MusicDisc> {

        public PlayRecordType(int id) {
            super(id);
        }

        @Override
        public int encode(MusicDisc value) {
            return value.id();
        }

        @ApiStatus.Experimental // are these the correct ids?
        public enum MusicDisc implements ById {
            DISC_11(1171),
            DISC_13(1161),
            DISC_5(1175),
            DISC_BLOCKS(1163),
            DISC_CAT(1162),
            DISC_CHIRP(1164),
            DISC_FAR(1165),
            DISC_MALL(1166),
            DISC_MELLOHI(1167),
            DISC_OTHERSIDE(1173),
            DISC_PIGSTEP(1176),
            DISC_RELIC(1174),
            DISC_STAL(1168),
            DISC_STRAD(1169),
            DISC_WAIT(1172),
            DISC_WARD(1170);

            private final int id;

            MusicDisc(int id) {
                this.id = id;
            }

            @Override
            public int id() {
                return id;
            }
        }
    }

    public static final class DirectionType extends Type<Direction> {

        public DirectionType(int id) {
            super(id);
        }

        @Override
        public int encode(Direction value) {
            return value.ordinal();
        }
    }

    public static final class BlockStateType extends Type<WrappedBlockState> {

        public BlockStateType(int id) {
            super(id);
        }

        @Override
        public int encode(WrappedBlockState value) {
            return value.getGlobalId();
        }
    }

    public static final class TextColorType extends Type<TextColor> {

        public TextColorType(int id) {
            super(id);
        }

        @Override
        public int encode(TextColor value) {
            return value.value();
        }
    }

    public static final class IntType extends Type<Integer> {

        public IntType(int id) {
            super(id);
        }

        @Override
        public int encode(Integer value) {
            return value;
        }
    }
}
