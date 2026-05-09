package com.auction.shared.models;

/** Represents a vehicle item in the auction. */
public class Vehicle extends Item {
    private String brand;
    private String model;
    private int color;

    public Vehicle() {
        super("", "", "", ItemCategory.VEHICLE, 0.0, 0L, 0L);
    }

    public Vehicle(String id, String name, String description, double startingPrice,
         long startTime, long endTime, String brand, String model, int color) {
        super(id, name, description, ItemCategory.VEHICLE, startingPrice, startTime, endTime);
        this.brand = brand;
        this.model = model;
        this.color = color;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    @Override
    public void printInfo() {
        System.out.println("Vehicle: " + name + " - Brand: " + brand + ", Model: " + model + ", Color: " + color);
    }

}