package me.petulikan1.zMachines.utils;

import java.time.Duration;

public class TimeUtils {

    public static long toTicks(Duration duration) {
        return duration.toMillis() / 50;
    }

}
