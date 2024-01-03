package dev.slne.surf.surfapi.core.api.util;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.*;

public interface BlockStateFactory {

    static Builder builder(@NotNull StateType stateType) {
        return new BlockStateFactoryImpl.BuilderImpl(checkNotNull(stateType, "stateType").createBlockState().clone());
    }

    static Builder builder(@NotNull WrappedBlockState wrappedBlockState) {
        return new BlockStateFactoryImpl.BuilderImpl(checkNotNull(wrappedBlockState, "wrappedBlockState").clone());
    }

    static WrappedBlockState of(@NotNull StateType stateType) {
        return checkNotNull(stateType, "stateType").createBlockState().clone();
    }

    @ParametersAreNonnullByDefault
    @SuppressWarnings("unused")
    interface Builder {
        int age();

        Builder age(int age);

        boolean attached();

        Builder attached(boolean attached);

        Attachment attachment();

        Builder attachment(@NotNull Attachment attachment);

        Axis axis();

        Builder axis(@NotNull Axis axis);

        boolean berries();

        Builder berries(boolean berries);

        int bites();

        Builder bites(int bites);

        boolean bottom();

        Builder bottom(boolean bottom);

        int candles();

        Builder candles(int candles);

        int charges();

        Builder charges(int charges);

        boolean conditional();

        Builder conditional(boolean conditional);

        int delay();

        Builder delay(int delay);

        boolean disarmed();

        Builder disarmed(boolean disarmed);

        int distance();

        Builder distance(int distance);

        boolean down();

        Builder down(boolean down);

        boolean drag();

        Builder drag(boolean drag);

        int eggs();

        Builder eggs(int eggs);

        boolean enabled();

        Builder enabled(boolean enabled);

        boolean extended();

        Builder extended(boolean extended);

        boolean eye();

        Builder eye(boolean eye);

        Face face();

        Builder face(@NotNull Face face);

        BlockFace facing();

        Builder facing(@NotNull BlockFace facing);

        Half half();

        Builder half(@NotNull Half half);

        boolean hanging();

        Builder hanging(boolean hanging);

        boolean hasBook();

        Builder hasBook(boolean hasBook);

        boolean hasBottle0();

        Builder hasBottle0(boolean hasBottle0);

        boolean hasBottle1();

        Builder hasBottle1(boolean hasBottle1);

        boolean hasBottle2();

        Builder hasBottle2(boolean hasBottle2);

        boolean hasRecord();

        Builder hasRecord(boolean hasRecord);

        int hatch();

        Builder hatch(int hatch);

        Hinge hinge();

        Builder hinge(@NotNull Hinge hinge);

        int honeyLevel();

        Builder honeyLevel(int honeyLevel);

        boolean inWall();

        Builder inWall(boolean inWall);

        Instrument instrument();

        Builder instrument(@NotNull Instrument instrument);

        boolean inverted();

        Builder inverted(boolean inverted);

        int layers();

        Builder layers(int layers);

        Leaves leaves();

        Builder leaves(Leaves leaves);

        int level();

        Builder level(int level);

        boolean lit();

        Builder lit(boolean lit);

        boolean locked();

        Builder locked(boolean locked);

        Mode mode();

        Builder mode(@NotNull Mode mode);

        int moisture();

        Builder moisture(int moisture);

        North north();

        Builder north(@NotNull North north);

        int note();

        Builder note(int note);

        boolean occupied();

        Builder occupied(boolean occupied);

        boolean shrieking();

        Builder shrieking(boolean shrieking);

        boolean canSummon();

        Builder canSummon(boolean canSummon);

        boolean open();

        Builder open(boolean open);

        Orientation orientation();

        Builder orientation(Orientation orientation);

        Part part();

        Builder part(Part part);

        boolean persistent();

        Builder persistent(boolean persistent);

        int pickles();

        Builder pickles(int pickles);

        int power();

        Builder power(int power);

        boolean powered();

        Builder powered(boolean powered);

        int rotation();

        Builder rotation(int rotation);

        SculkSensorPhase sculkSensorPhase();

        Builder sculkSensorPhase(SculkSensorPhase sculkSensorPhase);

        Shape shape();

        Builder shape(Shape shape);

        boolean short_();

        Builder short_(boolean short_);

        boolean signalFire();

        Builder signalFire(boolean signalFire);

        boolean slotZeroOccupied();

        Builder slotZeroOccupied(boolean slotZeroOccupied);

        boolean slotOneOccupied();

        Builder slotOneOccupied(boolean slotOneOccupied);

        boolean slotTwoOccupied();

        Builder slotTwoOccupied(boolean slotTwoOccupied);

        boolean slotThreeOccupied();

        Builder slotThreeOccupied(boolean slotThreeOccupied);

        boolean slotFourOccupied();

        Builder slotFourOccupied(boolean slotFourOccupied);

        boolean slotFiveOccupied();

        Builder slotFiveOccupied(boolean slotFiveOccupied);

        boolean snowy();

        Builder snowy(boolean snowy);

        int stage();

        Builder stage(int stage);

        South south();

        Builder south(South south);

        Thickness thickness();

        Builder thickness(Thickness thickness);

        Tilt tilt();

        Builder tilt(Tilt tilt);

        boolean triggered();

        Builder triggered(boolean triggered);

        Type typeData();

        Builder typeData(Type type);

        boolean unstable();

        Builder unstable(boolean unstable);

        boolean up();

        Builder up(boolean up);

        VerticalDirection verticalDirection();

        Builder verticalDirection(VerticalDirection verticalDirection);

        boolean waterlogged();

        Builder waterlogged(boolean waterlogged);

        East east();

        Builder east(East east);

        West west();

        Builder west(West west);

        Bloom bloom();

        Builder bloom(Bloom bloom);

        boolean cracked();

        Builder cracked(boolean cracked);

        boolean crafting();

        Builder crafting(boolean crafting);

        TrialSpawnerState trialSpawnerState();

        Builder trialSpawnerState(TrialSpawnerState trialSpawnerState);

        WrappedBlockState build();
    }
}
