package com.moj.authservice.Util;

import lombok.experimental.UtilityClass;

import java.time.Duration;

@UtilityClass
public class FormatBlockTime {
    public String formatBlockTime(Duration duration) {
        long minutes = duration.toMinutes();
        int seconds = duration.toSecondsPart();
        if (minutes == 0) {
            return seconds + " seconds";
        }

        if (seconds == 0) {
            return minutes + " minutes";
        }

        return minutes + " minutes and " + seconds + " seconds";
    }
}
