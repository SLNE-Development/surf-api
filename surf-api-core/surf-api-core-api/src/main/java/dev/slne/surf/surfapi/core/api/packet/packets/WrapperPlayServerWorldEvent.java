package dev.slne.surf.surfapi.core.api.packet.packets;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.NotNull;

public final class WrapperPlayServerWorldEvent extends PacketWrapper<WrapperPlayServerWorldEvent> {

    private int eventId;
    private Vector3i pos;
    private int data;
    private boolean global;

    public WrapperPlayServerWorldEvent(PacketSendEvent event) {
        super(event);
    }

    public WrapperPlayServerWorldEvent(int eventId, Vector3i pos, int data, boolean global) {
        super(0x26);

        this.eventId = eventId;
        this.pos = pos;
        this.data = data;
        this.global = global;
    }

    @Override
    public void read() {
        this.eventId = readInt();
        this.pos = readBlockPosition();
        this.data = readInt();
        this.global = readBoolean();
    }

    @Override
    public void write() {
        writeInt(this.eventId);
        writeBlockPosition(this.pos);
        writeInt(this.data);
        writeBoolean(this.global);
    }

    @Override
    public void copy(@NotNull WrapperPlayServerWorldEvent wrapper) {
        this.eventId = wrapper.eventId;
        this.pos = wrapper.pos;
        this.data = wrapper.data;
        this.global = wrapper.global;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public Vector3i getPos() {
        return pos;
    }

    public void setPos(Vector3i pos) {
        this.pos = pos;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }
}
