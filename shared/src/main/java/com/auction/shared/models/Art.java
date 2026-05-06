package com.auction.shared.models;

public class Art extends Item {
    private String artistName;

    public Art() {
        super(0, "", "", ItemCategory.ART, 0.0, 0L, 0L);
    }

    public Art(int id, String name, String description, double startingPrice, long startTime, long endTime, String artistName) {
        super(id, name, description, ItemCategory.ART, startingPrice, startTime, endTime);
        this.artistName = artistName;
    }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    @Override
    public void printInfo() {
        System.out.println("Art: " + name + " - Artist: " + artistName);
    }
}