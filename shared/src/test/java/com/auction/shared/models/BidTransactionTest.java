package com.auction.shared.models;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test cho lớp BidTransaction.
 */
public class BidTransactionTest {

    @Test
    void testConstructorAndGetters() {
        BidTransaction tx = new BidTransaction("TX001", "ITEM-123", "B001", 1500.0, 1700000000000L);
        assertEquals("TX001", tx.getId());
        assertEquals("ITEM-123", tx.getItemId());
        assertEquals("B001", tx.getBidderId());
        assertEquals(1500.0, tx.getBidAmount());
        assertEquals(1700000000000L, tx.getTimestamp());
    }

    @Test
    void testMultipleTransactions() {
        BidTransaction tx1 = new BidTransaction("TX001", "ITEM-1", "B001", 100.0, 1000L);
        BidTransaction tx2 = new BidTransaction("TX002", "ITEM-1", "B002", 200.0, 2000L);
        assertNotEquals(tx1.getId(), tx2.getId());
        assertTrue(tx2.getBidAmount() > tx1.getBidAmount());
    }
}
