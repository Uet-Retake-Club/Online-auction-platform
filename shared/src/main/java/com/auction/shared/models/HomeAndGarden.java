package com.auction.shared.models;

public class HomeAndGarden extends Item {
    private String color;

    public HomeAndGarden() {
        super("", "", "", ItemCategory.HOME_AND_GARDEN, 0.0, 0L, 0L, "");
    }

    public HomeAndGarden(String id, String name, String description, double startingPrice, long startTime, long endTime, String color, String sellerId) {
        super(id, name, description, ItemCategory.HOME_AND_GARDEN, startingPrice, startTime, endTime, sellerId);
        this.color = color;
    }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    @Override
    public void printInfo() {
        System.out.println("Home and Garden: " + name + " - Color: " + color);
    }
}
