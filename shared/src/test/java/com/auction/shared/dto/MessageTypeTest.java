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
        assertEquals(52, values.length, "MessageType must have exactly 52 values");
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
        assertEquals(MessageType.GET_WALLET_BALANCE, MessageType.valueOf("GET_WALLET_BALANCE"));
        assertEquals(MessageType.WALLET_BALANCE_RESPONSE, MessageType.valueOf("WALLET_BALANCE_RESPONSE"));
        assertEquals(MessageType.WALLET_TOPUP_REQUEST, MessageType.valueOf("WALLET_TOPUP_REQUEST"));
        assertEquals(MessageType.WALLET_TOPUP_APPROVE, MessageType.valueOf("WALLET_TOPUP_APPROVE"));
        assertEquals(MessageType.NEW_ITEM_BROADCAST, MessageType.valueOf("NEW_ITEM_BROADCAST"));
        assertEquals(MessageType.WATCHLIST_ADD, MessageType.valueOf("WATCHLIST_ADD"));
        assertEquals(MessageType.WATCHLIST_REMOVE, MessageType.valueOf("WATCHLIST_REMOVE"));
        assertEquals(MessageType.GET_WATCHLIST, MessageType.valueOf("GET_WATCHLIST"));
        assertEquals(MessageType.WATCHLIST_RESPONSE, MessageType.valueOf("WATCHLIST_RESPONSE"));
        assertEquals(MessageType.GET_ALL_ITEMS, MessageType.valueOf("GET_ALL_ITEMS"));
        assertEquals(MessageType.GET_ALL_ITEMS_RESPONSE, MessageType.valueOf("GET_ALL_ITEMS_RESPONSE"));
        assertEquals(MessageType.ADMIN_GET_PENDING_TOPUPS, MessageType.valueOf("ADMIN_GET_PENDING_TOPUPS"));
        assertEquals(MessageType.ADMIN_APPROVE_TOPUP, MessageType.valueOf("ADMIN_APPROVE_TOPUP"));
        assertEquals(MessageType.ADMIN_REJECT_TOPUP, MessageType.valueOf("ADMIN_REJECT_TOPUP"));
        assertEquals(MessageType.ADMIN_PENDING_TOPUPS_RESPONSE, MessageType.valueOf("ADMIN_PENDING_TOPUPS_RESPONSE"));
        assertEquals(MessageType.GET_WALLET_HISTORY, MessageType.valueOf("GET_WALLET_HISTORY"));
        assertEquals(MessageType.WALLET_HISTORY_RESPONSE, MessageType.valueOf("WALLET_HISTORY_RESPONSE"));
        assertEquals(MessageType.ADMIN_GET_STATS, MessageType.valueOf("ADMIN_GET_STATS"));
        assertEquals(MessageType.ADMIN_STATS_RESPONSE, MessageType.valueOf("ADMIN_STATS_RESPONSE"));
        assertEquals(MessageType.ADMIN_GET_USERS, MessageType.valueOf("ADMIN_GET_USERS"));
        assertEquals(MessageType.ADMIN_USERS_RESPONSE, MessageType.valueOf("ADMIN_USERS_RESPONSE"));
        assertEquals(MessageType.ADMIN_GET_AUCTIONS, MessageType.valueOf("ADMIN_GET_AUCTIONS"));
        assertEquals(MessageType.ADMIN_AUCTIONS_RESPONSE, MessageType.valueOf("ADMIN_AUCTIONS_RESPONSE"));
        // ... (wait, let's keep all elements including ADMIN_BAN_USER etc to be fully safe)
        assertEquals(MessageType.ADMIN_BAN_USER, MessageType.valueOf("ADMIN_BAN_USER"));
        assertEquals(MessageType.ADMIN_UNBAN_USER, MessageType.valueOf("ADMIN_UNBAN_USER"));
        assertEquals(MessageType.ADMIN_GET_BIDS, MessageType.valueOf("ADMIN_GET_BIDS"));
        assertEquals(MessageType.ADMIN_BIDS_RESPONSE, MessageType.valueOf("ADMIN_BIDS_RESPONSE"));
        assertEquals(MessageType.INVOICE_PAY, MessageType.valueOf("INVOICE_PAY"));
        assertEquals(MessageType.INVOICE_CANCEL, MessageType.valueOf("INVOICE_CANCEL"));
        assertEquals(MessageType.GET_MY_BIDS, MessageType.valueOf("GET_MY_BIDS"));
        assertEquals(MessageType.MY_BIDS_RESPONSE, MessageType.valueOf("MY_BIDS_RESPONSE"));
        assertEquals(MessageType.GET_MY_INVOICES, MessageType.valueOf("GET_MY_INVOICES"));
        assertEquals(MessageType.GET_MY_INVOICES_SUCCESS, MessageType.valueOf("GET_MY_INVOICES_SUCCESS"));
        assertEquals(MessageType.GET_SELLER_DASHBOARD, MessageType.valueOf("GET_SELLER_DASHBOARD"));
    }


    @Test
    void testInvalidValueThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            MessageType.valueOf("INVALID_TYPE");
        });
    }
}
