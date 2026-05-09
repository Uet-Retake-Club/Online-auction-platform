package com.auction.shared.models;

public class Electronics extends Item {
    private int brand;
    private int warranty_period;

    public Electronics() {
        super("", "", "", ItemCategory.ELECTRONICS, 0.0, 0L, 0L);
    }

    public Electronics(String id, String name, String description, double startingPrice, long startTime, long endTime, int brand, int warranty_period) {
        super(id, name, description, ItemCategory.ELECTRONICS, startingPrice, startTime, endTime);
        this.brand = brand;
        this.warranty_period = warranty_period;
    }

    public int getBrand() { return brand; }
    public void setBrand(int brand) { this.brand = brand; }

    public int getWarranty_period() { return warranty_period; }
    public void setWarranty_period(int warranty_period) { this.warranty_period = warranty_period; }

    @Override
    public void printInfo() {
        System.out.println("Electronics: " + name + " - Brand: " + brand + ", Warranty: " + warranty_period + " months");
    }
}