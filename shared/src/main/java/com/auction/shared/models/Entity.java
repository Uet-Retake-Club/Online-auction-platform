package com.auction.shared.models;

/** Base class for all entities in the system. */
public abstract class Entity {
  protected String id;

  /**
   * Constructs a new Entity.
   *
   * @param id the unique identifier
   */
  public Entity(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}