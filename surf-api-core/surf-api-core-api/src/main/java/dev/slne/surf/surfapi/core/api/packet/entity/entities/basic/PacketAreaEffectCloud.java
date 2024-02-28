package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.particle.Particle;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketAreaEffectCloud extends PacketEntity<PacketAreaEffectCloud>, Spawnable {

  int RADIUS_INDEX = 8, COLOR_INDEX = 9, SINGLE_POINT_INDEX = 10, PARTICLE_INDEX = 11;

  float radius();

  void radius(float radius);

  TextColor color();

  void color(@NotNull TextColor color);

  boolean singlePoint();

  void singlePoint(boolean singlePoint);

  Particle particle();

  void particle(@NotNull Particle particle);
}
