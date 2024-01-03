package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.Useless;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketPanda extends PacketAnimal<PacketPanda>, Spawnable {

    /**
     * The index of the panda's flags in the metadata array.
     */
    int UNHAPPY_INDEX = 17, SNEEZE_INDEX = 18, EATING_INDEX = 19, MAIN_GENE_INDEX = 20, HIDDEN_GENE_INDEX = 21, PANDA_FLAGS_INDEX = 22;

    /**
     * The flags for the panda.
     */
    byte SNEEZING_FLAG = 0x02, ROLLING_FLAG = 0x04, SITTING_FLAG = 0x08, ON_BACK_FLAG = 0x10;

    /**
     * Gets how many ticks the panda will be unhappy for
     *
     * @return The number of ticks the panda will be unhappy for
     */
    int unhappyTicks();

    /**
     * Sets the number of ticks this panda will be unhappy for.
     * <p>
     * This value counts down.
     *
     * @param ticks unhappy ticks
     */
    void unhappyTicks(int ticks);

    /**
     * Gets the current sneeze progress, or how many ticks this panda will sneeze for.
     *
     * @return sneeze progress
     */
    @Useless
    int sneezeTicks();

    /**
     * Sets the sneeze progress in this animation.
     * This value counts up only if {@link #isSneezing()} is true
     *
     * @param ticks sneeze progress
     */
    @Useless
    void sneezeTicks(int ticks);

    /**
     * Gets the current eating progress, or how many ticks this panda has been eating for.
     *
     * @return eating progress
     */
    int eatingTicks();

    /**
     * Sets the eating ticks for this panda.
     * <p>
     *
     * This starts counting up as long as it is greater than 0.
     *
     * @param ticks eating ticks
     */
    void eatingTicks(int ticks);

    /**
     * Gets this Panda's main gene.
     *
     * @return main gene
     */
    Gene mainGene();

    /**
     * Sets this Panda's main gene.
     *
     * @param gene main gene
     */
    void mainGene(@NotNull Gene gene);

    /**
     * Gets this Panda's hidden gene.
     *
     * @return hidden gene
     */
    Gene hiddenGene();

    /**
     * Sets this Panda's hidden gene.
     *
     * @param gene hidden gene
     */
    void hiddenGene(@NotNull Gene gene);

    /**
     * Gets whether the Panda is sneezing
     *
     * @return Whether the Panda is sneezing
     */
    boolean isSneezing();

    /**
     * Sets whether the Panda is sneezing
     *
     * @param sneezing Whether the Panda is sneezing
     */
    void isSneezing(boolean sneezing);

    /**
     * Gets whether the Panda is rolling
     *
     * @return Whether the Panda is rolling
     */
    boolean isRolling();

    /**
     * Sets whether the Panda is rolling
     *
     * @param rolling Whether the Panda is rolling
     */
    void isRolling(boolean rolling);

    /**
     * Gets if this panda is sitting.
     *
     * @return is sitting
     */
    boolean isSitting();

    /**
     * Sets if this panda is currently sitting.
     *
     * @param sitting is currently sitting
     */
    void isSitting(boolean sitting);

    /**
     * Gets whether the Panda is on its back
     *
     * @return Whether the Panda is on its back
     */
    boolean isOnBack();

    /**
     * Sets whether the Panda is on its back
     *
     * @param onBack Whether the Panda is on its back
     */
    void isOnBack(boolean onBack);

    enum Gene {
        NORMAL(false),
        LAZY(false),
        WORRIED(false),
        PLAYFUL(false),
        BROWN(true),
        WEAK(true),
        AGGRESSIVE(false);

        private final boolean recessive;

        Gene(boolean recessive) {
            this.recessive = recessive;
        }

        /**
         * Gets whether this gene is recessive, i.e. required in both parents to
         * propagate to children.
         *
         * @return recessive status
         */
        public boolean isRecessive() {
            return recessive;
        }
    }
}
