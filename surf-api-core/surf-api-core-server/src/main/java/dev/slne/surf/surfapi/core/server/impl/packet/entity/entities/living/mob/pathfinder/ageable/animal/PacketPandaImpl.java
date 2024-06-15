package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketPanda;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketPandaImpl extends PacketAnimalImpl<PacketPanda> implements PacketPanda {

  public PacketPandaImpl(UUID uuid) {
    super(uuid, EntityTypes.PANDA);
  }

  @Override
  public int unhappyTicks() {
    return get(UNHAPPY_INDEX, 0);
  }

  @Override
  public void unhappyTicks(int ticks) {
    set(UNHAPPY_INDEX, ticks);
    afterSet();
  }

  @Override
  public int sneezeTicks() {
    return get(SNEEZE_INDEX, 0);
  }

  @Override
  public void sneezeTicks(int ticks) {
    set(SNEEZE_INDEX, ticks);
    afterSet();
  }

  @Override
  public int eatingTicks() {
    return get(EATING_INDEX, 0);
  }

  @Override
  public void eatingTicks(int ticks) {
    set(EATING_INDEX, ticks);
    afterSet();
  }

  @Override
  public Gene mainGene() {
    return Gene.values()[get(MAIN_GENE_INDEX, 0)];
  }

  @Override
  public void mainGene(@NotNull Gene gene) {
    set(MAIN_GENE_INDEX, checkNotNull(gene, "gene").ordinal());
    afterSet();
  }

  @Override
  public Gene hiddenGene() {
    return Gene.values()[get(HIDDEN_GENE_INDEX, 0)];
  }

  @Override
  public void hiddenGene(@NotNull Gene gene) {
    set(HIDDEN_GENE_INDEX, checkNotNull(gene, "gene").ordinal());
    afterSet();
  }

  @Override
  public boolean isSneezing() {
    return getMaskBit(PANDA_FLAGS_INDEX, SNEEZING_FLAG);
  }

  @Override
  public void isSneezing(boolean sneezing) {
    setMaskBit(PANDA_FLAGS_INDEX, SNEEZING_FLAG, sneezing);
    afterSet();
  }

  @Override
  public boolean isRolling() {
    return getMaskBit(PANDA_FLAGS_INDEX, ROLLING_FLAG);
  }

  @Override
  public void isRolling(boolean rolling) {
    setMaskBit(PANDA_FLAGS_INDEX, ROLLING_FLAG, rolling);
    afterSet();
  }

  @Override
  public boolean isSitting() {
    return getMaskBit(PANDA_FLAGS_INDEX, SITTING_FLAG);
  }

  @Override
  public void isSitting(boolean sitting) {
    setMaskBit(PANDA_FLAGS_INDEX, SITTING_FLAG, sitting);
    afterSet();
  }

  @Override
  public boolean isOnBack() {
    return getMaskBit(PANDA_FLAGS_INDEX, ON_BACK_FLAG);
  }

  @Override
  public void isOnBack(boolean onBack) {
    setMaskBit(PANDA_FLAGS_INDEX, ON_BACK_FLAG, onBack);
    afterSet();
  }
}
