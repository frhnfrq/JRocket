package xyz.farhanfarooqui.JRocket;

import java.util.concurrent.atomic.AtomicLong;

class Utils {
    private static AtomicLong idCounter = new AtomicLong(1);

    public static String createID() {
        return String.valueOf(idCounter.getAndIncrement());
    }
}
