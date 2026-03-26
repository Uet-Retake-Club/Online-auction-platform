package com.auction.shared.models;

public class Art extends Item {
    private String artistName;

    public Art(String id, String name, String description, double startingPrice, long startTime, long endTime, String artistName) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.artistName = artistName;
    }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    @Override
    public void printInfo() {
        System.out.println("Art: " + name + " - Artist: " + artistName);
    }
}