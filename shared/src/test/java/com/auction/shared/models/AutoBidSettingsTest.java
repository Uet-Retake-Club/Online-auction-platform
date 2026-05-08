package com.auction.shared.models;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test cho lớp AutoBidSettings.
 */
public class AutoBidSettingsTest {

    @Test
    void testConstructorDefaults() {
        AutoBidSettings settings = new AutoBidSettings("B001", "AUC001", 2000.0, 50.0, false);
        assertEquals("B001", settings.getBidderId());
        assertEquals("AUC001", settings.getAuctionId());
        assertEquals(2000.0, settings.getMaxPrice());
        assertEquals(50.0, settings.getBidIncrement());
        assertTrue(settings.isActive(), "Auto-bid phải active mặc định khi khởi tạo");
        assertFalse(settings.isAggressiveMode());
    }

    @Test
    void testAggressiveMode() {
        AutoBidSettings settings = new AutoBidSettings("B002", "AUC001", 3000.0, 100.0, true);
        assertTrue(settings.isAggressiveMode());
    }

    @Test
    void testSetActive() {
        AutoBidSettings settings = new AutoBidSettings("B001", "AUC001", 2000.0, 50.0, false);
        assertTrue(settings.isActive());
        settings.setActive(false);
        assertFalse(settings.isActive());
    }

    @Test
    void testSetMaxPrice() {
        AutoBidSettings settings = new AutoBidSettings("B001", "AUC001", 2000.0, 50.0, false);
        settings.setMaxPrice(3000.0);
        assertEquals(3000.0, settings.getMaxPrice());
    }

    @Test
    void testSetBidIncrement() {
        AutoBidSettings settings = new AutoBidSettings("B001", "AUC001", 2000.0, 50.0, false);
        settings.setBidIncrement(100.0);
        assertEquals(100.0, settings.getBidIncrement());
    }
}
