package dev.slne.surf.surfapi.core.api.packet.entity;

public record Brightness(int block, int sky) {
    public static Brightness FULL_BRIGHT = new Brightness(15, 15);

    public Brightness {
//        checkArgument(0 <= block && block <= 15, "Block brightness out of range: %s", block);
//        checkArgument(0 <= sky && sky <= 15, "Sky brightness out of range: %s", sky);
    }

    public int pack() {
        return this.block << 4 | this.sky << 20;
    }

    public static Brightness unpack(int packed) {
        int block = packed >> 4 & 65535;
        int sky = packed >> 20 & 65535;
        return new Brightness(block, sky);
    }
}
