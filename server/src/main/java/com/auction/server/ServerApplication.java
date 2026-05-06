package com.auction.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Online Auction Server...");
        
        // Add server startup logic here (e.g. database init, socket listeners)
        
        logger.info("Server is running. Press Ctrl+C to stop.");
        
        // Keep the server alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.error("Server interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
