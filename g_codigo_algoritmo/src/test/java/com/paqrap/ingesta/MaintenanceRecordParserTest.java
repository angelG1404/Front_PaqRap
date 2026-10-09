package com.paqrap.ingesta;

import com.paqrap.model.Maintenance;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class MaintenanceRecordParserTest {

    @Test
    public void testParseValidLine() {
        LocalDate epoch = LocalDate.of(2026, 3, 1);
        MaintenanceRecordParser parser = new MaintenanceRecordParser(epoch);
        
        // 20260315 is 14 days after 20260301 (14 * 24 = 336 hours)
        Maintenance maintenance = parser.parse("20260315:TA01");
        
        assertEquals("TA01", maintenance.vehicleId);
        assertEquals(336.0, maintenance.startTimeHours, 1e-9);
        assertEquals(360.0, maintenance.endTimeHours, 1e-9);
    }
}
