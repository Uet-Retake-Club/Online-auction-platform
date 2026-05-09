package com.auction.shared.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test cho enum MessageType — đảm bảo đủ giá trị và valueOf hoạt động đúng.
 */
public class MessageTypeTest {

    @Test
    void testAllValuesExist() {
        MessageType[] values = MessageType.values();
        assertEquals(8, values.length, "MessageType phải có đúng 8 giá trị");
    }

    @Test
    void testValueOf() {
        assertEquals(MessageType.LOGIN, MessageType.valueOf("LOGIN"));
        assertEquals(MessageType.PLACE_BID, MessageType.valueOf("PLACE_BID"));
        assertEquals(MessageType.SETUP_AUTO_BID, MessageType.valueOf("SETUP_AUTO_BID"));
        assertEquals(MessageType.BID_SUCCESS, MessageType.valueOf("BID_SUCCESS"));
        assertEquals(MessageType.BID_ERROR, MessageType.valueOf("BID_ERROR"));
        assertEquals(MessageType.NEW_BID_BROADCAST, MessageType.valueOf("NEW_BID_BROADCAST"));
        assertEquals(MessageType.AUCTION_ENDED, MessageType.valueOf("AUCTION_ENDED"));
        assertEquals(MessageType.GET_STATUS, MessageType.valueOf("GET_STATUS"));
    }

    @Test
    void testInvalidValueThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            MessageType.valueOf("INVALID_TYPE");
        });
    }
}
