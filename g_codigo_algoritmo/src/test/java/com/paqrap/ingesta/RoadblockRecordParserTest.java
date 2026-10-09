package com.paqrap.ingesta;

import com.paqrap.model.Roadblock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RoadblockRecordParserTest {

    @Test
    public void testParseValidLine() {
        RoadblockRecordParser parser = new RoadblockRecordParser();
        Roadblock roadblock = parser.parse("01d06h00m-01d15h00m:31,21,34,21");
        
        // 01d06h00m = 6.0, 01d15h00m = 15.0
        assertEquals(6.0, roadblock.startTimeHours, 1e-9);
        assertEquals(15.0, roadblock.endTimeHours, 1e-9);
        
        // Roadblock decomposes the path: (31,21) -> (34,21) creates 4 points: 31, 32, 33, 34
        assertEquals(4, roadblock.path.size());
        assertEquals(31.0, roadblock.path.get(0).getX(), 1e-9);
        assertEquals(32.0, roadblock.path.get(1).getX(), 1e-9);
        assertEquals(33.0, roadblock.path.get(2).getX(), 1e-9);
        assertEquals(34.0, roadblock.path.get(3).getX(), 1e-9);
    }
}
