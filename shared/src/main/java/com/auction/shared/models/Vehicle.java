package com.auction.shared.models;

public class Vehicle extends Item {
    private String ownerName;

    public Vehicle(String id, String name, String description, double startingPrice, long startTime, long endTime, String ownerName) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.ownerName = ownerName;
    }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    @Override
    public void printInfo() {
        System.out.println("Vehicle: " + name + " - Owner: " + ownerName);
    }
}