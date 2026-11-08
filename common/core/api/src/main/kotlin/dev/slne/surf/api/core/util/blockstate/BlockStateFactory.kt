package dev.slne.surf.api.core.util.blockstate

import com.github.retrooper.packetevents.protocol.world.BlockFace
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.enums.*
import com.github.retrooper.packetevents.protocol.world.states.type.StateType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Deprecated("Not longer maintained.")
interface BlockStateFactory {
    @Suppress("unused")
    interface Builder {
        fun age(): Int
        fun age(age: Int): Builder

        fun attached(): Boolean
        fun attached(attached: Boolean): Builder

        fun attachment(): Attachment
        fun attachment(attachment: Attachment): Builder

        fun axis(): Axis
        fun axis(axis: Axis): Builder

        fun berries(): Boolean
        fun berries(berries: Boolean): Builder

        fun bites(): Int
        fun bites(bites: Int): Builder

        fun bottom(): Boolean
        fun bottom(bottom: Boolean): Builder

        fun candles(): Int
        fun candles(candles: Int): Builder

        fun charges(): Int
        fun charges(charges: Int): Builder

        fun conditional(): Boolean
        fun conditional(conditional: Boolean): Builder

        fun delay(): Int
        fun delay(delay: Int): Builder

        fun disarmed(): Boolean
        fun disarmed(disarmed: Boolean): Builder

        fun distance(): Int
        fun distance(distance: Int): Builder

        fun down(): Boolean
        fun down(down: Boolean): Builder

        fun drag(): Boolean
        fun drag(drag: Boolean): Builder

        fun eggs(): Int
        fun eggs(eggs: Int): Builder

        fun enabled(): Boolean
        fun enabled(enabled: Boolean): Builder

        fun extended(): Boolean
        fun extended(extended: Boolean): Builder

        fun eye(): Boolean
        fun eye(eye: Boolean): Builder

        fun face(): Face
        fun face(face: Face): Builder

        fun facing(): BlockFace
        fun facing(facing: BlockFace): Builder

        fun half(): Half
        fun half(half: Half): Builder

        fun hanging(): Boolean
        fun hanging(hanging: Boolean): Builder

        fun hasBook(): Boolean
        fun hasBook(hasBook: Boolean): Builder

        fun hasBottle0(): Boolean
        fun hasBottle0(hasBottle0: Boolean): Builder

        fun hasBottle1(): Boolean
        fun hasBottle1(hasBottle1: Boolean): Builder

        fun hasBottle2(): Boolean
        fun hasBottle2(hasBottle2: Boolean): Builder

        fun hasRecord(): Boolean
        fun hasRecord(hasRecord: Boolean): Builder

        fun hatch(): Int
        fun hatch(hatch: Int): Builder

        fun hinge(): Hinge
        fun hinge(hinge: Hinge): Builder

        fun honeyLevel(): Int
        fun honeyLevel(honeyLevel: Int): Builder

        fun inWall(): Boolean
        fun inWall(inWall: Boolean): Builder

        fun instrument(): Instrument
        fun instrument(instrument: Instrument): Builder

        fun inverted(): Boolean
        fun inverted(inverted: Boolean): Builder

        fun layers(): Int
        fun layers(layers: Int): Builder

        fun leaves(): Leaves
        fun leaves(leaves: Leaves): Builder

        fun level(): Int
        fun level(level: Int): Builder

        fun lit(): Boolean
        fun lit(lit: Boolean): Builder

        fun locked(): Boolean
        fun locked(locked: Boolean): Builder

        fun mode(): Mode
        fun mode(mode: Mode): Builder

        fun moisture(): Int
        fun moisture(moisture: Int): Builder

        fun north(): North
        fun north(north: North): Builder

        fun note(): Int
        fun note(note: Int): Builder

        fun occupied(): Boolean
        fun occupied(occupied: Boolean): Builder

        fun shrieking(): Boolean
        fun shrieking(shrieking: Boolean): Builder

        fun canSummon(): Boolean
        fun canSummon(canSummon: Boolean): Builder

        fun open(): Boolean
        fun open(open: Boolean): Builder

        fun orientation(): Orientation
        fun orientation(orientation: Orientation): Builder

        fun part(): Part
        fun part(part: Part): Builder

        fun persistent(): Boolean
        fun persistent(persistent: Boolean): Builder

        fun pickles(): Int
        fun pickles(pickles: Int): Builder

        fun power(): Int
        fun power(power: Int): Builder

        fun powered(): Boolean
        fun powered(powered: Boolean): Builder

        fun rotation(): Int
        fun rotation(rotation: Int): Builder

        fun sculkSensorPhase(): SculkSensorPhase
        fun sculkSensorPhase(sculkSensorPhase: SculkSensorPhase): Builder

        fun shape(): Shape
        fun shape(shape: Shape?): Builder

        fun short(): Boolean
        fun short(short: Boolean): Builder

        fun signalFire(): Boolean
        fun signalFire(signalFire: Boolean): Builder

        fun slotZeroOccupied(): Boolean
        fun slotZeroOccupied(slotZeroOccupied: Boolean): Builder

        fun slotOneOccupied(): Boolean
        fun slotOneOccupied(slotOneOccupied: Boolean): Builder

        fun slotTwoOccupied(): Boolean
        fun slotTwoOccupied(slotTwoOccupied: Boolean): Builder

        fun slotThreeOccupied(): Boolean
        fun slotThreeOccupied(slotThreeOccupied: Boolean): Builder

        fun slotFourOccupied(): Boolean
        fun slotFourOccupied(slotFourOccupied: Boolean): Builder

        fun slotFiveOccupied(): Boolean
        fun slotFiveOccupied(slotFiveOccupied: Boolean): Builder

        fun snowy(): Boolean
        fun snowy(snowy: Boolean): Builder

        fun stage(): Int
        fun stage(stage: Int): Builder

        fun south(): South
        fun south(south: South): Builder

        fun thickness(): Thickness
        fun thickness(thickness: Thickness): Builder

        fun tilt(): Tilt
        fun tilt(tilt: Tilt): Builder

        fun triggered(): Boolean
        fun triggered(triggered: Boolean): Builder

        fun typeData(): Type
        fun typeData(type: Type): Builder

        fun unstable(): Boolean
        fun unstable(unstable: Boolean): Builder

        fun up(): Boolean
        fun up(up: Boolean): Builder

        fun verticalDirection(): VerticalDirection
        fun verticalDirection(verticalDirection: VerticalDirection): Builder

        fun waterlogged(): Boolean
        fun waterlogged(waterlogged: Boolean): Builder

        fun east(): East
        fun east(east: East): Builder

        fun west(): West
        fun west(west: West): Builder

        fun bloom(): Bloom
        fun bloom(bloom: Bloom): Builder

        fun cracked(): Boolean
        fun cracked(cracked: Boolean): Builder

        fun crafting(): Boolean
        fun crafting(crafting: Boolean): Builder

        fun trialSpawnerState(): TrialSpawnerState
        fun trialSpawnerState(trialSpawnerState: TrialSpawnerState): Builder

        fun build(): WrappedBlockState
    }

    @OptIn(ExperimentalContracts::class)
    companion object {
        @Suppress("DEPRECATION")
        @JvmStatic
        fun builder(stateType: StateType): Builder =
            BlockStateFactoryImpl.BuilderImpl(stateType.createBlockState().clone())

        @Suppress("DEPRECATION")
        @JvmStatic
        fun builder(wrappedBlockState: WrappedBlockState): Builder =
            BlockStateFactoryImpl.BuilderImpl(wrappedBlockState.clone())

        fun build(stateType: StateType, block: Builder.() -> Unit): WrappedBlockState {
            contract {
                callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            }

            return builder(stateType).apply(block).build()
        }

        fun build(
            wrappedBlockState: WrappedBlockState,
            block: Builder.() -> Unit
        ): WrappedBlockState {
            contract {
                callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            }

            return builder(wrappedBlockState).apply(block).build()
        }

        @JvmStatic
        fun of(stateType: StateType): WrappedBlockState = stateType.createBlockState().clone()
    }
}
