package com.auction.shared.utils;

/**
 * Stateless policy for computing the minimum bid increment at any price point.
 *
 * <p>All methods are pure functions: they accept the <em>current</em> highest bid price
 * (not the starting price) and return a deterministic result. The tier table is:
 *
 * <pre>
 *  Current Price          Min Increment
 *  ──────────────────────────────────
 *  $0       – $99.99      $5
 *  $100     – $499.99     $10
 *  $500     – $999.99     $20
 *  $1,000   – $4,999.99   $50
 *  $5,000   – $9,999.99   $100
 *  $10,000  – $49,999.99  $500
 *  $50,000+               $1,000
 * </pre>
 *
 * <p>Usage:
 * <pre>
 *   double floor   = BidIncrementPolicy.calculate(currentPrice);
 *   double minNext = BidIncrementPolicy.minNextBid(currentPrice);
 *   boolean ok     = BidIncrementPolicy.isValid(currentPrice, bidAmount);
 * </pre>
 */
public final class BidIncrementPolicy {

  private BidIncrementPolicy() {
    // utility class — no instances
  }

  /**
   * Returns the minimum allowed bid increment for the given current price.
   *
   * <p>The returned value is the <em>step floor</em>: any manual or auto-bid
   * increment must be at least this large.
   *
   * @param currentPrice the current highest bid price (≥ 0)
   * @return minimum increment in the same currency unit
   */
  public static double calculate(final double currentPrice) {
    if (currentPrice <     100.0) return    5.0;
    if (currentPrice <     500.0) return   10.0;
    if (currentPrice <   1_000.0) return   20.0;
    if (currentPrice <   5_000.0) return   50.0;
    if (currentPrice <  10_000.0) return  100.0;
    if (currentPrice <  50_000.0) return  500.0;
    return 1_000.0;
  }

  /**
   * Returns the minimum valid next bid price (current price + policy floor).
   *
   * @param currentPrice the current highest bid price
   * @return the smallest amount a new bid may be placed at
   */
  public static double minNextBid(final double currentPrice) {
    return currentPrice + calculate(currentPrice);
  }

  /**
   * Returns {@code true} if {@code bidAmount} meets or exceeds the minimum
   * next bid required for {@code currentPrice}.
   *
   * <p>A bid is valid when {@code bidAmount >= currentPrice + calculate(currentPrice)}.
   *
   * @param currentPrice the current highest bid price
   * @param bidAmount    the amount the user wishes to bid
   * @return {@code true} if the bid is at or above the minimum threshold
   */
  public static boolean isValid(final double currentPrice, final double bidAmount) {
    return bidAmount >= minNextBid(currentPrice);
  }

  /**
   * Returns {@code true} if {@code increment} is at or above the policy floor
   * for the given current price.
   *
   * <p>Used to validate the auto-bid increment setting before activation. The
   * increment must satisfy {@code increment >= calculate(currentPrice)}.
   *
   * @param currentPrice the current highest bid price
   * @param increment    the auto-bid step the user wishes to configure
   * @return {@code true} if the increment is valid
   */
  public static boolean isValidIncrement(final double currentPrice, final double increment) {
    return increment >= calculate(currentPrice);
  }
}
