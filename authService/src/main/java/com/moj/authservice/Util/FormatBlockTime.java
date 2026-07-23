package com.moj.authservice.Util;

import lombok.experimental.UtilityClass;

import java.time.Duration;

@UtilityClass
public class FormatBlockTime {
    public String formatBlockTime(Duration duration) {
        long minutes = duration.toMinutes();
        int seconds = duration.toSecondsPart();

        String formattedMinutes =
                minutes + (minutes == 1 ? " minute" : " minutes");

        String formattedSeconds =
                seconds + (seconds == 1 ? " second" : " seconds");

        if (minutes == 0) {
            return seconds + (seconds == 1 ? " second" : " seconds");
        }



        if (seconds == 0) {
            return formattedMinutes;
        }


        return formattedMinutes + " and " + formattedSeconds;
    }
    public String formatBlockTimeForReset(Duration duration) {
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        String formattedHours =
                hours + (hours == 1 ? " hour" : " hours");

        String formattedMinutes =
                minutes + (minutes == 1 ? " minute" : " minutes");

        if (hours == 0 && minutes == 0) {
            return seconds + (seconds == 1 ? " second" : " seconds");
        }

        if (hours == 0) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }


        if (minutes == 0) {
            return formattedHours;
        }


        return formattedHours + " and " + formattedMinutes;
    }
}
