package com.auction.client.chart;

/**
 * Immutable record representing a single bid event on the price chart.
 *
 * <p>Instances are created by {@link BidDataStore#addPoint} and consumed
 * read-only by {@link ChartRenderer} and {@link ChartTooltip}.
 */
public final class DataPoint {

  /** Bid price at the moment this event occurred. */
  public final double price;

  /** Epoch-millisecond timestamp of the bid event. */
  public final long timestampMs;

  /**
   * Constructs a new immutable data point.
   *
   * @param price       bid price
   * @param timestampMs epoch-millisecond timestamp
   */
  public DataPoint(final double price, final long timestampMs) {
    this.price       = price;
    this.timestampMs = timestampMs;
  }
}
