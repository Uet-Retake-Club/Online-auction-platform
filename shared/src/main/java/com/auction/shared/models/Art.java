package com.auction.shared.models;

/** Represents an art item in the auction. */
public class Art extends Item {
  private String artistName;

  /**
   * Constructs a new Art item.
   *
   * @param id the unique identifier
   * @param name the name of the art
   * @param description the description
   * @param startingPrice the starting price
   * @param startTime the start time
   * @param endTime the end time
   * @param artistName the name of the artist
   */
  public Art(
      String id,
      String name,
      String description,
      double startingPrice,
      long startTime,
      long endTime,
      String artistName) {
    super(id, name, description, startingPrice, startTime, endTime);
    this.artistName = artistName;
  }

  public String getArtistName() {
    return artistName;
  }

  public void setArtistName(String artistName) {
    this.artistName = artistName;
  }

  @Override
  public void printInfo() {
    System.out.println("Art: " + name + " - Artist: " + artistName);
  }
}