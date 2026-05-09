package com.auction.shared.models;

/** Represents a vehicle item in the auction. */
public class Vehicle extends Item {
  private String ownerName;

  /**
   * Constructs a new Vehicle.
   *
   * @param id the unique identifier
   * @param name the name of the vehicle
   * @param description the description
   * @param startingPrice the starting price
   * @param startTime the start time
   * @param endTime the end time
   * @param ownerName the name of the owner
   */
  public Vehicle(
      String id,
      String name,
      String description,
      double startingPrice,
      long startTime,
      long endTime,
      String ownerName) {
    super(id, name, description, startingPrice, startTime, endTime);
    this.ownerName = ownerName;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  @Override
  public void printInfo() {
    System.out.println("Vehicle: " + name + " - Owner: " + ownerName);
  }
}