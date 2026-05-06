package com.auction.shared.models;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics() {
        super(0, "", "", ItemCategory.ELECTRONICS, 0.0, 0L, 0L);
    }

    public Electronics(int id, String name, String description, double startingPrice, long startTime, long endTime, int warrantyMonths) {
        super(id, name, description, ItemCategory.ELECTRONICS, startingPrice, startTime, endTime);
        this.warrantyMonths = warrantyMonths;
    }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    @Override
    public void printInfo() {
        System.out.println("Electronics: " + name + " - Warranty: " + warrantyMonths + " months");
    }
}