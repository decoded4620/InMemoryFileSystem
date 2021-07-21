package com.material.server;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The File Server accepts connections from {@link com.material.client.FileSystemClient} objects
 * and handles client requests.
 */
public class FileServer {
  private static final Logger LOG = LoggerFactory.getLogger(FileServer.class);

  private final ExecutorService _serverExecutor = Executors.newSingleThreadExecutor();
  private final int _portNumber;

  public FileServer() {
    this(0);
  }

  public FileServer(int portNumber) {
    _portNumber = portNumber;
  }

  /**
   * Main Program Entry point
   * @param args a String[]
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("You must specify a port on which to launch the Server");
    }
    int portNumber = Integer.parseInt(args[0]);

    FileServer fileServer = new FileServer(portNumber);
    Runtime.getRuntime().addShutdownHook(new Thread(fileServer::shutdown));
    LOG.info("Starting file server on port: " + portNumber);
    fileServer.start();
  }

  public void start() {
    CompletionService<Void> service = new ExecutorCompletionService<>(_serverExecutor);

    service.submit(() -> {
      new Server(_portNumber).run();
      return null;
    });

    try {
      Future<Void> completed = service.take();
      completed.get();
    } catch (ExecutionException | InterruptedException ex) {
      _serverExecutor.shutdownNow();
    }
  }

  public void shutdown() {
    _serverExecutor.shutdownNow();
  }

  public boolean isRunning() {
    return !_serverExecutor.isShutdown() && !_serverExecutor.isTerminated();
  }
}
