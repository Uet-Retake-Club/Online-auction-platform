package com.auction.client.services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings;
import com.auction.shared.models.BidTransaction;
import com.google.gson.Gson;

import javafx.application.Platform;

/**
 * BidService routes bid actions from the UI to the server and updates listeners.
 */
public class BidService implements NetworkClientService.ServerMessageListener {

  private static BidService instance;
  private double currentBidAmount = 0.0;
  private final double minimumIncrement = 20.00;
  private boolean isAuctionOpen = true;

  private final List<BidTransaction> bidHistory = new ArrayList<>();
  private final Gson gson = new Gson();

  public List<BidTransaction> getBidHistory() {
    synchronized (bidHistory) {
      return new ArrayList<>(bidHistory);
    }
  }

  private Consumer<Double> onPriceUpdated;
  private Consumer<BidTransaction> onNewBid;
  private Consumer<Response> onAutoBidResult;
  private Consumer<String> onBidError;
  private Consumer<Double> onWalletBalanceUpdated;
  private Consumer<Long> onEndTimeReceived;
  private final List<Consumer<Double>> walletBalanceListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

  public void addWalletBalanceListener(final Consumer<Double> listener) {
    if (listener != null) {
      walletBalanceListeners.add(listener);
    }
  }

  public void removeWalletBalanceListener(final Consumer<Double> listener) {
    if (listener != null) {
      walletBalanceListeners.remove(listener);
    }
  }

  private BidService() {
    NetworkClientService.getInstance().addListener(this);
  }

  /**
   * Returns the singleton BidService instance.
   *
   * @return bid service instance
   */
  public static BidService getInstance() {
    if (instance == null) {
      synchronized (BidService.class) {
        if (instance == null) {
          instance = new BidService();
        }
      }
    }
    return instance;
  }

  /**
   * Sets callbacks for price updates and new bid notifications.
   *
   * @param onPriceUpdated callback when the price changes
   * @param onNewBid callback when a new bid arrives
   */
  public void setCallbacks(final Consumer<Double> onPriceUpdated,
      final Consumer<BidTransaction> onNewBid) {
    this.onPriceUpdated = onPriceUpdated;
    this.onNewBid = onNewBid;
  }



  /**
   * Sets the callback to receive auto-bid result responses.
   *
   * @param callback response callback
   */
  public void setOnAutoBidResult(final Consumer<Response> callback) {
    this.onAutoBidResult = callback;
  }

  /**
   * Sets the callback for bid error messages.
   *
   * @param callback error callback
   */
  public void setOnBidError(final Consumer<String> callback) {
    this.onBidError = callback;
  }

  /**
   * Sets the callback for wallet balance updates.
   *
   * @param callback balance callback
   */
  public void setOnWalletBalanceUpdated(final Consumer<Double> callback) {
    this.onWalletBalanceUpdated = callback;
  }

  public void setOnEndTimeReceived(final Consumer<Long> callback) {
    this.onEndTimeReceived = callback;
  }

  /**
   * Returns the current highest bid amount.
   *
   * @return current bid amount
   */
  public double getCurrentBidAmount() {
    return currentBidAmount;
  }

  /**
   * Returns the minimum bid increment.
   *
   * @return minimum increment value
   */
  public double getMinimumIncrement() {
    return minimumIncrement;
  }

  /**
   * Resets bid state when the user navigates to a new auction item.
   * Clears the stale price so the next GET_STATUS response populates it fresh.
   */
  public void resetForItem() {
    this.currentBidAmount = 0.0;
    this.isAuctionOpen = true;
    this.bidHistory.clear();
    this.onPriceUpdated = null;
    this.onNewBid = null;
    this.onAutoBidResult = null;
    this.onBidError = null;
    this.onWalletBalanceUpdated = null;
    this.onEndTimeReceived = null;
  }

  /**
   * Marks the auction as closed and prevents further bidding.
   */
  public void setAuctionClosed() {
    this.isAuctionOpen = false;
  }

  /**
   * Sends a bid request to the server for the given auction.
   *
   * @param bidderId bidder identifier
   * @param auctionId auction identifier
   * @param amount bid amount
   * @return null if the request was sent, or an error message
   */
  public String placeBid(final String bidderId, final String auctionId,
      final double amount) {
    if (!isAuctionOpen) {
      return "Auction has ended.";
    }

    final BidTransaction transaction = new BidTransaction("", auctionId, bidderId, 
        amount, System.currentTimeMillis());
    final String payload = gson.toJson(transaction);

    final Request req = new Request(MessageType.PLACE_BID, 
        UserSession.getInstance().getUserId(), payload);
    NetworkClientService.getInstance().sendRequest(req);

    return null; 
  }

  /**
   * Sends a request to register auto-bid settings.
   *
   * @param bidderId bidder identifier
   * @param auctionId auction identifier
   * @param maxPrice maximum auto-bid price
   * @param bidIncrement bid step amount
   * @param aggressiveMode true for aggressive auto-bidding
   * @return null if the request was sent, or an error message
   */
  public String setupAutoBid(final String bidderId, final String auctionId,
      final double maxPrice, final double bidIncrement,
      final boolean aggressiveMode) {
    if (!isAuctionOpen) {
      return "Auction has ended.";
    }

    final AutoBidSettings settings = new AutoBidSettings(
        bidderId, auctionId, maxPrice, bidIncrement, aggressiveMode);
    final String payload = gson.toJson(settings);

    final Request req = new Request(MessageType.SETUP_AUTO_BID,
        UserSession.getInstance().getUserId(), payload);
    NetworkClientService.getInstance().sendRequest(req);

    return null;
  }

  /**
   * Sends a request to refresh the current auction status for the given item.
   */
  public void requestStatus() {
    final String itemId = UserSession.getInstance().getSelectedItemId();
    final Request req = new Request(MessageType.GET_STATUS,
        UserSession.getInstance().getUserId(), itemId != null ? itemId : "");
    NetworkClientService.getInstance().sendRequest(req);
  }

  @Override
  public void onMessageReceived(final Response response) {
    if (response.getType() == MessageType.NEW_BID_BROADCAST
        || response.getType() == MessageType.BID_SUCCESS) {

      final String msg = response.getMessage();
      if (msg != null && !msg.isEmpty()) {
        try {
          final long endTime = Long.parseLong(msg);
          if (endTime > 0) {
            Platform.runLater(() -> {
              if (onEndTimeReceived != null) {
                onEndTimeReceived.accept(endTime);
              }
            });
          }
        } catch (NumberFormatException ignored) {}
      }

      if (response.getPayload() == null || response.getPayload().isEmpty()) {
        return;
      }

      final String payload = response.getPayload();
      if (payload.startsWith("[")) {
        // Handle history array
        try {
          final BidTransaction[] history = gson.fromJson(payload, BidTransaction[].class);
          if (history != null && history.length > 0) {
            this.bidHistory.clear();
            for (BidTransaction tx : history) {
              bidHistory.add(tx);
            }
            final BidTransaction latest = history[history.length - 1];
            this.currentBidAmount = latest.getBidAmount();
            
            Platform.runLater(() -> {
              if (onPriceUpdated != null) {
                onPriceUpdated.accept(currentBidAmount);
              }
              if (onNewBid != null) {
                // For initial load, we might want to notify about all bids or just the latest
                // The AuctionDetailController usually adds one row at a time.
                // Let's notify listeners about each bid in the history so they show up.
                for (BidTransaction tx : history) {
                  onNewBid.accept(tx);
                }
              }
            });
          } else {
            this.currentBidAmount = UserSession.getInstance().getSelectedItemPrice();
            Platform.runLater(() -> {
              if (onPriceUpdated != null) {
                onPriceUpdated.accept(currentBidAmount);
              }
            });
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        // Handle single bid
        final BidTransaction newBid;
        try {
          newBid = gson.fromJson(payload, BidTransaction.class);
        } catch (Exception e) {
          return;
        }
        if (newBid == null) return;

        final String viewingId = UserSession.getInstance().getSelectedItemId();
        if (viewingId != null && newBid.getItemId() != null
            && !viewingId.equals(newBid.getItemId())) {
          return;
        }

        synchronized (bidHistory) {
          boolean alreadyExists = false;
          if (newBid.getId() != null && !newBid.getId().isEmpty()) {
            for (BidTransaction existing : bidHistory) {
              if (newBid.getId().equals(existing.getId())) {
                alreadyExists = true;
                break;
              }
            }
          }
          if (alreadyExists) {
            return;
          }
          this.currentBidAmount = newBid.getBidAmount();
          bidHistory.add(newBid);
        }

        Platform.runLater(() -> {
          if (onPriceUpdated != null) {
            onPriceUpdated.accept(currentBidAmount);
          }
          if (onNewBid != null) {
            onNewBid.accept(newBid);
          }
        });
      }

    } else if (response.getType() == MessageType.SETUP_AUTO_BID) {
      Platform.runLater(() -> {
        if (onAutoBidResult != null) {
          onAutoBidResult.accept(response);
        }
      });

    } else if (response.getType() == MessageType.AUCTION_ENDED) {
      this.isAuctionOpen = false;

    } else if (response.getType() == MessageType.BID_ERROR) {
      Platform.runLater(() -> {
        if (onBidError != null) {
          onBidError.accept(response.getMessage());
        }
      });

    } else if (response.getType() == MessageType.TIME_EXTENDED) {
      if (response.getPayload() != null && !response.getPayload().isEmpty()) {
        try {
          final long newEndTime = Long.parseLong(response.getPayload());
          Platform.runLater(() -> {
            if (onEndTimeReceived != null) {
              onEndTimeReceived.accept(newEndTime);
            }
          });
          System.out.println("[CLIENT] Received time extension! New end time: " + newEndTime);
        } catch (NumberFormatException e) {
          System.err.println("[CLIENT] Error parsing TIME_EXTENDED payload: " + e.getMessage());
        }
      }

    } else if (response.getType() == MessageType.WALLET_BALANCE_RESPONSE) {
      try {
        UserSession.getInstance().setWalletBalance(Double.parseDouble(response.getPayload()));
      } catch (Exception ignored) {}
      Platform.runLater(() -> {
        try {
          final double balance = Double.parseDouble(response.getPayload());
          if (onWalletBalanceUpdated != null) {
            onWalletBalanceUpdated.accept(balance);
          }
          for (final Consumer<Double> listener : walletBalanceListeners) {
            listener.accept(balance);
          }
        } catch (Exception ignored) {}
      });
    }
  }
}