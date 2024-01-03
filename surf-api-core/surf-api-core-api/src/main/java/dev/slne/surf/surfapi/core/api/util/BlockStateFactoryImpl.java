package dev.slne.surf.surfapi.core.api.util;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.*;

public record BlockStateFactoryImpl() {

    @ParametersAreNonnullByDefault
    record BuilderImpl(WrappedBlockState blockState) implements BlockStateFactory.Builder {

        @Override
        public int age() {
            return blockState.getAge();
        }

        @Override
        public BlockStateFactory.Builder age(int age) {
            blockState.setAge(age);

            return this;
        }

        @Override
        public boolean attached() {
            return blockState.isAttached();
        }

        @Override
        public BlockStateFactory.Builder attached(boolean attached) {
            blockState.setAttached(attached);

            return this;
        }

        @Override
        public Attachment attachment() {
            return blockState.getAttachment();
        }

        @Override
        public BlockStateFactory.Builder attachment(@NotNull Attachment attachment) {
            blockState.setAttachment(checkNotNull(attachment, "attachment"));

            return this;
        }

        @Override
        public Axis axis() {
            return blockState.getAxis();
        }

        @Override
        public BlockStateFactory.Builder axis(@NotNull Axis axis) {
            blockState.setAxis(checkNotNull(axis, "axis"));

            return this;
        }

        @Override
        public boolean berries() {
            return blockState.isBerries();
        }

        @Override
        public BlockStateFactory.Builder berries(boolean berries) {
            blockState.setBerries(berries);

            return this;
        }

        @Override
        public int bites() {
            return blockState.getBites();
        }

        @Override
        public BlockStateFactory.Builder bites(int bites) {
            blockState.setBites(bites);

            return this;
        }

        @Override
        public boolean bottom() {
            return blockState.isBottom();
        }

        @Override
        public BlockStateFactory.Builder bottom(boolean bottom) {
            blockState.setBottom(bottom);

            return this;
        }

        @Override
        public int candles() {
            return blockState.getCandles();
        }

        @Override
        public BlockStateFactory.Builder candles(int candles) {
            blockState.setCandles(candles);

            return this;
        }

        @Override
        public int charges() {
            return blockState.getCharges();
        }

        @Override
        public BlockStateFactory.Builder charges(int charges) {
            blockState.setCharges(charges);

            return this;
        }

        @Override
        public boolean conditional() {
            return blockState.isConditional();
        }

        @Override
        public BlockStateFactory.Builder conditional(boolean conditional) {
            blockState.setConditional(conditional);

            return this;
        }

        @Override
        public int delay() {
            return blockState.getDelay();
        }

        @Override
        public BlockStateFactory.Builder delay(int delay) {
            blockState.setDelay(delay);

            return this;
        }

        @Override
        public boolean disarmed() {
            return blockState.isDisarmed();
        }

        @Override
        public BlockStateFactory.Builder disarmed(boolean disarmed) {
            blockState.setDisarmed(disarmed);

            return this;
        }

        @Override
        public int distance() {
            return blockState.getDistance();
        }

        @Override
        public BlockStateFactory.Builder distance(int distance) {
            blockState.setDistance(distance);

            return this;
        }

        @Override
        public boolean down() {
            return blockState.isDown();
        }

        @Override
        public BlockStateFactory.Builder down(boolean down) {
            blockState.setDown(down);

            return this;
        }

        @Override
        public boolean drag() {
            return blockState.isDrag();
        }

        @Override
        public BlockStateFactory.Builder drag(boolean drag) {
            blockState.setDrag(drag);

            return this;
        }

        @Override
        public int eggs() {
            return blockState.getEggs();
        }

        @Override
        public BlockStateFactory.Builder eggs(int eggs) {
            blockState.setEggs(eggs);

            return this;
        }

        @Override
        public boolean enabled() {
            return blockState.isEnabled();
        }

        @Override
        public BlockStateFactory.Builder enabled(boolean enabled) {
            blockState.setEnabled(enabled);

            return this;
        }

        @Override
        public boolean extended() {
            return blockState.isExtended();
        }

        @Override
        public BlockStateFactory.Builder extended(boolean extended) {
            blockState.setExtended(extended);

            return this;
        }

        @Override
        public boolean eye() {
            return blockState.isEye();
        }

        @Override
        public BlockStateFactory.Builder eye(boolean eye) {
            blockState.setEye(eye);

            return this;
        }

        @Override
        public Face face() {
            return blockState.getFace();
        }

        @Override
        public BlockStateFactory.Builder face(@NotNull Face face) {
            blockState.setFace(checkNotNull(face, "face"));

            return this;
        }

        @Override
        public BlockFace facing() {
            return blockState.getFacing();
        }

        @Override
        public BlockStateFactory.Builder facing(@NotNull BlockFace facing) {
            blockState.setFacing(checkNotNull(facing, "facing"));

            return this;
        }

        @Override
        public Half half() {
            return blockState.getHalf();
        }

        @Override
        public BlockStateFactory.Builder half(@NotNull Half half) {
            blockState.setHalf(checkNotNull(half, "half"));

            return this;
        }

        @Override
        public boolean hanging() {
            return blockState.isHanging();
        }

        @Override
        public BlockStateFactory.Builder hanging(boolean hanging) {
            blockState.setHanging(hanging);

            return this;
        }

        @Override
        public boolean hasBook() {
            return blockState.isHasBook();
        }

        @Override
        public BlockStateFactory.Builder hasBook(boolean hasBook) {
            blockState.setHasBook(hasBook);

            return this;
        }

        @Override
        public boolean hasBottle0() {
            return blockState.isHasBottle0();
        }

        @Override
        public BlockStateFactory.Builder hasBottle0(boolean hasBottle0) {
            blockState.setHasBottle0(hasBottle0);

            return this;
        }

        @Override
        public boolean hasBottle1() {
            return blockState.isHasBottle1();
        }

        @Override
        public BlockStateFactory.Builder hasBottle1(boolean hasBottle1) {
            blockState.setHasBottle1(hasBottle1);

            return this;
        }

        @Override
        public boolean hasBottle2() {
            return blockState.isHasBottle2();
        }

        @Override
        public BlockStateFactory.Builder hasBottle2(boolean hasBottle2) {
            blockState.setHasBottle2(hasBottle2);

            return this;
        }

        @Override
        public boolean hasRecord() {
            return blockState.isHasRecord();
        }

        @Override
        public BlockStateFactory.Builder hasRecord(boolean hasRecord) {
            blockState.setHasRecord(hasRecord);

            return this;
        }

        @Override
        public int hatch() {
            return blockState.getHatch();
        }

        @Override
        public BlockStateFactory.Builder hatch(int hatch) {
            blockState.setHatch(hatch);

            return this;
        }

        @Override
        public Hinge hinge() {
            return blockState.getHinge();
        }

        @Override
        public BlockStateFactory.Builder hinge(@NotNull Hinge hinge) {
            blockState.setHinge(checkNotNull(hinge, "hinge"));

            return this;
        }

        @Override
        public int honeyLevel() {
            return blockState.getHoneyLevel();
        }

        @Override
        public BlockStateFactory.Builder honeyLevel(int honeyLevel) {
            blockState.setHoneyLevel(honeyLevel);

            return this;
        }

        @Override
        public boolean inWall() {
            return blockState.isInWall();
        }

        @Override
        public BlockStateFactory.Builder inWall(boolean inWall) {
            blockState.setInWall(inWall);

            return this;
        }

        @Override
        public Instrument instrument() {
            return blockState.getInstrument();
        }

        @Override
        public BlockStateFactory.Builder instrument(@NotNull Instrument instrument) {
            blockState.setInstrument(checkNotNull(instrument, "instrument"));

            return this;
        }

        @Override
        public boolean inverted() {
            return blockState.isInverted();
        }

        @Override
        public BlockStateFactory.Builder inverted(boolean inverted) {
            blockState.setInverted(inverted);

            return this;
        }

        @Override
        public int layers() {
            return blockState.getLayers();
        }

        @Override
        public BlockStateFactory.Builder layers(int layers) {
            blockState.setLayers(layers);

            return this;
        }

        @Override
        public Leaves leaves() {
            return blockState.getLeaves();
        }

        @Override
        public BlockStateFactory.Builder leaves(Leaves leaves) {
            blockState.setLeaves(leaves);

            return this;
        }

        @Override
        public int level() {
            return blockState.getLevel();
        }

        @Override
        public BlockStateFactory.Builder level(int level) {
            blockState.setLevel(level);

            return this;
        }

        @Override
        public boolean lit() {
            return blockState.isLit();
        }

        @Override
        public BlockStateFactory.Builder lit(boolean lit) {
            blockState.setLit(lit);

            return this;
        }

        @Override
        public boolean locked() {
            return blockState.isLocked();
        }

        @Override
        public BlockStateFactory.Builder locked(boolean locked) {
            blockState.setLocked(locked);

            return this;
        }

        @Override
        public Mode mode() {
            return blockState.getMode();
        }

        @Override
        public BlockStateFactory.Builder mode(@NotNull Mode mode) {
            blockState.setMode(checkNotNull(mode, "mode"));

            return this;
        }

        @Override
        public int moisture() {
            return blockState.getMoisture();
        }

        @Override
        public BlockStateFactory.Builder moisture(int moisture) {
            blockState.setMoisture(moisture);

            return this;
        }

        @Override
        public North north() {
            return blockState.getNorth();
        }

        @Override
        public BlockStateFactory.Builder north(@NotNull North north) {
            blockState.setNorth(checkNotNull(north, "north"));

            return this;
        }

        @Override
        public int note() {
            return blockState.getNote();
        }

        @Override
        public BlockStateFactory.Builder note(int note) {
            blockState.setNote(note);

            return this;
        }

        @Override
        public boolean occupied() {
            return blockState.isOccupied();
        }

        @Override
        public BlockStateFactory.Builder occupied(boolean occupied) {
            blockState.setOccupied(occupied);

            return this;
        }

        @Override
        public boolean shrieking() {
            return blockState.isShrieking();
        }

        @Override
        public BlockStateFactory.Builder shrieking(boolean shrieking) {
            blockState.setShrieking(shrieking);

            return this;
        }

        @Override
        public boolean canSummon() {
            return blockState.isCanSummon();
        }

        @Override
        public BlockStateFactory.Builder canSummon(boolean canSummon) {
            blockState.setCanSummon(canSummon);

            return this;
        }

        @Override
        public boolean open() {
            return blockState.isOpen();
        }

        @Override
        public BlockStateFactory.Builder open(boolean open) {
            blockState.setOpen(open);

            return this;
        }

        @Override
        public Orientation orientation() {
            return blockState.getOrientation();
        }

        @Override
        public BlockStateFactory.Builder orientation(Orientation orientation) {
            blockState.setOrientation(checkNotNull(orientation, "orientation"));

            return this;
        }

        @Override
        public Part part() {
            return blockState.getPart();
        }

        @Override
        public BlockStateFactory.Builder part(Part part) {
            blockState.setPart(checkNotNull(part, "part"));

            return this;
        }

        @Override
        public boolean persistent() {
            return blockState.isPersistent();
        }

        @Override
        public BlockStateFactory.Builder persistent(boolean persistent) {
            blockState.setPersistent(persistent);

            return this;
        }

        @Override
        public int pickles() {
            return blockState.getPickles();
        }

        @Override
        public BlockStateFactory.Builder pickles(int pickles) {
            blockState.setPickles(pickles);

            return this;
        }

        @Override
        public int power() {
            return blockState.getPower();
        }

        @Override
        public BlockStateFactory.Builder power(int power) {
            blockState.setPower(power);

            return this;
        }

        @Override
        public boolean powered() {
            return blockState.isPowered();
        }

        @Override
        public BlockStateFactory.Builder powered(boolean powered) {
            blockState.setPowered(powered);

            return this;
        }

        @Override
        public int rotation() {
            return blockState.getRotation();
        }

        @Override
        public BlockStateFactory.Builder rotation(int rotation) {
            blockState.setRotation(rotation);

            return this;
        }

        @Override
        public SculkSensorPhase sculkSensorPhase() {
            return blockState.getSculkSensorPhase();
        }

        @Override
        public BlockStateFactory.Builder sculkSensorPhase(SculkSensorPhase sculkSensorPhase) {
            blockState.setSculkSensorPhase(checkNotNull(sculkSensorPhase, "sculkSensorPhase"));

            return this;
        }

        @Override
        public Shape shape() {
            return blockState.getShape();
        }

        @Override
        public BlockStateFactory.Builder shape(Shape shape) {
            blockState.setShape(checkNotNull(shape, "shape"));

            return this;
        }

        @Override
        public boolean short_() {
            return blockState.isShort();
        }

        @Override
        public BlockStateFactory.Builder short_(boolean short_) {
            blockState.setShort(short_);

            return this;
        }

        @Override
        public boolean signalFire() {
            return blockState.isSignalFire();
        }

        @Override
        public BlockStateFactory.Builder signalFire(boolean signalFire) {
            blockState.setSignalFire(signalFire);

            return this;
        }

        @Override
        public boolean slotZeroOccupied() {
            return blockState.isSlotZeroOccupied();
        }

        @Override
        public BlockStateFactory.Builder slotZeroOccupied(boolean slotZeroOccupied) {
            blockState.setSlotZeroOccupied(slotZeroOccupied);

            return this;
        }

        @Override
        public boolean slotOneOccupied() {
            return blockState.isSlotOneOccupied();
        }

        @Override
        public BlockStateFactory.Builder slotOneOccupied(boolean slotOneOccupied) {
            blockState.setSlotOneOccupied(slotOneOccupied);

            return this;
        }

        @Override
        public boolean slotTwoOccupied() {
            return blockState.isSlotTwoOccupied();
        }

        @Override
        public BlockStateFactory.Builder slotTwoOccupied(boolean slotTwoOccupied) {
            blockState.setSlotTwoOccupied(slotTwoOccupied);

            return this;
        }

        @Override
        public boolean slotThreeOccupied() {
            return blockState.isSlotThreeOccupied();
        }

        @Override
        public BlockStateFactory.Builder slotThreeOccupied(boolean slotThreeOccupied) {
            blockState.setSlotThreeOccupied(slotThreeOccupied);

            return this;
        }

        @Override
        public boolean slotFourOccupied() {
            return blockState.isSlotFourOccupied();
        }

        @Override
        public BlockStateFactory.Builder slotFourOccupied(boolean slotFourOccupied) {
            blockState.setSlotFourOccupied(slotFourOccupied);

            return this;
        }

        @Override
        public boolean slotFiveOccupied() {
            return blockState.isSlotFiveOccupied();
        }

        @Override
        public BlockStateFactory.Builder slotFiveOccupied(boolean slotFiveOccupied) {
            blockState.setSlotFiveOccupied(slotFiveOccupied);

            return this;
        }

        @Override
        public boolean snowy() {
            return blockState.isSnowy();
        }

        @Override
        public BlockStateFactory.Builder snowy(boolean snowy) {
            blockState.setSnowy(snowy);

            return this;
        }

        @Override
        public int stage() {
            return blockState.getStage();
        }

        @Override
        public BlockStateFactory.Builder stage(int stage) {
            blockState.setStage(stage);

            return this;
        }

        @Override
        public South south() {
            return blockState.getSouth();
        }

        @Override
        public BlockStateFactory.Builder south(South south) {
            blockState.setSouth(checkNotNull(south, "south"));

            return this;
        }

        @Override
        public Thickness thickness() {
            return blockState.getThickness();
        }

        @Override
        public BlockStateFactory.Builder thickness(Thickness thickness) {
            blockState.setThickness(checkNotNull(thickness, "thickness"));

            return this;
        }

        @Override
        public Tilt tilt() {
            return blockState.getTilt();
        }

        @Override
        public BlockStateFactory.Builder tilt(Tilt tilt) {
            blockState.setTilt(checkNotNull(tilt, "tilt"));

            return this;
        }

        @Override
        public boolean triggered() {
            return blockState.isTriggered();
        }

        @Override
        public BlockStateFactory.Builder triggered(boolean triggered) {
            blockState.setTriggered(triggered);

            return this;
        }

        @Override
        public Type typeData() {
            return blockState.getTypeData();
        }

        @Override
        public BlockStateFactory.Builder typeData(Type type) {
            blockState.setTypeData(checkNotNull(type, "type"));

            return this;
        }

        @Override
        public boolean unstable() {
            return blockState.isUnstable();
        }

        @Override
        public BlockStateFactory.Builder unstable(boolean unstable) {
            blockState.setUnstable(unstable);

            return this;
        }

        @Override
        public boolean up() {
            return blockState.isUp();
        }

        @Override
        public BlockStateFactory.Builder up(boolean up) {
            blockState.setUp(up);

            return this;
        }

        @Override
        public VerticalDirection verticalDirection() {
            return blockState.getVerticalDirection();
        }

        @Override
        public BlockStateFactory.Builder verticalDirection(VerticalDirection verticalDirection) {
            blockState.setVerticalDirection(checkNotNull(verticalDirection, "verticalDirection"));

            return this;
        }

        @Override
        public boolean waterlogged() {
            return blockState.isWaterlogged();
        }

        @Override
        public BlockStateFactory.Builder waterlogged(boolean waterlogged) {
            blockState.setWaterlogged(waterlogged);

            return this;
        }

        @Override
        public East east() {
            return blockState.getEast();
        }

        @Override
        public BlockStateFactory.Builder east(East east) {
            blockState.setEast(checkNotNull(east, "east"));

            return this;
        }

        @Override
        public West west() {
            return blockState.getWest();
        }

        @Override
        public BlockStateFactory.Builder west(West west) {
            blockState.setWest(checkNotNull(west, "west"));

            return this;
        }

        @Override
        public Bloom bloom() {
            return blockState.getBloom();
        }

        @Override
        public BlockStateFactory.Builder bloom(Bloom bloom) {
            blockState.setBloom(checkNotNull(bloom, "bloom"));

            return this;
        }

        @Override
        public boolean cracked() {
            return blockState.isCracked();
        }

        @Override
        public BlockStateFactory.Builder cracked(boolean cracked) {
            blockState.setCracked(cracked);

            return this;
        }

        @Override
        public boolean crafting() {
            return blockState.isCrafting();
        }

        @Override
        public BlockStateFactory.Builder crafting(boolean crafting) {
            blockState.setCrafting(crafting);

            return this;
        }

        @Override
        public TrialSpawnerState trialSpawnerState() {
            return blockState.getTrialSpawnerState();
        }

        @Override
        public BlockStateFactory.Builder trialSpawnerState(TrialSpawnerState trialSpawnerState) {
            blockState.setTrialSpawnerState(checkNotNull(trialSpawnerState, "trialSpawnerState"));

            return this;
        }

        @Override
        public WrappedBlockState build() {
            return blockState.clone();
        }
    }
}
