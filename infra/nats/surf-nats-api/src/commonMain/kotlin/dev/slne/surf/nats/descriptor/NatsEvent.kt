package dev.slne.surf.nats.descriptor

public interface NatsEvent {
    public val natsDescriptor: NatsEventDescriptor<*>
}
