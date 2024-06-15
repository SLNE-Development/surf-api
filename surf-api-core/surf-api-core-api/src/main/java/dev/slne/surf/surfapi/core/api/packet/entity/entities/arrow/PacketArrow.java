package dev.slne.surf.surfapi.core.api.packet.entity.entities.arrow;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Shootable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.SpawnVelocity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import java.util.Optional;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

@CanBeSpawned
public interface PacketArrow extends PacketAbstractArrow<PacketArrow>, SpawnVelocity, Shootable,
    Spawnable {

  int COLOR_INDEX = 10;

  Optional<TextColor> color();

  void color(@Nullable TextColor color);
}
