package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.Useless;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.PacketLivingEntity;

public interface PacketMob<Impl extends PacketMob<Impl>> extends PacketLivingEntity<Impl> {

    int MOB_BIT_MASK_INDEX = 15;

    byte NO_AI_BIT = 0x01, LEFT_HANDED_BIT = 0x02, AGGRESSIVE_BIT = 0x04;

    @Useless // packet entities don't have an AI as far as I know
    boolean noAI();

    @Useless // packet entities don't have an AI as far as I know
    void noAI(boolean noAI);

    boolean leftHanded();

    void leftHanded(boolean leftHanded);

    boolean aggressive();

    void aggressive(boolean aggressive);
}
