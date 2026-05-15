package com.auction.server.exceptions;

public class InvalidBidException extends AuctionException {
    public InvalidBidException(String message) {
        super(message);
    }
}