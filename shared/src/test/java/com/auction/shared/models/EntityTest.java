package com.auction.shared.models;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test cho lớp trừu tượng Entity thông qua subclass Bidder.
 */
public class EntityTest {

    @Test
    void testGetId() {
        Bidder bidder = new Bidder("U001", "test", "test@mail.com");
        assertEquals("U001", bidder.getId());
    }

    @Test
    void testSetId() {
        Bidder bidder = new Bidder("U001", "test", "test@mail.com");
        bidder.setId("U999");
        assertEquals("U999", bidder.getId());
    }
}
