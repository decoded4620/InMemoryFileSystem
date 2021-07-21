package com.material.filesystem.util;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handles Concurrent Operations during a test.
 */
public class ConcurrentOperationTest {
  private static final Logger LOG = LoggerFactory.getLogger(ConcurrentOperationTest.class);
  private final ExecutorService _executorService;
  public ConcurrentOperationTest(ExecutorService executorService) {
    _executorService = executorService;
  }

  public void submitTasks(Collection<Runnable> tasks, int timeToWaitMs) {
    CountDownLatch latch = new CountDownLatch(tasks.size());
    LOG.info("Submitted " + tasks.size() + " runnable tasks for concurrent operations test");
    tasks.forEach(task  -> _executorService.submit(() -> {
      try {
        LOG.info("Running task for concurrent operations test");
        task.run();
      } finally {
        latch.countDown();
      }
    }));

    _executorService.shutdown();

    try {
      if(!latch.await(timeToWaitMs, TimeUnit.MILLISECONDS)) {
        throw new RuntimeException("Timeout waiting for tasks to complete!");
      }
      LOG.info("Completed concurrent operations test");
    } catch (InterruptedException ex) {
      throw new RuntimeException("Interrupted while waiting for tasks to complete!", ex);
    }
  }
}
