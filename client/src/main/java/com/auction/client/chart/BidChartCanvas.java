package com.auction.client.chart;

import com.auction.shared.models.BidTransaction;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * BidChartCanvas — a self-contained JavaFX Canvas component that renders a
 * real-time rolling-window line chart of bid price history.
 *
 * <p>Usage:
 * <pre>
 *   BidChartCanvas chart = new BidChartCanvas("item-001", 600_000L);
 *   chart.setOnNewHighBid(bid -&gt; System.out.println("New high: " + bid.getBidAmount()));
 *   chart.addDataPoints(BidService.getInstance().getBidHistory());
 *
 *   // On every new bid from BidService.onNewBid callback:
 *   chart.addDataPoint(bid.getBidAmount(), bid.getTimestamp());
 * </pre>
 *
 * <p>Internally delegates to:
 * <ul>
 *   <li>{@link BidDataStore}  — thread-safe data management</li>
 *   <li>{@link ChartRenderer} — all canvas drawing</li>
 *   <li>{@link ChartTooltip}  — mouse-hover tooltip</li>
 * </ul>
 */
public final class BidChartCanvas {

  // ── Fields ───────────────────────────────────────────────────────────────
  private final String  itemId;
  private Consumer<BidTransaction> onNewHighBid;

  private final BidDataStore   store;
  private final ChartRenderer  renderer;

  /** Guards against scheduling more than one redraw per JavaFX pulse. */
  private volatile boolean redrawPending = false;

  // ── JavaFX nodes ─────────────────────────────────────────────────────────
  private final Canvas    canvas;
  private final VBox      root;
  private final Label     emptyLabel;
  private final StackPane chartStack;

  /** Mock-data feed (only active in dev/test mode). */
  private Timeline mockTimeline;

  // ── Constructor ──────────────────────────────────────────────────────────

  /**
   * Creates a new BidChartCanvas.
   *
   * @param itemId      the auction item identifier (used for new-high callbacks)
   * @param maxWindowMs the rolling time-window in milliseconds (default 600 000 = 10 min)
   */
  public BidChartCanvas(final String itemId, final long maxWindowMs) {
    this.itemId = itemId;
    store        = new BidDataStore(maxWindowMs);

    // ── Canvas ────────────────────────────────────────────────────────────
    canvas = new Canvas(640, 220);
    final GraphicsContext gc = canvas.getGraphicsContext2D();
    renderer = new ChartRenderer(gc, false);

    // ── Empty state label ─────────────────────────────────────────────────
    emptyLabel = new Label("⏳  Waiting for bids...");
    emptyLabel.getStyleClass().add("bid-chart-empty");
    emptyLabel.setStyle(
        "-fx-font-size: 13px;"
        + "-fx-text-fill: -text-muted;"
        + "-fx-font-style: italic;");
    emptyLabel.setMouseTransparent(true);

    // ── Chart stack (canvas + empty label) ───────────────────────────────
    chartStack = new StackPane(canvas, emptyLabel);
    chartStack.setAlignment(Pos.CENTER);

    // Bind canvas to fill the StackPane
    canvas.widthProperty().bind(chartStack.widthProperty());
    canvas.heightProperty().bind(chartStack.heightProperty());

    // Coalesce resize redraws — both width and height fire in the same pulse
    canvas.widthProperty().addListener((obs, o, n) -> scheduleRedraw());
    canvas.heightProperty().addListener((obs, o, n) -> scheduleRedraw());

    // ── Root VBox ─────────────────────────────────────────────────────────
    root = new VBox(chartStack);
    root.getStyleClass().add("bid-chart-card");
    root.setPrefWidth(0);
    root.setMinWidth(200);
    VBox.setVgrow(chartStack, Priority.ALWAYS);

    // Trigger first real redraw after the node joins the live scene
    root.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (newScene != null) {
        Platform.runLater(this::redraw);
      }
    });
  }

  // ── Public API ───────────────────────────────────────────────────────────

  /**
   * Returns the root VBox node to embed in an FXML container.
   *
   * @return the root layout node
   */
  public VBox getRoot() {
    return root;
  }

  /**
   * Adds a single bid data point. Thread-safe — may be called from any thread.
   *
   * @param price       bid price
   * @param timestampMs epoch-millisecond timestamp
   */
  public void addDataPoint(final double price, final long timestampMs) {
    final double prevMax = store.getGlobalMax();
    store.addPoint(price, timestampMs);

    if (price > prevMax && onNewHighBid != null) {
      final BidTransaction syntheticBid = new BidTransaction(
          "", itemId != null ? itemId : "", "Highest Bid", price, timestampMs);
      syntheticBid.setBidderUsername("Highest Bid");
      Platform.runLater(() -> onNewHighBid.accept(syntheticBid));
    }
    scheduleRedraw();
  }

  /**
   * Bulk-loads bid history. Thread-safe — may be called from any thread.
   *
   * @param history list of existing bid transactions
   */
  public void addDataPoints(final List<BidTransaction> history) {
    store.loadHistory(history);
    scheduleRedraw();
  }

  /**
   * Registers a callback invoked whenever a new global price high is detected.
   *
   * @param callback consumer called with the new highest {@link BidTransaction}
   */
  public void setOnNewHighBid(final Consumer<BidTransaction> callback) {
    this.onNewHighBid = callback;
  }

  /**
   * Sets the rolling time window.
   *
   * @param ms window duration in milliseconds
   */
  public void setMaxWindowMs(final long ms) {
    store.setMaxWindowMs(ms);
    scheduleRedraw();
  }

  /**
   * Switches the chart between dark and light rendering.
   *
   * @param dark {@code true} for dark mode
   */
  public void setDarkMode(final boolean dark) {
    renderer.setDarkMode(dark);
    scheduleRedraw();
  }

  /**
   * Starts a mock data feed that emits randomised bids every 1 500 ms.
   * Intended for dev/testing when there is no live server connection.
   */
  public void startMockDataFeed() {
    if (mockTimeline != null) return;
    final double[] lastPrice = {500.0};
    mockTimeline = new Timeline(new KeyFrame(Duration.millis(1500), e -> {
      lastPrice[0] = Math.max(1, lastPrice[0] + Math.random() * 60 - 10);
      addDataPoint(lastPrice[0], System.currentTimeMillis());
    }));
    mockTimeline.setCycleCount(Timeline.INDEFINITE);
    mockTimeline.play();
  }

  /** Stops the mock data feed if it is running. */
  public void stopMockDataFeed() {
    if (mockTimeline != null) {
      mockTimeline.stop();
      mockTimeline = null;
    }
  }

  // ── Rendering ────────────────────────────────────────────────────────────

  /**
   * Coalesces multiple redraw requests into one per JavaFX pulse.
   */
  private void scheduleRedraw() {
    if (!redrawPending) {
      redrawPending = true;
      Platform.runLater(this::redraw);
    }
  }

  /** Full chart redraw. Must run on the JavaFX Application Thread. */
  private void redraw() {
    redrawPending = false;

    final double w = canvas.getWidth();
    final double h = canvas.getHeight();
    if (w <= 0 || h <= 0) return;

    final GraphicsContext gc = canvas.getGraphicsContext2D();
    gc.clearRect(0, 0, w, h);

    // ── Purge stale points and compute Y-axis bounds ───────────────────────
    final long   now    = System.currentTimeMillis();
    final long   cutoff = now - store.getMaxWindowMs();
    store.purge(cutoff);

    final List<DataPoint> snapshot = store.getSnapshot();

    if (snapshot.isEmpty()) {
      emptyLabel.setVisible(true);
      renderer.drawBackground(w, h);
      return;
    }
    emptyLabel.setVisible(false);

    double minPrice = Double.MAX_VALUE;
    double maxPrice = Double.NEGATIVE_INFINITY;
    long   oldestTs = now;
    for (final DataPoint p : snapshot) {
      if (p.price       < minPrice) minPrice = p.price;
      if (p.price       > maxPrice) maxPrice = p.price;
      if (p.timestampMs < oldestTs) oldestTs = p.timestampMs;
    }

    final double priceRange = (maxPrice > minPrice) ? maxPrice - minPrice : 100.0;
    final double pricePad   = Math.max(priceRange * 0.10, 20.0);
    final double yMin       = Math.max(0.0, minPrice - pricePad);
    final double yMax       = maxPrice + pricePad;

    // ── Dynamic time window ───────────────────────────────────────────────
    final long   actualSpan  = now - oldestTs;
    final long   paddedSpan  = Math.max((long) (actualSpan * 1.10), 30_000L);
    final long   windowStart = Math.max(now - paddedSpan, now - store.getMaxWindowMs());
    final double timeRange   = Math.max((double) (now - windowStart), 30_000.0);

    final double plotW = w - ChartRenderer.PAD_LEFT - ChartRenderer.PAD_RIGHT;
    final double plotH = h - ChartRenderer.PAD_TOP  - ChartRenderer.PAD_BOTTOM;
    if (plotW <= 0 || plotH <= 0) return;

    // ── Draw layers ───────────────────────────────────────────────────────
    renderer.drawBackground(w, h);
    renderer.drawGrid(plotW, plotH, windowStart, timeRange);
    renderer.drawLine(snapshot, plotW, plotH, yMin, yMax, windowStart, timeRange);
    renderer.drawAxesLabels(w, h, plotW, plotH, yMin, yMax, windowStart, timeRange);
  }

}
