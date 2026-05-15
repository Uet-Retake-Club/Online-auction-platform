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
        assertEquals(18, values.length, "MessageType must have exactly 18 values");
    }

    @Test
    void testValueOf() {
        assertEquals(MessageType.LOGIN, MessageType.valueOf("LOGIN"));
        assertEquals(MessageType.LOGIN_SUCCESS, MessageType.valueOf("LOGIN_SUCCESS"));
        assertEquals(MessageType.LOGIN_FAIL, MessageType.valueOf("LOGIN_FAIL"));
        assertEquals(MessageType.REGISTER, MessageType.valueOf("REGISTER"));
        assertEquals(MessageType.REGISTER_SUCCESS, MessageType.valueOf("REGISTER_SUCCESS"));
        assertEquals(MessageType.REGISTER_FAIL, MessageType.valueOf("REGISTER_FAIL"));
        assertEquals(MessageType.PLACE_BID, MessageType.valueOf("PLACE_BID"));
        assertEquals(MessageType.SETUP_AUTO_BID, MessageType.valueOf("SETUP_AUTO_BID"));
        assertEquals(MessageType.BID_SUCCESS, MessageType.valueOf("BID_SUCCESS"));
        assertEquals(MessageType.BID_ERROR, MessageType.valueOf("BID_ERROR"));
        assertEquals(MessageType.NEW_BID_BROADCAST, MessageType.valueOf("NEW_BID_BROADCAST"));
        assertEquals(MessageType.AUCTION_ENDED, MessageType.valueOf("AUCTION_ENDED"));
        assertEquals(MessageType.GET_STATUS, MessageType.valueOf("GET_STATUS"));
        assertEquals(MessageType.CREATE_ITEM, MessageType.valueOf("CREATE_ITEM"));
        assertEquals(MessageType.CREATE_ITEM_SUCCESS, MessageType.valueOf("CREATE_ITEM_SUCCESS"));
        assertEquals(MessageType.CREATE_ITEM_FAIL, MessageType.valueOf("CREATE_ITEM_FAIL"));
        assertEquals(MessageType.GET_SELLER_ITEMS, MessageType.valueOf("GET_SELLER_ITEMS"));
        assertEquals(MessageType.GET_SELLER_ITEMS_RESPONSE, MessageType.valueOf("GET_SELLER_ITEMS_RESPONSE"));
    }

    @Test
    void testInvalidValueThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            MessageType.valueOf("INVALID_TYPE");
        });
    }
}
