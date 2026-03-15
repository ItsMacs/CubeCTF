package eu.macsworks.cubectf.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DurationUtil {

    public String formatDuration(long durationMs){
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        String minutesStr = minutes < 10 ? "0" + minutes : "" + minutes;
        String secondsStr = seconds < 10 ? "0" + seconds : "" + seconds;

        return minutesStr + ":" + secondsStr;
    }
    
    public String formatDurationWithWords(long durationMs){
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes > 0) return minutes + " minute" + (minutes != 1 ? "s" : "") + " " + seconds + " second" + (seconds != 1 ? "s" : "");

        return seconds + " second" + (seconds != 1 ? "s" : "");
    }

}
