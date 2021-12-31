package xyz.rgnt.crownedhelper.statics;

public class TimeStatics {

    /**
     * @return Current unix timestamp
     */
    public static long getCurrentUnix() {
        return System.currentTimeMillis() / 1000L;
    }


    /**
     *
     * @param unixTime
     * @param threshold
     * @return
     */
    public static boolean deltaIsLargerThan(long unixTime, long threshold) {
        return getCurrentUnix() - unixTime > threshold;
    }

}
