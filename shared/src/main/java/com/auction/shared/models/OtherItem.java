package com.auction.shared.models;

public class OtherItem extends Item {
    
    public OtherItem() {
        super("", "", "", ItemCategory.OTHER, 0.0, 0L, 0L, "");
    }

    public OtherItem(String id, String name, String description, double startingPrice, long startTime, long endTime, String sellerId) {
        super(id, name, description, ItemCategory.OTHER, startingPrice, startTime, endTime, sellerId);
    }

    @Override
    public void printInfo() {
        System.out.println("Other Item: " + name + " - Description: " + description);
    }
}
