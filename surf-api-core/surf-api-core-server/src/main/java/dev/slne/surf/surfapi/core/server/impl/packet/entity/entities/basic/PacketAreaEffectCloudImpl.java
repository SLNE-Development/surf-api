package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketAreaEffectCloud;
import dev.slne.surf.surfapi.core.api.util.ParticleFactory;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PacketAreaEffectCloudImpl extends PacketEntityImpl<PacketAreaEffectCloud> implements PacketAreaEffectCloud {


    public PacketAreaEffectCloudImpl(UUID uuid) {
        super(uuid, EntityTypes.AREA_EFFECT_CLOUD);
    }

    @Override
    public float radius() {
        return get(RADIUS_INDEX, 0.5f);
    }

    @Override
    public void radius(float radius) {
        set(RADIUS_INDEX, radius);
        afterSet();
    }

    @Override
    public TextColor color() {
        return TextColor.color(get(COLOR_INDEX, 0x000000));
    }

    @Override
    public void color(@NotNull TextColor color) {
        set(COLOR_INDEX, color.value());
        afterSet();
    }

    @Override
    public boolean singlePoint() {
        return get(SINGLE_POINT_INDEX, false);
    }

    @Override
    public void singlePoint(boolean singlePoint) {
        set(SINGLE_POINT_INDEX, singlePoint);
        afterSet();
    }

    @Override
    public Particle particle() {
        return get(PARTICLE_INDEX, ParticleFactory.of(ParticleTypes.EFFECT));
    }

    @Override
    public void particle(@NotNull Particle particle) {
        set(PARTICLE_INDEX, EntityDataTypes.PARTICLE, particle);
    }
}
