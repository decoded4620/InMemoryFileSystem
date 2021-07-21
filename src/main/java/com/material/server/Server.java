package com.material.server;

import com.material.filesystem.DefaultFileSystem;
import com.material.filesystem.FileSystem;
import com.material.filesystem.user.DefaultUserManager;
import com.material.filesystem.user.UserManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Server implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(Server.class);
  // TODO - this composition approach could be done differently, but it is simple for demo purpose
  public final ClientInputProcessor _cliServer;
  AtomicBoolean running = new AtomicBoolean(true);
  // handle client connections
  ExecutorService _requestHandler = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
  private int _portNumber = 0;

  public Server(int portNumber) {
    _portNumber = portNumber;
    UserManager um = new DefaultUserManager();
    FileSystem fs = new DefaultFileSystem(um);
    _cliServer = new ClientInputProcessor(new FileSystemCommandMapBuilder(fs, um));
  }

  private void spawnClientHandler(Socket clientSocket) {
    LOG.info("Handling client: " + clientSocket.getPort());
    _requestHandler.submit(
        new ClientConnectionHandler(clientSocket, _cliServer::processInput, _cliServer::onClientClosed));
  }

  private Socket getClientSocket(ServerSocket serverSocket) throws IOException {
    LOG.info("Waiting for client connection...");
    Socket clientSocket = serverSocket.accept();
    LOG.info("Client connected " + clientSocket.getPort() + " : " + clientSocket.getLocalPort());
    return clientSocket;
  }

  @Override
  public void run() {
    // run the server
    LOG.info("Started server on port: " + _portNumber);
    try {
      ServerSocket serverSocket = new ServerSocket(_portNumber);
      while (running.get()) {
        spawnClientHandler(getClientSocket(serverSocket));
      }
    } catch (IOException ex) {
      LOG.error("IOException", ex);
      // could not get connection
      running.set(false);
    }

    _requestHandler.shutdown();
  }
}
