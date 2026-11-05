package com.paqrap.simulacion;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioStartTimeTest {
    @Test void preservesHourAndMinute() {
        assertEquals(LocalDateTime.of(2026, 11, 4, 14, 37), ScenarioRunner.fechaInicio("2026-11-04T14:37"));
        assertEquals(LocalDateTime.of(2026, 11, 4, 14, 37), ScenarioRunner.fechaInicio("2026-11-04T14:37:59"));
    }
    @Test void acceptsLegacyDateAndRejectsInvalidTime() {
        assertEquals(LocalDateTime.of(2026, 11, 4, 0, 0), ScenarioRunner.fechaInicio("2026-11-04"));
        assertThrows(IllegalArgumentException.class, () -> ScenarioRunner.fechaInicio("2026-11-04T25:00"));
    }
}
