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

  private Consumer<Double> onPriceUpdated;
  private Consumer<BidTransaction> onNewBid;
  private Consumer<String> onPriceChangeNotification; 
  private Consumer<Response> onAutoBidResult;  
  private Consumer<String> onBidError;         

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
   * Sets the toast-style notification callback for price changes.
   *
   * @param callback notification callback
   */
  public void setOnPriceChangeNotification(final Consumer<String> callback) {
    this.onPriceChangeNotification = callback;
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
      return "Phiên đấu giá đã đóng.";
    }

    final BidTransaction transaction = new BidTransaction("", auctionId, bidderId, 
        amount, System.currentTimeMillis());
    final String payload = gson.toJson(transaction);

    final Request req = new Request(MessageType.PLACE_BID, 
        UserSession.getInstance().getUsername(), payload);
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
      return "Phiên đấu giá đã đóng.";
    }

    final AutoBidSettings settings = new AutoBidSettings(
        bidderId, auctionId, maxPrice, bidIncrement, aggressiveMode);
    final String payload = gson.toJson(settings);

    final Request req = new Request(MessageType.SETUP_AUTO_BID,
        UserSession.getInstance().getUsername(), payload);
    NetworkClientService.getInstance().sendRequest(req);

    return null;
  }

  /**
   * Sends a request to refresh the current auction status.
   */
  public void requestStatus() {
    final Request req = new Request(MessageType.GET_STATUS,
        UserSession.getInstance().getUsername(), "");
    NetworkClientService.getInstance().sendRequest(req);
  }

  @Override
  public void onMessageReceived(final Response response) {
    if (response.getType() == MessageType.NEW_BID_BROADCAST 
        || response.getType() == MessageType.BID_SUCCESS) {

      if (response.getPayload() == null || response.getPayload().isEmpty()) {
        return;
      }

      final BidTransaction newBid = gson.fromJson(response.getPayload(), BidTransaction.class);
      this.currentBidAmount = newBid.getBidAmount();
      bidHistory.add(newBid);

      Platform.runLater(() -> {
        if (onPriceUpdated != null) {
          onPriceUpdated.accept(currentBidAmount);
        }
        if (onNewBid != null) {
          onNewBid.accept(newBid);
        }
        if (onPriceChangeNotification != null) {
          final String msg = String.format("Price updated: $%.2f by %s",
              newBid.getBidAmount(), newBid.getBidderId());
          onPriceChangeNotification.accept(msg);
        }
      });

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
    }
  }
}