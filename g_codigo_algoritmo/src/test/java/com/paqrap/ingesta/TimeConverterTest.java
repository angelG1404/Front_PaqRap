package com.paqrap.ingesta;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeConverterTest {

    @Test
    public void testParseHours() {
        // (1-1)*24 + 6 + 0/60 = 6.0
        assertEquals(6.0, TimeConverter.parseHours("01d06h00m"), 1e-9);
        // (2-1)*24 + 0 + 30/60 = 24.5
        assertEquals(24.5, TimeConverter.parseHours("02d00h30m"), 1e-9);
    }

    @Test
    public void testParseRange() {
        double[] range = TimeConverter.parseRange("01d06h00m-02d00h30m");
        assertEquals(6.0, range[0], 1e-9);
        assertEquals(24.5, range[1], 1e-9);
    }

    @Test
    public void testDaysBetweenAsHours() {
        LocalDate epoch = LocalDate.of(2026, 9, 1);
        LocalDate date = LocalDate.of(2026, 9, 3);
        // 2 days * 24 = 48 hours
        assertEquals(48.0, TimeConverter.daysBetweenAsHours(epoch, date), 1e-9);
    }
}
