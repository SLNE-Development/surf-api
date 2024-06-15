package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.frame.PacketItemFrame;

@CanBeSpawned
public interface PacketGlowItemFrame extends PacketItemFrame<PacketGlowItemFrame>, Spawnable {

}
