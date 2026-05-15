package com.auction.server.exceptions;

public class DatabaseOperationException extends AuctionException {
    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}