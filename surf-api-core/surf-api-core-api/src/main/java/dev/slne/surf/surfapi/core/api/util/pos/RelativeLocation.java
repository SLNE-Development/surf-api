package dev.slne.surf.surfapi.core.api.util.pos;

import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;

public interface RelativeLocation {

    double x();

    double y();

    double z();

    float yaw();

    float pitch();

    default WrapperPlayServerEntityRelativeMoveAndRotation toPacket(int entityId, boolean onGround) {
        return new WrapperPlayServerEntityRelativeMoveAndRotation(entityId, x(), y(), z(), yaw(), pitch(), onGround);
    }

    static RelativeLocation of(double deltaX, double deltaY, double deltaZ, float deltaYaw, float deltaPitch) {
        return new RelativeLocationImpl(deltaX, deltaY, deltaZ, deltaYaw, deltaPitch);
    }

    static RelativeLocation of(Location locationA, Location locationB) {
        return of(locationA.getX() - locationB.getX(), locationA.getY() - locationB.getY(), locationA.getZ() - locationB.getZ(), locationA.getYaw() - locationB.getYaw(), locationA.getPitch() - locationB.getPitch());
    }
}
