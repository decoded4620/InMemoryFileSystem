package com.material.filesystem;

import com.material.client.FileSystemClient;
import com.material.filesystem.util.StopWatch;
import com.material.server.FileServer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class FileServerAndClientTest {

  @Test
  public void testBasicInteraction() {
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    FileServer fileServer = new FileServer(4959);

    executorService.submit(() -> {
      fileServer.start();
      while(fileServer.isRunning()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          break;
        }
      }
    });

    createFSClient("src/test/resources/testCommands.txt", "127.0.0.1", 4959);
  }

  @Test
  public void testStressInteraction() {
    ExecutorService executorService = Executors.newFixedThreadPool(4);
    FileServer fileServer = new FileServer(5050);

    CountDownLatch latch = new CountDownLatch(3);

    executorService.submit(() -> {
      fileServer.start();
      while(fileServer.isRunning()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          break;
        }
      }
    });


    StopWatch w = new StopWatch();
    w.start();
    executorService.submit(() -> {
      createFSClient("src/test/resources/client1.txt", "127.0.0.1", 5050);
      latch.countDown();

    });

    executorService.submit(() -> {
      createFSClient("src/test/resources/client2.txt", "127.0.0.1", 5050);
      latch.countDown();
    });

    executorService.submit(() -> {
      createFSClient("src/test/resources/client3.txt", "127.0.0.1", 5050);
      latch.countDown();
    });

    try {
      latch.await();
      double totalRunTime = w.currentTime(TimeUnit.MILLISECONDS);
      w.stop();
      System.out.println("Total time to run all clients: " + totalRunTime + "ms");
    } catch (InterruptedException ex) {
      Assertions.fail(ex);
    }
    fileServer.shutdown();
  }

  private void createFSClient(String fromCommandsFile, String host, int port) {
    try {
      FileInputStream inputStream = new FileInputStream(fromCommandsFile);

      Thread.sleep(1000);
      FileSystemClient client = new FileSystemClient(inputStream, host, port);
      client.connect();

    } catch (FileNotFoundException | InterruptedException ex) {
      //
    }
  }
}
