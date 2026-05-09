package com.auction.shared.models;

public class Collectibles extends Item {
    private String type;
    private String rarity;
    private String condition;

    public Collectibles() {
        super("", "", "", ItemCategory.COLLECTIBLES, 0.0, 0L, 0L);
    }

    public Collectibles(String id, String name, String description, double startingPrice, long startTime, long endTime,
            String type, String rarity, String condition) {
        super(id, name, description, ItemCategory.COLLECTIBLES, startingPrice, startTime, endTime);
        this.type = type;
        this.rarity = rarity;
        this.condition = condition;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    @Override
    public void printInfo() {
        System.out.println("Collectible: " + name + " - Type: " + type + ", Rarity: " + rarity + ", Condition: " + condition);
    }
}
