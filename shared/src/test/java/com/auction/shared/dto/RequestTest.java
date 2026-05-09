package com.auction.shared.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test cho lớp Request DTO.
 */
public class RequestTest {

    @Test
    void testConstructorAndGetters() {
        Request request = new Request(MessageType.LOGIN, "user1", "{\"data\":\"test\"}");
        assertEquals(MessageType.LOGIN, request.getType());
        assertEquals("user1", request.getSenderId());
        assertEquals("{\"data\":\"test\"}", request.getPayload());
    }

    @Test
    void testPlaceBidRequest() {
        Request request = new Request(MessageType.PLACE_BID, "bidder1", "{\"amount\":1500}");
        assertEquals(MessageType.PLACE_BID, request.getType());
        assertEquals("bidder1", request.getSenderId());
    }

    @Test
    void testNullPayload() {
        Request request = new Request(MessageType.GET_STATUS, "user1", null);
        assertNull(request.getPayload());
    }
}
