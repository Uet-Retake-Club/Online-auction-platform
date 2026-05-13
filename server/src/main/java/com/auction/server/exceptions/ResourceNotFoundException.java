package com.auction.server.exceptions;

public class ResourceNotFoundException extends AuctionException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}