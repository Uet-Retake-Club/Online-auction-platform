package com.auction.shared.models;

/** Represents an electronics item in the auction. */
public class Electronics extends Item {
  private int warrantyMonths;

  /**
   * Constructs a new Electronics item.
   *
   * @param id the unique identifier
   * @param name the name of the electronics
   * @param description the description
   * @param startingPrice the starting price
   * @param startTime the start time
   * @param endTime the end time
   * @param warrantyMonths the warranty period in months
   */
  public Electronics(
      String id,
      String name,
      String description,
      double startingPrice,
      long startTime,
      long endTime,
      int warrantyMonths) {
    super(id, name, description, startingPrice, startTime, endTime);
    this.warrantyMonths = warrantyMonths;
  }

  public int getWarrantyMonths() {
    return warrantyMonths;
  }

  public void setWarrantyMonths(int warrantyMonths) {
    this.warrantyMonths = warrantyMonths;
  }

  @Override
  public void printInfo() {
    System.out.println("Electronics: " + name + " - Warranty: " + warrantyMonths + " months");
  }
}