package com.auction.shared.models;

public class Sports extends Item {
    private String sportType;
    private String color;

    public Sports() {
        super("", "", "", ItemCategory.SPORTS, 0.0, 0L, 0L, "");
    }

    public Sports(String id, String name, String description, double startingPrice, long startTime, long endTime, String sportType, String color, String sellerId) {
        super(id, name, description, ItemCategory.SPORTS, startingPrice, startTime, endTime, sellerId);
        this.sportType = sportType;
        this.color = color;
    }

    public String getSportType() { return sportType; }
    public void setSportType(String sportType) { this.sportType = sportType; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }  

    @Override
    public void printInfo() {
        System.out.println("Sports: " + name + " - Sport Type: " + sportType + ", Color: " + color);
    }
}
