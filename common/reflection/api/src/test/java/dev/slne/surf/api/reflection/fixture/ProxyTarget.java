package dev.slne.surf.api.reflection.fixture;

import java.io.IOException;

@SuppressWarnings({"unused", "FieldMayBeFinal"})
final class ProxyTarget extends ProxyTargetBase {

    private static int counter = 0;
    private static final String CONSTANT = "constant";

    private String name;
    private final int fixed;

    private ProxyTarget(String name) {
        this(name, -1);
    }

    private ProxyTarget(String name, int fixed) {
        this.name = name;
        this.fixed = fixed;
    }

    private String greet(String other) {
        return name + " greets " + other;
    }

    private long sum(long a, int b) {
        return a + b;
    }

    private static int twice(int value) {
        return value * 2;
    }

    private void fail() throws IOException {
        throw new IOException("boom");
    }

    private String overloaded(String value) {
        return "string";
    }

    private String overloaded(Integer value) {
        return "integer";
    }
}
