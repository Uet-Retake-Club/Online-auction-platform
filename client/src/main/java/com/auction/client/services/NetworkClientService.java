package com.auction.client.services;

import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * NetworkClientService manages the client's socket connection and message flow.
 */
public class NetworkClientService {

  private static NetworkClientService instance;
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private final Gson gson;
  private volatile boolean isRunning = false;
  private volatile boolean isConnecting = false;

  private String serverHost;
  private int serverPort;

  private final List<ServerMessageListener> listeners = new ArrayList<>();

  /**
   * Listener interface for messages sent by the server.
   */
  public interface ServerMessageListener {
    /**
     * Called when a server response is received.
     *
     * @param response parsed response message
     */
    void onMessageReceived(Response response);
  }

  private NetworkClientService() {
    this.gson = new Gson();
  }

  /**
   * Returns the singleton NetworkClientService instance.
   *
   * @return network client singleton
   */
  public static NetworkClientService getInstance() {
    if (instance == null) {
      synchronized (NetworkClientService.class) {
        if (instance == null) {
          instance = new NetworkClientService();
        }
      }
    }
    return instance;
  }

  /**
   * Adds a listener for messages received from the server.
   *
   * @param listener callback listener
   */
  public void addListener(final ServerMessageListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * Returns whether the client is currently connected.
   *
   * @return true if the socket is open and the client is running
   */
  public boolean isConnected() {
    return isRunning && socket != null && !socket.isClosed();
  }

  /**
   * Connects to the server and retries until the connection succeeds.
   *
   * @param host server host
   * @param port server port
   */
  public void connect(final String host, final int port) {
    this.serverHost = host;
    this.serverPort = port;
    attemptConnection();
  }

  private void attemptConnection() {
    if (isConnecting) {
      return;
    }
    isConnecting = true;

    new Thread(() -> {
      int retryCount = 0;
      final int maxRetries = 10;
      while (retryCount < maxRetries && !isRunning) {
        try {
          socket = new Socket(serverHost, serverPort);
          out = new PrintWriter(socket.getOutputStream(), true);
          in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          isRunning = true;
          isConnecting = false;

          System.out.println("[CLIENT] Da ket noi den Server: " + serverHost + ":" + serverPort);
          startListeningThread();
          return;
        } catch (Exception e) {
          retryCount++;
          System.err.println("[CLIENT] Ket noi that bai (lan " + retryCount 
              + "/" + maxRetries + "): " + e.getMessage());
          if (retryCount < maxRetries) {
            try {
              Thread.sleep(2000);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              break;
            }
          }
        }
      }
      isConnecting = false;
      if (!isRunning) {
        System.err.println("[CLIENT] Khong the ket noi Server sau " 
            + maxRetries + " lan thu. Hay khoi dong Server truoc.");
      }
    }, "Client-Connect-Thread").start();
  }

  /**
   * Ensures the client is connected, retrying if needed.
   */
  public void ensureConnected() {
    if (!isConnected() && !isConnecting) {
      attemptConnection();
    }
  }

  private void startListeningThread() {
    new Thread(() -> {
      try {
        String line;
        while (isRunning && (line = in.readLine()) != null) {
          final Response response = gson.fromJson(line, Response.class);
          for (ServerMessageListener listener : listeners) {
            listener.onMessageReceived(response);
          }
        }
      } catch (Exception e) {
        if (isRunning) {
          System.err.println("[CLIENT] Mat ket noi toi Server.");
          isRunning = false;
        }
      }
    }, "Client-Listen-Thread").start();
  }

  /**
   * Sends a request to the server over the current socket connection.
   *
   * @param request request object to send
   */
  public void sendRequest(final Request request) {
    if (out != null && isRunning) {
      final String jsonRequest = gson.toJson(request);
      out.println(jsonRequest);
    } else {
      System.err.println("[CLIENT] Khong the gui, Socket chua ket noi!");
    }
  }

  /**
   * Disconnects the client and closes socket resources.
   */
  public void disconnect() {
    isRunning = false;
    try {
      if (out != null) {
        out.close();
      }
      if (in != null) {
        in.close();
      }
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
      System.out.println("[CLIENT] Da ngat ket noi an toan.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}