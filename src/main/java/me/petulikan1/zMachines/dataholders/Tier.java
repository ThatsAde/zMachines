package me.petulikan1.zMachines.dataholders;

public final class Tier {
    public static final int MIN = 1;
    public static final int MAX = 3;
    public static final double STEP = 1.2;

    private Tier() {}

    public static double speedMultiplier(int tier) {
        return Math.pow(STEP, tier - 1);
    }

    public static double timeMultiplier(int tier) {
        return 1.0 / speedMultiplier(tier);
    }

    public static boolean isValid(int tier) {
        return tier >= MIN && tier <= MAX;
    }

    public static int clamp(int tier) {
        if (tier < MIN) return MIN;
        if (tier > MAX) return MAX;
        return tier;
    }
}
