package com.auction.shared.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class UserBidDTOTest {

    @Test
    public void testGettersAndSetters() {
        UserBidDTO dto = new UserBidDTO();
        
        dto.setItemId("item123");
        dto.setName("Test Item");
        dto.setCategory("ELECTRONICS");
        dto.setDescription("Testing");
        dto.setStartingPrice(100.0);
        dto.setCurrentHighestBid(150.0);
        dto.setHighestBidderId("user1");
        dto.setEndTime(123456789L);
        dto.setStatus("OPEN");
        dto.setMyHighestBid(120.0);
        dto.setWatchlisted(true);

        assertEquals("item123", dto.getItemId());
        assertEquals("Test Item", dto.getName());
        assertEquals("ELECTRONICS", dto.getCategory());
        assertEquals("Testing", dto.getDescription());
        assertEquals(100.0, dto.getStartingPrice(), 0.001);
        assertEquals(150.0, dto.getCurrentHighestBid(), 0.001);
        assertEquals("user1", dto.getHighestBidderId());
        assertEquals(123456789L, dto.getEndTime());
        assertEquals("OPEN", dto.getStatus());
        assertEquals(120.0, dto.getMyHighestBid(), 0.001);
        assertTrue(dto.isWatchlisted());
    }

    @Test
    public void testConstructor() {
        UserBidDTO dto = new UserBidDTO("item123", "Test Item", "ELECTRONICS", "Testing",
                100.0, 150.0, "user1", 123456789L, "OPEN", 120.0, true);

        assertEquals("item123", dto.getItemId());
        assertEquals("Test Item", dto.getName());
        assertEquals("ELECTRONICS", dto.getCategory());
        assertEquals("Testing", dto.getDescription());
        assertEquals(100.0, dto.getStartingPrice(), 0.001);
        assertEquals(150.0, dto.getCurrentHighestBid(), 0.001);
        assertEquals("user1", dto.getHighestBidderId());
        assertEquals(123456789L, dto.getEndTime());
        assertEquals("OPEN", dto.getStatus());
        assertEquals(120.0, dto.getMyHighestBid(), 0.001);
        assertTrue(dto.isWatchlisted());
    }
}
