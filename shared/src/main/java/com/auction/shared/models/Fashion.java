package com.auction.shared.models;

public class Fashion extends Item {
    private String brand;
    private String size;
    private String color;
    private String material;

    public Fashion() {
        super("", "", "", ItemCategory.FASHION, 0.0, 0L, 0L);
    }
    
    public Fashion(String id, String name, String description, double startingPrice, long startTime, long endTime,
            String brand, String size, String color, String material) {
        super(id, name, description, ItemCategory.FASHION, startingPrice, startTime, endTime);
        this.brand = brand;
        this.size = size;
        this.color = color;
        this.material = material;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    @Override
    public void printInfo() {
        System.out.println("Fashion: " + name + " - Brand: " + brand + ", Size: " + size + ", Color: " + color + ", Material: " + material);
    }
}
