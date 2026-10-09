package com.paqrap.ingesta;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TimeConverter {

    /**
     * Converts "DDdHHhMMm" (e.g., "01d06h00m") to double hours.
     * Logic: (day-1)*24 + hour + minute/60.0.
     */
    public static double parseHours(String token) {
        // Expected format: DDdHHhMMm
        int d = Integer.parseInt(token.substring(0, 2));
        int h = Integer.parseInt(token.substring(3, 5));
        int m = Integer.parseInt(token.substring(6, 8));
        return (d - 1) * 24.0 + h + (m / 60.0);
    }

    /**
     * Parses "DDdHHhMMm-DDdHHhMMm" and returns double[2] {start, end}.
     */
    public static double[] parseRange(String rangeToken) {
        String[] parts = rangeToken.split("-");
        return new double[]{parseHours(parts[0]), parseHours(parts[1])};
    }

    /**
     * Converts a calendar date (aaaammdd) to relative hours based on an epoch.
     */
    public static double daysBetweenAsHours(LocalDate epoch, LocalDate fecha) {
        long days = ChronoUnit.DAYS.between(epoch, fecha);
        return days * 24.0;
    }
}
