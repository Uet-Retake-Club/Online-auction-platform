package com.auction.shared.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("BidIncrementPolicy — unit tests")
class BidIncrementPolicyTest {

  // ── calculate() — tier boundary verification ─────────────────────────────

  @ParameterizedTest(name = "calculate({0}) = {1}")
  @CsvSource({
    "0.0,      5.0",   // floor of tier 1
    "99.0,     5.0",   // top of tier 1
    "99.99,    5.0",   // just below tier 2
    "100.0,   10.0",   // exact boundary → tier 2
    "499.0,   10.0",   // top of tier 2
    "499.99,  10.0",   // just below tier 3
    "500.0,   20.0",   // exact boundary → tier 3
    "999.0,   20.0",   // top of tier 3
    "999.99,  20.0",   // just below tier 4
    "1000.0,  50.0",   // exact boundary → tier 4
    "4999.0,  50.0",   // top of tier 4
    "4999.99, 50.0",   // just below tier 5
    "5000.0, 100.0",   // exact boundary → tier 5
    "9999.0, 100.0",   // top of tier 5
    "9999.99,100.0",   // just below tier 6
    "10000.0,500.0",   // exact boundary → tier 6
    "49999.0,500.0",   // top of tier 6
    "50000.0,1000.0",  // exact boundary → tier 7
    "99999.0,1000.0"   // well into tier 7
  })
  @DisplayName("calculate: tier boundaries map to correct increments")
  void calculate_returnsCorrectTierIncrement(final double price, final double expected) {
    assertEquals(expected, BidIncrementPolicy.calculate(price), 0.001);
  }

  // ── minNextBid() ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("minNextBid: $0 → $5 (tier 1 floor + 0)")
  void minNextBid_atZero() {
    assertEquals(5.0, BidIncrementPolicy.minNextBid(0.0), 0.001);
  }

  @Test
  @DisplayName("minNextBid: $1,240 → $1,290 (tier 4: +$50)")
  void minNextBid_atMidTier4() {
    assertEquals(1_290.0, BidIncrementPolicy.minNextBid(1_240.0), 0.001);
  }

  @Test
  @DisplayName("minNextBid: $50,000 → $51,000 (tier 7: +$1,000)")
  void minNextBid_atTier7() {
    assertEquals(51_000.0, BidIncrementPolicy.minNextBid(50_000.0), 0.001);
  }

  // ── isValid() ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("isValid: accepts bid exactly at minimum threshold")
  void isValid_atExactMinimum() {
    // current=$500 → floor=$20 → min next=$520
    assertTrue(BidIncrementPolicy.isValid(500.0, 520.0));
  }

  @Test
  @DisplayName("isValid: accepts bid above minimum threshold")
  void isValid_aboveMinimum() {
    assertTrue(BidIncrementPolicy.isValid(500.0, 600.0));
  }

  @Test
  @DisplayName("isValid: rejects bid one cent below minimum")
  void isValid_oneCentBelowMinimum() {
    // current=$500 → min=$520; $519.99 should be rejected
    assertFalse(BidIncrementPolicy.isValid(500.0, 519.99));
  }

  @Test
  @DisplayName("isValid: rejects bid equal to current price (no increment)")
  void isValid_equalToCurrentPrice() {
    assertFalse(BidIncrementPolicy.isValid(1_000.0, 1_000.0));
  }

  @Test
  @DisplayName("isValid: rejects bid below current price")
  void isValid_belowCurrentPrice() {
    assertFalse(BidIncrementPolicy.isValid(1_000.0, 900.0));
  }

  // ── isValidIncrement() ────────────────────────────────────────────────────

  @Test
  @DisplayName("isValidIncrement: accepts increment exactly at policy floor")
  void isValidIncrement_atFloor() {
    // current=$1,240 → floor=$50
    assertTrue(BidIncrementPolicy.isValidIncrement(1_240.0, 50.0));
  }

  @Test
  @DisplayName("isValidIncrement: accepts increment above policy floor")
  void isValidIncrement_aboveFloor() {
    assertTrue(BidIncrementPolicy.isValidIncrement(1_240.0, 100.0));
  }

  @Test
  @DisplayName("isValidIncrement: rejects increment below policy floor")
  void isValidIncrement_belowFloor() {
    // current=$1,240 → floor=$50; $20 is too low
    assertFalse(BidIncrementPolicy.isValidIncrement(1_240.0, 20.0));
  }

  @Test
  @DisplayName("isValidIncrement: rejects $5 increment on a $500 item (floor=$20)")
  void isValidIncrement_tier3Floor() {
    assertFalse(BidIncrementPolicy.isValidIncrement(500.0, 5.0));
  }

  @Test
  @DisplayName("isValidIncrement: rejects zero increment")
  void isValidIncrement_zero() {
    assertFalse(BidIncrementPolicy.isValidIncrement(100.0, 0.0));
  }
}
