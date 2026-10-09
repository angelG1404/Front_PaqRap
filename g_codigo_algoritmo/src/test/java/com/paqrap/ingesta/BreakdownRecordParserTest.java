package com.paqrap.ingesta;

import com.paqrap.model.Breakdown;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BreakdownRecordParserTest {

    @Test
    public void testParseValidLine() {
        BreakdownRecordParser parser = new BreakdownRecordParser();
        // 01d10h30m = (1-1)*24 + 10 + 30/60 = 10.5
        Breakdown breakdown = parser.parse("01d10h30m:TA01,2");
        
        assertEquals("TA01", breakdown.vehicleId);
        assertEquals(10.5, breakdown.startMomentHours, 1e-9);
        assertEquals(2, breakdown.type);
    }
}
