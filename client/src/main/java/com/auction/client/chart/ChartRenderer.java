package com.auction.client.chart;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Stateless renderer for the bid price chart.
 *
 * <p>All draw methods take explicit geometry parameters so this class carries
 * no mutable state beyond the {@link GraphicsContext} reference and the current
 * dark-mode flag, both of which are set at construction time.
 */
public final class ChartRenderer {

  // ── Layout constants (shared with BidChartCanvas) ────────────────────────
  static final double PAD_LEFT   = 68;
  static final double PAD_RIGHT  = 20;
  static final double PAD_TOP    = 24;
  static final double PAD_BOTTOM = 42;

  private static final double GRID_LINES       = 5;
  private static final int    MAX_VISIBLE_PTS  = 300;

  private static final DateTimeFormatter TIME_FMT =
      DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

  private final GraphicsContext gc;
  private boolean isDarkMode;

  /**
   * Constructs a renderer bound to the given {@link GraphicsContext}.
   *
   * @param gc         JavaFX graphics context of the target canvas
   * @param isDarkMode {@code true} to use dark-mode colour palette
   */
  public ChartRenderer(final GraphicsContext gc, final boolean isDarkMode) {
    this.gc         = gc;
    this.isDarkMode = isDarkMode;
  }

  /**
   * Updates the dark-mode flag; takes effect on the next draw call.
   *
   * @param dark {@code true} for dark mode
   */
  public void setDarkMode(final boolean dark) {
    this.isDarkMode = dark;
  }

  /**
   * Fills the chart background and plot-area rectangle.
   *
   * @param w total canvas width
   * @param h total canvas height
   */
  public void drawBackground(final double w, final double h) {
    final Color bg = isDarkMode ? Color.web("#1A1C26") : Color.web("#F8F9FA");
    gc.setFill(bg);
    gc.fillRoundRect(0, 0, w, h, 12, 12);

    final Color plotBg = isDarkMode ? Color.web("#0F111A", 0.6) : Color.web("#FFFFFF", 0.7);
    gc.setFill(plotBg);
    gc.fillRect(PAD_LEFT, PAD_TOP, w - PAD_LEFT - PAD_RIGHT, h - PAD_TOP - PAD_BOTTOM);
  }

  /**
   * Draws horizontal (price) and vertical (time) grid lines inside the plot area.
   *
   * @param plotW       plot area width
   * @param plotH       plot area height
   * @param windowStart start of the visible time window (epoch ms)
   * @param timeRange   duration of the visible time window (ms)
   */
  public void drawGrid(final double plotW, final double plotH,
                       final long windowStart, final double timeRange) {
    final Color gridColor = isDarkMode ? Color.web("#2D3142", 0.9) : Color.web("#E9ECEF", 0.9);
    gc.setStroke(gridColor);
    gc.setLineWidth(0.8);
    gc.setLineDashes();

    for (int i = 0; i <= (int) GRID_LINES; i++) {
      final double y = PAD_TOP + plotH - (i / GRID_LINES) * plotH;
      gc.strokeLine(PAD_LEFT, y, PAD_LEFT + plotW, y);
    }
    for (int i = 0; i <= 5; i++) {
      final double x = PAD_LEFT + (i / 5.0) * plotW;
      gc.strokeLine(x, PAD_TOP, x, PAD_TOP + plotH);
    }

    final Color borderColor = isDarkMode ? Color.web("#2D3142") : Color.web("#DEE2E6");
    gc.setStroke(borderColor);
    gc.setLineWidth(1.0);
    gc.strokeRect(PAD_LEFT, PAD_TOP, plotW, plotH);
  }

  /**
   * Draws the bid price line as a single polyline path with a subtle fill
   * under the curve. Points are downsampled to at most {@code MAX_VISIBLE_PTS}
   * before drawing to prevent performance degradation with rapid auto-bids.
   *
   * @param points      snapshot of data points (already purged and sorted)
   * @param plotW       plot area width
   * @param plotH       plot area height
   * @param yMin        minimum Y-axis price value (with padding)
   * @param yMax        maximum Y-axis price value (with padding)
   * @param windowStart start of the visible time window (epoch ms)
   * @param timeRange   duration of the visible time window (ms, > 0)
   */
  public void drawLine(final List<DataPoint> points,
                       final double plotW, final double plotH,
                       final double yMin, final double yMax,
                       final long windowStart, final double timeRange) {
    if (points.isEmpty()) return;

    final double ySpan   = yMax - yMin;
    final List<DataPoint> visible = downsample(points, MAX_VISIBLE_PTS);

    gc.setStroke(Color.web("#4FC3F7"));
    gc.setLineWidth(2.0);
    gc.setLineDashes();
    gc.setLineCap(StrokeLineCap.ROUND);
    gc.setLineJoin(StrokeLineJoin.ROUND);

    // ── Single polyline path ───────────────────────────────────────────────
    gc.beginPath();
    boolean first = true;
    for (final DataPoint dp : visible) {
      final double cx = PAD_LEFT + ((double) (dp.timestampMs - windowStart) / timeRange) * plotW;
      final double cy = PAD_TOP + plotH - ((dp.price - yMin) / ySpan) * plotH;
      if (first) { gc.moveTo(cx, cy); first = false; }
      else        { gc.lineTo(cx, cy); }
    }
    gc.stroke();

    // ── Gradient fill under the line ───────────────────────────────────────
    // Extend path to bottom corners, close, and fill with very subtle tint.
    gc.lineTo(PAD_LEFT + plotW, PAD_TOP + plotH);
    gc.lineTo(PAD_LEFT,         PAD_TOP + plotH);
    gc.closePath();
    gc.setFill(Color.web("#4FC3F7", 0.08));
    gc.fill();
  }

  /**
   * Draws Y-axis price labels and X-axis time labels.
   *
   * @param w           total canvas width
   * @param h           total canvas height
   * @param plotW       plot area width
   * @param plotH       plot area height
   * @param yMin        minimum Y-axis value
   * @param yMax        maximum Y-axis value
   * @param windowStart start of the visible time window (epoch ms)
   * @param timeRange   duration of the visible time window (ms)
   */
  public void drawAxesLabels(final double w, final double h,
                             final double plotW, final double plotH,
                             final double yMin, final double yMax,
                             final long windowStart, final double timeRange) {
    final Color labelColor = isDarkMode ? Color.web("#ADB5BD") : Color.web("#6C757D");
    gc.setFill(labelColor);
    gc.setFont(Font.font("Inter", FontWeight.NORMAL, 10.5));

    final double ySpan = yMax - yMin;

    for (int i = 0; i <= (int) GRID_LINES; i++) {
      final double price = yMin + (i / GRID_LINES) * ySpan;
      final double y     = PAD_TOP + plotH - (i / GRID_LINES) * plotH;
      gc.fillText(formatPrice(price), 4, y + 4);
    }
    for (int i = 0; i <= 5; i++) {
      final long   ts    = windowStart + (long) ((i / 5.0) * timeRange);
      final double x     = PAD_LEFT + (i / 5.0) * plotW;
      final String label = TIME_FMT.format(Instant.ofEpochMilli(ts));
      gc.fillText(label, x - 20, h - PAD_BOTTOM + 14);
    }
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  /**
   * Downsamples {@code src} to at most {@code maxPts} evenly-spaced points.
   * Returns {@code src} unchanged when it is already within the limit.
   *
   * @param src    source point list
   * @param maxPts target maximum number of points
   * @return downsampled list (or the original list when no reduction needed)
   */
  private List<DataPoint> downsample(final List<DataPoint> src, final int maxPts) {
    if (src.size() <= maxPts) return src;
    final List<DataPoint> result = new ArrayList<>(maxPts);
    final double step = (double) (src.size() - 1) / (maxPts - 1);
    for (int i = 0; i < maxPts; i++) {
      result.add(src.get((int) Math.round(i * step)));
    }
    return result;
  }

  /**
   * Formats a price value for Y-axis labels using K/M suffixes for large values.
   *
   * @param price the price value
   * @return formatted string, e.g. {@code "$1.2K"}
   */
  private static String formatPrice(final double price) {
    if (price >= 1_000_000) return String.format("$%.1fM", price / 1_000_000);
    if (price >= 1_000)     return String.format("$%.1fK", price / 1_000);
    return String.format("$%.0f", price);
  }
}
