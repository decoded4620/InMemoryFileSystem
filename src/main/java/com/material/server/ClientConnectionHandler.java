package com.material.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This simple class handles a single client connection to the the file server.
 */
public class ClientConnectionHandler implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(ClientConnectionHandler.class);
  private static final AtomicInteger numConnections = new AtomicInteger(0);

  private final Socket _clientSocket;
  private final Function<String, String> _inputProcessor;
  private final Runnable _onCloseHandler;

  /**
   * Constructor
   *
   * @param clientSocket the connected server socket.
   */
  public ClientConnectionHandler(Socket clientSocket, Function<String, String> inputProcessor,
      Runnable onCloseHandler) {
    _clientSocket = clientSocket;
    _inputProcessor = inputProcessor;
    _onCloseHandler = onCloseHandler;
  }

  @Override
  public void run() {
    numConnections.incrementAndGet();
    LOG.info("ClientConnectionHandler started: " + _clientSocket.getPort() + ", active connections: "
        + numConnections.get());
    try (PrintWriter out = new PrintWriter(_clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(_clientSocket.getInputStream()))) {

      // con
      out.println("connected on: " + _clientSocket.getPort());

      String inputLine;

      while ((inputLine = in.readLine()) != null) {
        LOG.debug(_clientSocket.getInetAddress().getHostName() + ":" + _clientSocket.getPort() + " <-- " + inputLine);
        try {
          String response = _inputProcessor.apply(inputLine);
          LOG.debug(_clientSocket.getInetAddress().getHostName() + ":" + _clientSocket.getPort() + " --> " + response);

          if (response.contains("\n")) {
            String[] lines = response.split("\n");
            out.println("START_LINES");
            Arrays.stream(lines).forEach(out::println);
            out.println("END_LINES");
          } else {
            out.println(response);
          }
        } catch (Exception e) {
          LOG.error("Error processing client input", e);
          // TODO - obviously redact any sensitive data before sending to the server
          out.println(e.getMessage());
        }
      }

      _onCloseHandler.run();
      LOG.warn("Closing Client: " + _clientSocket.getPort());
      _clientSocket.close();
      numConnections.decrementAndGet();
      LOG.info("Closed, active connections... " + numConnections.get());
    } catch (IOException ex) {
      LOG.error("Error received for client: " + _clientSocket.getPort(), ex);
    }
  }
}
