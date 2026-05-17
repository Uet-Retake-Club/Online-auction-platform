package com.auction.shared.models;

/** Represents an electronics item in the auction. */
public class Electronics extends Item {
    private String brand;
    private String warranty_period;

    public Electronics() {
        super("", "", "", ItemCategory.ELECTRONICS, 0.0, 0L, 0L, "");
    }

    public Electronics(String id, String name, String description, double startingPrice, long startTime, long endTime, String brand, String warranty_period, String sellerId) {
        super(id, name, description, ItemCategory.ELECTRONICS, startingPrice, startTime, endTime, sellerId);
        this.brand = brand;
        this.warranty_period = warranty_period;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getWarranty_period() { return warranty_period; }
    public void setWarranty_period(String warranty_period) { this.warranty_period = warranty_period; }

    @Override
    public void printInfo() {
        System.out.println("Electronics: " + name + " - Brand: " + brand + ", Warranty: " + warranty_period + " months");
    }
}