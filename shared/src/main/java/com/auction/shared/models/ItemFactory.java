package com.auction.shared.models;

public class ItemFactory {
    public static Item createItem(ItemCategory category) {
        switch (category) {
            case ELECTRONICS:
                return new Electronics();
            case VEHICLE:
                return new Vehicle();
            case HOME_AND_GARDEN:
                return new HomeAndGarden();
            case SPORTS:
                return new Sports();
            case FASHION:
                return new Fashion();
            case COLLECTIBLES:
                return new Collectibles();
            default:
                return new OtherItem();
        }
    }
}