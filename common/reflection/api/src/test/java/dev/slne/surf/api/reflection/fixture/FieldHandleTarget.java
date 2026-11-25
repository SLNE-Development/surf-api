package dev.slne.surf.api.reflection.fixture;

@SuppressWarnings({"unused", "FieldMayBeFinal"})
public final class FieldHandleTarget {

    private static int staticCount = 0;
    private static long staticTotal = 0L;
    private static boolean staticFlag = false;
    private static String staticLabel = "static";
    private static final int STATIC_FIXED = 1;

    private int count = 0;
    private long total = 0L;
    private boolean flag = false;
    private String label = "label";
    private Hidden hidden = new Hidden();
    private Object payload = "payload";
    private int xValue = 0;
    private final int fixed = 1;

    static final class Hidden {
    }
}
