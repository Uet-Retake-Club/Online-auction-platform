package com.auction.shared.models;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test cho lớp Auction.
 */
public class AuctionTest {

    // Dùng Collectibles làm concrete Item để tạo Auction
    private Collectibles createSampleItem() {
        return new Collectibles("ITEM-001", "Starry Night", "Van Gogh painting", 1000.0,
                System.currentTimeMillis(), System.currentTimeMillis() + 3600000, 
                "Painting", "Rare", "Good", "S001");
    }

    @Test
    void testAuctionConstructor() {
        Collectibles item = createSampleItem();
        Seller seller = new Seller("S001", "Gallery", "gallery@mail.com");
        Auction auction = new Auction("AUC-001", item, seller);

        assertEquals("AUC-001", auction.getId());
        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
        assertNotNull(auction.getBidHistory());
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void testAddBid() {
        Collectibles item = createSampleItem();
        Seller seller = new Seller("S001", "Gallery", "gallery@mail.com");
        Auction auction = new Auction("AUC-001", item, seller);

        BidTransaction tx = new BidTransaction("TX001", "ITEM-001", "B001", 1200.0, System.currentTimeMillis());
        auction.addBid(tx);

        assertEquals(1, auction.getBidHistory().size());
        assertEquals("TX001", auction.getBidHistory().get(0).getId());
    }

    @Test
    void testMultipleBids() {
        Collectibles item = createSampleItem();
        Seller seller = new Seller("S001", "Gallery", "gallery@mail.com");
        Auction auction = new Auction("AUC-001", item, seller);

        auction.addBid(new BidTransaction("TX001", "ITEM-001", "B001", 1200.0, 1000L));
        auction.addBid(new BidTransaction("TX002", "ITEM-001", "B002", 1400.0, 2000L));
        auction.addBid(new BidTransaction("TX003", "ITEM-001", "B001", 1600.0, 3000L));

        assertEquals(3, auction.getBidHistory().size());
    }

    @Test
    void testSetItem() {
        Collectibles item1 = createSampleItem();
        Collectibles item2 = new Collectibles("ITEM-002", "Mona Lisa", "Da Vinci", 5000.0,
                System.currentTimeMillis(), System.currentTimeMillis() + 3600000, 
                "Painting", "Ultra Rare", "Perfect", "S001");
        Seller seller = new Seller("S001", "Gallery", "gallery@mail.com");
        Auction auction = new Auction("AUC-001", item1, seller);

        auction.setItem(item2);
        assertEquals("ITEM-002", auction.getItem().getId());
    }
}
