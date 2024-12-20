package dev.slne.surf.surfapi.core.api.util.blockstate

import com.github.retrooper.packetevents.protocol.world.BlockFace
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.enums.*
import com.google.common.base.Preconditions
import javax.annotation.ParametersAreNonnullByDefault

class BlockStateFactoryImpl {
    @ParametersAreNonnullByDefault
    internal data class BuilderImpl(val blockState: WrappedBlockState) : BlockStateFactory.Builder {
        override fun age(): Int {
            return blockState.age
        }

        override fun age(age: Int): BlockStateFactory.Builder {
            blockState.setAge(age)

            return this
        }

        override fun attached(): Boolean {
            return blockState.isAttached
        }

        override fun attached(attached: Boolean): BlockStateFactory.Builder {
            blockState.setAttached(attached)

            return this
        }

        override fun attachment(): Attachment {
            return blockState.attachment
        }

        override fun attachment(attachment: Attachment): BlockStateFactory.Builder {
            blockState.setAttachment(
                Preconditions.checkNotNull<Attachment?>(
                    attachment,
                    "attachment"
                )
            )

            return this
        }

        override fun axis(): Axis {
            return blockState.axis
        }

        override fun axis(axis: Axis): BlockStateFactory.Builder {
            blockState.setAxis(Preconditions.checkNotNull<Axis?>(axis, "axis"))

            return this
        }

        override fun berries(): Boolean {
            return blockState.isBerries
        }

        override fun berries(berries: Boolean): BlockStateFactory.Builder {
            blockState.setBerries(berries)

            return this
        }

        override fun bites(): Int {
            return blockState.bites
        }

        override fun bites(bites: Int): BlockStateFactory.Builder {
            blockState.setBites(bites)

            return this
        }

        override fun bottom(): Boolean {
            return blockState.isBottom
        }

        override fun bottom(bottom: Boolean): BlockStateFactory.Builder {
            blockState.setBottom(bottom)

            return this
        }

        override fun candles(): Int {
            return blockState.candles
        }

        override fun candles(candles: Int): BlockStateFactory.Builder {
            blockState.setCandles(candles)

            return this
        }

        override fun charges(): Int {
            return blockState.charges
        }

        override fun charges(charges: Int): BlockStateFactory.Builder {
            blockState.setCharges(charges)

            return this
        }

        override fun conditional(): Boolean {
            return blockState.isConditional
        }

        override fun conditional(conditional: Boolean): BlockStateFactory.Builder {
            blockState.setConditional(conditional)

            return this
        }

        override fun delay(): Int {
            return blockState.delay
        }

        override fun delay(delay: Int): BlockStateFactory.Builder {
            blockState.setDelay(delay)

            return this
        }

        override fun disarmed(): Boolean {
            return blockState.isDisarmed
        }

        override fun disarmed(disarmed: Boolean): BlockStateFactory.Builder {
            blockState.setDisarmed(disarmed)

            return this
        }

        override fun distance(): Int {
            return blockState.distance
        }

        override fun distance(distance: Int): BlockStateFactory.Builder {
            blockState.setDistance(distance)

            return this
        }

        override fun down(): Boolean {
            return blockState.isDown
        }

        override fun down(down: Boolean): BlockStateFactory.Builder {
            blockState.setDown(down)

            return this
        }

        override fun drag(): Boolean {
            return blockState.isDrag
        }

        override fun drag(drag: Boolean): BlockStateFactory.Builder {
            blockState.setDrag(drag)

            return this
        }

        override fun eggs(): Int {
            return blockState.eggs
        }

        override fun eggs(eggs: Int): BlockStateFactory.Builder {
            blockState.setEggs(eggs)

            return this
        }

        override fun enabled(): Boolean {
            return blockState.isEnabled
        }

        override fun enabled(enabled: Boolean): BlockStateFactory.Builder {
            blockState.setEnabled(enabled)

            return this
        }

        override fun extended(): Boolean {
            return blockState.isExtended
        }

        override fun extended(extended: Boolean): BlockStateFactory.Builder {
            blockState.setExtended(extended)

            return this
        }

        override fun eye(): Boolean {
            return blockState.isEye
        }

        override fun eye(eye: Boolean): BlockStateFactory.Builder {
            blockState.setEye(eye)

            return this
        }

        override fun face(): Face {
            return blockState.face
        }

        override fun face(face: Face): BlockStateFactory.Builder {
            blockState.setFace(Preconditions.checkNotNull<Face?>(face, "face"))

            return this
        }

        override fun facing(): BlockFace {
            return blockState.facing
        }

        override fun facing(facing: BlockFace): BlockStateFactory.Builder {
            blockState.setFacing(Preconditions.checkNotNull<BlockFace?>(facing, "facing"))

            return this
        }

        override fun half(): Half {
            return blockState.half
        }

        override fun half(half: Half): BlockStateFactory.Builder {
            blockState.setHalf(Preconditions.checkNotNull<Half?>(half, "half"))

            return this
        }

        override fun hanging(): Boolean {
            return blockState.isHanging
        }

        override fun hanging(hanging: Boolean): BlockStateFactory.Builder {
            blockState.setHanging(hanging)

            return this
        }

        override fun hasBook(): Boolean {
            return blockState.isHasBook
        }

        override fun hasBook(hasBook: Boolean): BlockStateFactory.Builder {
            blockState.setHasBook(hasBook)

            return this
        }

        override fun hasBottle0(): Boolean {
            return blockState.isHasBottle0
        }

        override fun hasBottle0(hasBottle0: Boolean): BlockStateFactory.Builder {
            blockState.setHasBottle0(hasBottle0)

            return this
        }

        override fun hasBottle1(): Boolean {
            return blockState.isHasBottle1
        }

        override fun hasBottle1(hasBottle1: Boolean): BlockStateFactory.Builder {
            blockState.setHasBottle1(hasBottle1)

            return this
        }

        override fun hasBottle2(): Boolean {
            return blockState.isHasBottle2
        }

        override fun hasBottle2(hasBottle2: Boolean): BlockStateFactory.Builder {
            blockState.setHasBottle2(hasBottle2)

            return this
        }

        override fun hasRecord(): Boolean {
            return blockState.isHasRecord
        }

        override fun hasRecord(hasRecord: Boolean): BlockStateFactory.Builder {
            blockState.setHasRecord(hasRecord)

            return this
        }

        override fun hatch(): Int {
            return blockState.hatch
        }

        override fun hatch(hatch: Int): BlockStateFactory.Builder {
            blockState.setHatch(hatch)

            return this
        }

        override fun hinge(): Hinge {
            return blockState.hinge
        }

        override fun hinge(hinge: Hinge): BlockStateFactory.Builder {
            blockState.setHinge(Preconditions.checkNotNull<Hinge?>(hinge, "hinge"))

            return this
        }

        override fun honeyLevel(): Int {
            return blockState.honeyLevel
        }

        override fun honeyLevel(honeyLevel: Int): BlockStateFactory.Builder {
            blockState.setHoneyLevel(honeyLevel)

            return this
        }

        override fun inWall(): Boolean {
            return blockState.isInWall
        }

        override fun inWall(inWall: Boolean): BlockStateFactory.Builder {
            blockState.setInWall(inWall)

            return this
        }

        override fun instrument(): Instrument {
            return blockState.instrument
        }

        override fun instrument(instrument: Instrument): BlockStateFactory.Builder {
            blockState.setInstrument(
                Preconditions.checkNotNull<Instrument?>(
                    instrument,
                    "instrument"
                )
            )

            return this
        }

        override fun inverted(): Boolean {
            return blockState.isInverted
        }

        override fun inverted(inverted: Boolean): BlockStateFactory.Builder {
            blockState.setInverted(inverted)

            return this
        }

        override fun layers(): Int {
            return blockState.layers
        }

        override fun layers(layers: Int): BlockStateFactory.Builder {
            blockState.setLayers(layers)

            return this
        }

        override fun leaves(): Leaves {
            return blockState.leaves
        }

        override fun leaves(leaves: Leaves): BlockStateFactory.Builder {
            blockState.setLeaves(leaves)

            return this
        }

        override fun level(): Int {
            return blockState.level
        }

        override fun level(level: Int): BlockStateFactory.Builder {
            blockState.setLevel(level)

            return this
        }

        override fun lit(): Boolean {
            return blockState.isLit
        }

        override fun lit(lit: Boolean): BlockStateFactory.Builder {
            blockState.setLit(lit)

            return this
        }

        override fun locked(): Boolean {
            return blockState.isLocked
        }

        override fun locked(locked: Boolean): BlockStateFactory.Builder {
            blockState.setLocked(locked)

            return this
        }

        override fun mode(): Mode {
            return blockState.mode
        }

        override fun mode(mode: Mode): BlockStateFactory.Builder {
            blockState.setMode(Preconditions.checkNotNull<Mode?>(mode, "mode"))

            return this
        }

        override fun moisture(): Int {
            return blockState.moisture
        }

        override fun moisture(moisture: Int): BlockStateFactory.Builder {
            blockState.setMoisture(moisture)

            return this
        }

        override fun north(): North {
            return blockState.north
        }

        override fun north(north: North): BlockStateFactory.Builder {
            blockState.setNorth(Preconditions.checkNotNull<North?>(north, "north"))

            return this
        }

        override fun note(): Int {
            return blockState.note
        }

        override fun note(note: Int): BlockStateFactory.Builder {
            blockState.setNote(note)

            return this
        }

        override fun occupied(): Boolean {
            return blockState.isOccupied
        }

        override fun occupied(occupied: Boolean): BlockStateFactory.Builder {
            blockState.setOccupied(occupied)

            return this
        }

        override fun shrieking(): Boolean {
            return blockState.isShrieking
        }

        override fun shrieking(shrieking: Boolean): BlockStateFactory.Builder {
            blockState.setShrieking(shrieking)

            return this
        }

        override fun canSummon(): Boolean {
            return blockState.isCanSummon
        }

        override fun canSummon(canSummon: Boolean): BlockStateFactory.Builder {
            blockState.setCanSummon(canSummon)

            return this
        }

        override fun open(): Boolean {
            return blockState.isOpen
        }

        override fun open(open: Boolean): BlockStateFactory.Builder {
            blockState.setOpen(open)

            return this
        }

        override fun orientation(): Orientation {
            return blockState.orientation
        }

        override fun orientation(orientation: Orientation): BlockStateFactory.Builder {
            blockState.setOrientation(
                Preconditions.checkNotNull<Orientation?>(
                    orientation,
                    "orientation"
                )
            )

            return this
        }

        override fun part(): Part {
            return blockState.part
        }

        override fun part(part: Part): BlockStateFactory.Builder {
            blockState.setPart(Preconditions.checkNotNull<Part?>(part, "part"))

            return this
        }

        override fun persistent(): Boolean {
            return blockState.isPersistent
        }

        override fun persistent(persistent: Boolean): BlockStateFactory.Builder {
            blockState.setPersistent(persistent)

            return this
        }

        override fun pickles(): Int {
            return blockState.pickles
        }

        override fun pickles(pickles: Int): BlockStateFactory.Builder {
            blockState.setPickles(pickles)

            return this
        }

        override fun power(): Int {
            return blockState.power
        }

        override fun power(power: Int): BlockStateFactory.Builder {
            blockState.setPower(power)

            return this
        }

        override fun powered(): Boolean {
            return blockState.isPowered
        }

        override fun powered(powered: Boolean): BlockStateFactory.Builder {
            blockState.setPowered(powered)

            return this
        }

        override fun rotation(): Int {
            return blockState.rotation
        }

        override fun rotation(rotation: Int): BlockStateFactory.Builder {
            blockState.setRotation(rotation)

            return this
        }

        override fun sculkSensorPhase(): SculkSensorPhase {
            return blockState.sculkSensorPhase
        }

        override fun sculkSensorPhase(sculkSensorPhase: SculkSensorPhase): BlockStateFactory.Builder {
            blockState.setSculkSensorPhase(sculkSensorPhase)

            return this
        }

        override fun shape(): Shape {
            return blockState.shape
        }

        override fun shape(shape: Shape?): BlockStateFactory.Builder {
            blockState.setShape(Preconditions.checkNotNull<Shape?>(shape, "shape"))

            return this
        }

        override fun short(): Boolean {
            return blockState.isShort
        }

        override fun short(short: Boolean): BlockStateFactory.Builder {
            blockState.setShort(short)

            return this
        }

        override fun signalFire(): Boolean {
            return blockState.isSignalFire
        }

        override fun signalFire(signalFire: Boolean): BlockStateFactory.Builder {
            blockState.setSignalFire(signalFire)

            return this
        }

        override fun slotZeroOccupied(): Boolean {
            return blockState.isSlotZeroOccupied
        }

        override fun slotZeroOccupied(slotZeroOccupied: Boolean): BlockStateFactory.Builder {
            blockState.setSlotZeroOccupied(slotZeroOccupied)

            return this
        }

        override fun slotOneOccupied(): Boolean {
            return blockState.isSlotOneOccupied
        }

        override fun slotOneOccupied(slotOneOccupied: Boolean): BlockStateFactory.Builder {
            blockState.setSlotOneOccupied(slotOneOccupied)

            return this
        }

        override fun slotTwoOccupied(): Boolean {
            return blockState.isSlotTwoOccupied
        }

        override fun slotTwoOccupied(slotTwoOccupied: Boolean): BlockStateFactory.Builder {
            blockState.setSlotTwoOccupied(slotTwoOccupied)

            return this
        }

        override fun slotThreeOccupied(): Boolean {
            return blockState.isSlotThreeOccupied
        }

        override fun slotThreeOccupied(slotThreeOccupied: Boolean): BlockStateFactory.Builder {
            blockState.setSlotThreeOccupied(slotThreeOccupied)

            return this
        }

        override fun slotFourOccupied(): Boolean {
            return blockState.isSlotFourOccupied
        }

        override fun slotFourOccupied(slotFourOccupied: Boolean): BlockStateFactory.Builder {
            blockState.setSlotFourOccupied(slotFourOccupied)

            return this
        }

        override fun slotFiveOccupied(): Boolean {
            return blockState.isSlotFiveOccupied
        }

        override fun slotFiveOccupied(slotFiveOccupied: Boolean): BlockStateFactory.Builder {
            blockState.setSlotFiveOccupied(slotFiveOccupied)

            return this
        }

        override fun snowy(): Boolean {
            return blockState.isSnowy
        }

        override fun snowy(snowy: Boolean): BlockStateFactory.Builder {
            blockState.setSnowy(snowy)

            return this
        }

        override fun stage(): Int {
            return blockState.stage
        }

        override fun stage(stage: Int): BlockStateFactory.Builder {
            blockState.setStage(stage)

            return this
        }

        override fun south(): South {
            return blockState.south
        }

        override fun south(south: South): BlockStateFactory.Builder {
            blockState.setSouth(Preconditions.checkNotNull<South?>(south, "south"))

            return this
        }

        override fun thickness(): Thickness {
            return blockState.thickness
        }

        override fun thickness(thickness: Thickness): BlockStateFactory.Builder {
            blockState.setThickness(
                Preconditions.checkNotNull<Thickness?>(
                    thickness,
                    "thickness"
                )
            )

            return this
        }

        override fun tilt(): Tilt {
            return blockState.tilt
        }

        override fun tilt(tilt: Tilt): BlockStateFactory.Builder {
            blockState.setTilt(Preconditions.checkNotNull<Tilt?>(tilt, "tilt"))

            return this
        }

        override fun triggered(): Boolean {
            return blockState.isTriggered
        }

        override fun triggered(triggered: Boolean): BlockStateFactory.Builder {
            blockState.setTriggered(triggered)

            return this
        }

        override fun typeData(): Type {
            return blockState.typeData
        }

        override fun typeData(type: Type): BlockStateFactory.Builder {
            blockState.setTypeData(Preconditions.checkNotNull<Type?>(type, "type"))

            return this
        }

        override fun unstable(): Boolean {
            return blockState.isUnstable
        }

        override fun unstable(unstable: Boolean): BlockStateFactory.Builder {
            blockState.setUnstable(unstable)

            return this
        }

        override fun up(): Boolean {
            return blockState.isUp
        }

        override fun up(up: Boolean): BlockStateFactory.Builder {
            blockState.setUp(up)

            return this
        }

        override fun verticalDirection(): VerticalDirection {
            return blockState.verticalDirection
        }

        override fun verticalDirection(verticalDirection: VerticalDirection): BlockStateFactory.Builder {
            blockState.setVerticalDirection(verticalDirection)


            return this
        }

        override fun waterlogged(): Boolean {
            return blockState.isWaterlogged
        }

        override fun waterlogged(waterlogged: Boolean): BlockStateFactory.Builder {
            blockState.setWaterlogged(waterlogged)

            return this
        }

        override fun east(): East {
            return blockState.east
        }

        override fun east(east: East): BlockStateFactory.Builder {
            blockState.setEast(east)

            return this
        }

        override fun west(): West {
            return blockState.west
        }

        override fun west(west: West): BlockStateFactory.Builder {
            blockState.setWest(Preconditions.checkNotNull<West?>(west, "west"))

            return this
        }

        override fun bloom(): Bloom {
            return blockState.bloom
        }

        override fun bloom(bloom: Bloom): BlockStateFactory.Builder {
            blockState.setBloom(Preconditions.checkNotNull<Bloom?>(bloom, "bloom"))

            return this
        }

        override fun cracked(): Boolean {
            return blockState.isCracked
        }

        override fun cracked(cracked: Boolean): BlockStateFactory.Builder {
            blockState.setCracked(cracked)

            return this
        }

        override fun crafting(): Boolean {
            return blockState.isCrafting
        }

        override fun crafting(crafting: Boolean): BlockStateFactory.Builder {
            blockState.setCrafting(crafting)

            return this
        }

        override fun trialSpawnerState(): TrialSpawnerState {
            return blockState.trialSpawnerState
        }

        override fun trialSpawnerState(trialSpawnerState: TrialSpawnerState): BlockStateFactory.Builder {
            blockState.setTrialSpawnerState(
                trialSpawnerState
            )


            return this
        }

        override fun build(): WrappedBlockState {
            return blockState.clone()
        }
    }
}
