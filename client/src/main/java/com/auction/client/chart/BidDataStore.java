package com.auction.client.chart;

import com.auction.shared.models.BidTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-safe store for bid {@link DataPoint} objects.
 *
 * <p>Handles timestamp validation, sorted insertion, window-based purging,
 * and global-max tracking. All mutating operations are synchronised on the
 * internal list so they can be called safely from any thread.
 */
public final class BidDataStore {

  /** Backing list, always sorted ascending by {@code timestampMs}. */
  private final List<DataPoint> points = Collections.synchronizedList(new ArrayList<>());

  /** Rolling window in milliseconds — points older than this are pruned. */
  private long maxWindowMs;

  /** Running maximum price across all points ever added. */
  private double globalMax = Double.NEGATIVE_INFINITY;

  /**
   * Constructs a new store with the given rolling window.
   *
   * @param maxWindowMs rolling time window in milliseconds
   */
  public BidDataStore(final long maxWindowMs) {
    this.maxWindowMs = maxWindowMs;
  }

  /**
   * Adds a bid point after validating and clamping the timestamp.
   *
   * <p>Points with timestamps more than {@code maxWindowMs} in the past are
   * silently rejected. Future timestamps (up to 5 s ahead) are accepted but
   * clamped to {@code now + 5 s}.
   *
   * @param price       bid price
   * @param timestampMs raw epoch-ms timestamp from the bid event
   * @return {@code true} if the point was accepted and stored
   */
  public boolean addPoint(final double price, final long timestampMs) {
    final long now = System.currentTimeMillis();
    final long ts  = (timestampMs > 0 && timestampMs <= now + 5_000L)
        ? timestampMs : now;
    if (ts < now - maxWindowMs) return false; // reject stale

    synchronized (points) {
      points.add(new DataPoint(price, ts));
      points.sort(java.util.Comparator.comparingLong(dp -> dp.timestampMs));
    }

    if (price > globalMax) {
      globalMax = price;
    }
    return true;
  }

  /**
   * Returns an immutable snapshot of all current points for rendering.
   * The snapshot is safe to iterate without holding the store's lock.
   *
   * @return unmodifiable list of data points
   */
  public List<DataPoint> getSnapshot() {
    synchronized (points) {
      return Collections.unmodifiableList(new ArrayList<>(points));
    }
  }

  /**
   * Removes all points with timestamps older than {@code cutoffMs}.
   *
   * @param cutoffMs epoch-ms threshold — points before this are removed
   */
  public void purge(final long cutoffMs) {
    synchronized (points) {
      points.removeIf(p -> p.timestampMs < cutoffMs);
    }
  }

  /**
   * Bulk-loads bid history, filtering by the current window.
   *
   * @param history list of {@link BidTransaction} objects to import
   */
  public void loadHistory(final List<BidTransaction> history) {
    if (history == null || history.isEmpty()) return;
    final long cutoff = System.currentTimeMillis() - maxWindowMs;
    for (final BidTransaction tx : history) {
      if (tx.getTimestamp() > cutoff) {
        addPoint(tx.getBidAmount(), tx.getTimestamp());
      }
    }
  }

  /**
   * Updates the rolling time window. Does NOT automatically purge old points;
   * call {@link #purge} after if immediate eviction is required.
   *
   * @param ms new window duration in milliseconds
   */
  public void setMaxWindowMs(final long ms) {
    this.maxWindowMs = ms;
  }

  /**
   * Returns the current rolling window in milliseconds.
   *
   * @return max window ms
   */
  public long getMaxWindowMs() {
    return maxWindowMs;
  }

  /**
   * Returns the highest price seen across all points ever added to this store.
   *
   * @return global price maximum, or {@link Double#NEGATIVE_INFINITY} if empty
   */
  public double getGlobalMax() {
    return globalMax;
  }

  /**
   * Returns {@code true} if the store currently holds no points.
   *
   * @return {@code true} when empty
   */
  public boolean isEmpty() {
    return points.isEmpty();
  }
}
