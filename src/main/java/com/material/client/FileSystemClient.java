package com.material.client;

import com.material.server.FileServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This client connects to the {@link FileServer} and sends user input, and receives server responses. Responses
 * are printed to the console.
 */
public class FileSystemClient {
  final Logger LOG = LoggerFactory.getLogger(FileSystemClient.class);
  private final String _hostName;
  private final int _port;
  private final InputStream _inputStream;

  public FileSystemClient(InputStream inputStream, String hostName, int _port) {
    this._hostName = hostName;
    this._port = _port;
    this._inputStream = inputStream;
  }

  public static void main(String[] args) {
    String hostName = args[0]; // e.g. 127.0.0.1
    int portNumber = Integer.parseInt(args[1]); // e.g. 4959

    FileSystemClient client = new FileSystemClient(System.in, hostName, portNumber);
    client.connect();
  }

  public void connect() {

    try (Socket serverConnection = new Socket(_hostName, _port);
        PrintWriter out = new PrintWriter(serverConnection.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()))) {

      LOG.info("Starting file system client... connecting to server on " + _hostName + ":" + _port);

      BufferedReader stdIn = new BufferedReader(new InputStreamReader(this._inputStream));
      String fromServer;
      String fromUser;

      boolean readingLines = false;
      StringBuilder linesBuilder = new StringBuilder();
      while ((fromServer = in.readLine()) != null) {

        if ("START_LINES".equals(fromServer)) {
          readingLines = true;
          linesBuilder = new StringBuilder();
          continue;
        }

        if (readingLines) {
          if ("END_LINES".equals(fromServer)) {
            readingLines = false;
            LOG.info(linesBuilder.toString());
          } else {
            linesBuilder.append(fromServer).append("\n");
            continue;
          }
        }
        LOG.info("> " + fromServer);

        fromUser = stdIn.readLine();
        if (fromUser != null) {
          out.println(fromUser);
        } else {
          LOG.info("Last user command read");
          break;
        }
      }

      LOG.info("Client closing connection");
    } catch (UnknownHostException e) {
      LOG.error("Bad host " + _hostName, e);
      System.exit(1);
    } catch (IOException e) {
      LOG.error("I/O could not be obtained for host: " + _hostName, e);
      System.exit(1);
    }
  }
}
