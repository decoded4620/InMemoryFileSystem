package com.material.filesystem;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class FileSystemTreeNodeTest {

  @Test
  public void testCopy() {
    FileSystemTreeNode file = new DefaultFileSystemTreeNode(new File("def"), NodeType.FILE);
    FileSystemTreeNode copy = file.copy();

    if (!file.equals(copy)) {
      Assertions.fail("Copy was not correct, file.equals(copy) was false");
    }
  }

  @Test
  public void testTransactionalBehavior() {
    Executor threadPool = Executors.newFixedThreadPool(3);
    FileSystemTreeNode dir = new DefaultFileSystemTreeNode(new Directory("abc"), NodeType.DIRECTORY);
    FileSystemTreeNode file = new DefaultFileSystemTreeNode(new File("def"), NodeType.FILE);
    FileSystemTreeNode otherFile = new DefaultFileSystemTreeNode(new File("hij"), NodeType.FILE);
    CountDownLatch latch = new CountDownLatch(2);
    AtomicInteger retainCount = new AtomicInteger(0);
    // check thread for retain
    threadPool.execute(() -> {
      while (latch.getCount() > 0) {
        if(retainCount.get() > 1) {
          throw new IllegalThreadStateException("Transactions are broken, can only retain once");
        }
      }
    });

    threadPool.execute(() -> {
      dir.retain();
      retainCount.incrementAndGet();
      try {
        dir.addChild(file);
        Thread.sleep(100);
        dir.removeChild(file);
        Thread.sleep(100);
        dir.addChild(otherFile);
        Thread.sleep(100);
        dir.removeChild(otherFile);
        Thread.sleep(100);
      } catch(InterruptedException ex) {
        Assertions.fail("Failed to finish transaction");
      } finally {
        retainCount.decrementAndGet();
        dir.release();
        latch.countDown();
      }
    });

    threadPool.execute(() -> {
      dir.retain();
      retainCount.incrementAndGet();
      try {
        dir.addChild(file);
        dir.addChild(otherFile);
        Thread.sleep(100);
        dir.removeChild(file);
        dir.removeChild(otherFile);
        Thread.sleep(100);
      } catch(InterruptedException ex) {
        Assertions.fail("Failed to finish transaction");
      } finally {
        retainCount.decrementAndGet();
        dir.release();
        latch.countDown();
      }
    });

    try {
      if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
        Assertions.fail("Failed to wait for the latch");
      }
    } catch (InterruptedException ex) {
      Assertions.fail("Failed to wait for the latch", ex);
    }
  }
}
