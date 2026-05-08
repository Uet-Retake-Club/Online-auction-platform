package com.auction.shared.models;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test cho lớp User (abstract) thông qua Bidder và Seller.
 */
public class UserTest {

    @Test
    void testBidderConstructor() {
        Bidder bidder = new Bidder("B001", "Hoang", "hoang@mail.com");
        assertEquals("B001", bidder.getId());
        assertEquals("Hoang", bidder.getUsername());
        assertEquals("hoang@mail.com", bidder.getEmail());
        assertEquals("BIDDER", bidder.getRole());
    }

    @Test
    void testSellerConstructor() {
        Seller seller = new Seller("S001", "Tuan", "tuan@mail.com");
        assertEquals("S001", seller.getId());
        assertEquals("Tuan", seller.getUsername());
        assertEquals("tuan@mail.com", seller.getEmail());
        assertEquals("SELLER", seller.getRole());
    }

    @Test
    void testSetUsername() {
        Bidder bidder = new Bidder("B001", "OldName", "mail@mail.com");
        bidder.setUsername("NewName");
        assertEquals("NewName", bidder.getUsername());
    }

    @Test
    void testSetEmail() {
        Bidder bidder = new Bidder("B001", "Name", "old@mail.com");
        bidder.setEmail("new@mail.com");
        assertEquals("new@mail.com", bidder.getEmail());
    }
}
