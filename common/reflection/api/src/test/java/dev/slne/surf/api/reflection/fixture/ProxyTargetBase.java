package dev.slne.surf.api.reflection.fixture;

@SuppressWarnings({"unused", "FieldMayBeFinal"})
abstract class ProxyTargetBase {

    private String baseValue = "base";

    protected String inherited() {
        return "inherited";
    }
}
