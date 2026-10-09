package com.paqrap.ingesta;

import com.paqrap.model.Order;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderRecordParserTest {

    @Test
    public void testParseValidLine() {
        OrderRecordParser parser = new OrderRecordParser();
        Order order = parser.parse("01d00h00m:31,14,c9103,04,04");
        
        assertEquals("PED-1", order.getId());
        assertEquals(0.0, order.getPlacementTimeHours(), 1e-9);
        assertEquals(31.0, order.getDestination().getX(), 1e-9);
        assertEquals(14.0, order.getDestination().getY(), 1e-9);
        assertEquals("c9103", order.getDestination().getId());
        assertEquals(4, order.getQuantity());
        assertEquals(4.0, order.getMaxDeliveryWindowHours(), 1e-9);
    }

    @Test
    public void testParseInvalidQuantity() {
        OrderRecordParser parser = new OrderRecordParser();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            parser.parse("01d00h00m:31,14,c9103,0,04");
        });
        assertTrue(exception.getMessage().contains("línea 1"));
    }
}
