package com.material.filesystem;

import com.material.filesystem.user.TestUserManager;
import com.material.filesystem.util.ConcurrentOperationTest;
import com.material.filesystem.util.DataGenerator;
import com.material.filesystem.util.StopWatch;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileSystemTest {
  private static final Logger LOG = LoggerFactory.getLogger(FileSystemTest.class);
  private static final ThreadLocal<StopWatch> perThreadStopwatch = ThreadLocal.withInitial(StopWatch::new);

  public static void startCurrentThreadStopwatch() {
    perThreadStopwatch.get().start();
  }

  public static void stopCurrentThreadStopwatch(String withMessage) {
    LOG.info("Total Time for [" + withMessage + "] - " + perThreadStopwatch.get().stop(TimeUnit.SECONDS) + " seconds");
  }

  @Test
  void cantMoveRootNode() throws Exception {
    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    Path filePath = Paths.get("apple");
    tree.createNodeAtPath(filePath, NodeType.DIRECTORY, false, false);

    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> tree.moveNodeTo(Paths.get("/"), Paths.get("apple"), false, false,true));
  }

  @Test
  void cantDeleteRootNode() {
    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> tree.removeNodeAtPath(Paths.get("/"), false));
  }

  @Test
  void testFileReadWrite() throws Exception {
    // make a HUGE array;
    byte[] writeBytes = DataGenerator.randomArray(1000);
    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    Path filePath = Paths.get("apple", "banana", "carrot");
    startCurrentThreadStopwatch();
    // create the entire path
    FileSystemTreeNode node = tree.createNodeAtPath(filePath, NodeType.FILE, false, true);
    File file = (File) node.getFileSystemObject();
    file.setContents(writeBytes);
    Assertions.assertEquals(writeBytes, file.getContents(), "Contents not set properly");
    stopCurrentThreadStopwatch("Read and Write entire content");
  }

  @Test
  void testFileReadStream() throws Exception {
    byte[] writeBytes = DataGenerator.randomArray(10000000);
    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    Path filePath = Paths.get("apple", "banana", "carrot");
    startCurrentThreadStopwatch();
    FileSystemTreeNode node = tree.createNodeAtPath(filePath, NodeType.FILE, false, true);
    File file = (File) node.getFileSystemObject();
    // make a HUGE array;
    file.setContents(writeBytes);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(file.size());
    file.readContentStream(outputStream);

    final byte[] readStreamBytes = outputStream.toByteArray();
    Assertions.assertTrue(Arrays.equals(writeBytes, readStreamBytes),
        "Byte arrays were not equal, read stream is broken");
    stopCurrentThreadStopwatch("Write content and read as stream");

  }


  @Test
  void testFileWriteStream() throws Exception {
    byte[] writeBytes = DataGenerator.randomArray(10000000);
    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    Path filePath = Paths.get("apple", "banana", "carrot");
    startCurrentThreadStopwatch();
    FileSystemTreeNode node = tree.createNodeAtPath(filePath, NodeType.FILE, false, true);
    File file = (File) node.getFileSystemObject();
    // make a HUGE array;
    file.writeContentStream(new ByteArrayInputStream(writeBytes, 0, writeBytes.length), 0);

    Assertions.assertTrue(Arrays.equals(writeBytes, file.getContents()),
        "Byte arrays were not equal, write stream is broken");
    stopCurrentThreadStopwatch("Write content and read as stream");
  }

  @Test
  public void testFileSystemTreeOperations() throws Exception {
    FileSystem tree = new DefaultFileSystem(new TestUserManager());

    Path filePath = Paths.get("apple", "banana", "carrot");
    tree.createNodeAtPath(filePath, NodeType.FILE, false, true);
    Path dirPath = Paths.get("apple", "banana", "papaya");
    tree.createNodeAtPath(dirPath, NodeType.DIRECTORY, false, true);
    Path dirPath2 = Paths.get("apple", "banana", "guava");
    tree.createNodeAtPath(dirPath2, NodeType.DIRECTORY, false, true);
    Path filePath2 = Paths.get("apple", "banana", "guava", "mango");
    tree.createNodeAtPath(filePath2, NodeType.FILE, false, true);

    FileSystemTreeNode foundNode = tree.getNodeAtPath(filePath, false);
    Assertions.assertNotNull(foundNode);
    FileSystemTreeNode foundNode2 = tree.getNodeAtPath(filePath2, false);
    Assertions.assertNotNull(foundNode2);
    FileSystemTreeNode foundDirNode = tree.getNodeAtPath(dirPath, false);
    Assertions.assertNotNull(foundDirNode);
    FileSystemTreeNode foundDirNode2 = tree.getNodeAtPath(dirPath2, false);
    Assertions.assertNotNull(foundDirNode2);
    Path nodePath = foundNode.getPath();

    Assertions.assertEquals("apple/banana/carrot", nodePath.toString());
    // find file whose full path ends with c
    FileSystemTreeNode matchingNode = tree.findFirstNodeMatching(Pattern.compile(".*carrot$"));
    String matchingPath = matchingNode.getPath().toString();
    Assertions.assertNotNull(matchingNode);
    Assertions.assertEquals("apple/banana/carrot", matchingPath);

    int treeSize = tree.size();
    Assertions.assertEquals(7, treeSize);
    // should match all nodes.
    Collection<FileSystemTreeNode> matchingNodes = tree.findAllNodesMatching(Pattern.compile("^.*apple.*"));
    Assertions.assertEquals(treeSize - 1, matchingNodes.size());
    nodePath = Paths.get("apple", "banana", "guava", "mango");
    Assertions.assertTrue(tree.removeNodeAtPath(nodePath, false));
    Assertions.assertEquals(6, tree.size());
    nodePath = Paths.get("apple");
    Assertions.assertTrue(tree.removeNodeAtPath(nodePath, false));
    Assertions.assertEquals(1, tree.size());
  }

  @Test
  void testCreateAndMoveNode() throws Exception {
    FileSystem tree = new DefaultFileSystem(new TestUserManager());

    // node to create
    Path filePath = Paths.get("apple", "banana", "carrot");
    // create a move target
    Path destPath = Paths.get("apple", "banana", "baboon");

    // create the node
    FileSystemTreeNode node = tree.createNodeAtPath(filePath, NodeType.FILE, false, true);
    // move and rename it absolute path, overwrite any existing node
    FileSystemTreeNode movedNode = tree.moveNodeTo(filePath, destPath, false, false, true);
    Assertions.assertTrue(tree.nodeExists(destPath), "Node was not properly moved to the destination path " + destPath.toString());
    Assertions.assertSame(movedNode, node, "Node was not moved properly");

    // select  working node
    tree.selectWorkingNode(Paths.get("apple", "banana"), false);
    Path destPath2 = Paths.get("../../");
    // move the node again to a new directory and verify
    tree.moveNodeTo(destPath, destPath2, false, false, false);
    Assertions.assertSame(node, tree.getNodeAtPath(Paths.get("baboon"), false));

    // select and verify root node
    tree.selectWorkingNode(Paths.get("/"), false);
    Assertions.assertSame(tree.getWorkingNode(), tree.getRoot(), "Did not select root node properly.");
  }


  @Test
  void testCopyFileToDir() throws Exception {
    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    // node to create
    Path filePath = Paths.get("apple", "banana", "carrot");
    // create a move target
    Path destPath = Paths.get("apple", "banana", "baboon");

    // create the node
    FileSystemTreeNode node = tree.createNodeAtPath(filePath, NodeType.FILE, false, true);
    File nodeFile = (File) node.getFileSystemObject();
    nodeFile.setContents(DataGenerator.randomArray(500));
    // move and rename it absolute path, overwrite any existing node
    FileSystemTreeNode copy = tree.copyNode(filePath, destPath, false, false, true);
    File copyNodeFile = (File) copy.getFileSystemObject();
    Assertions.assertTrue(Arrays.equals(nodeFile.getContents(), copyNodeFile.getContents()));
    Assertions.assertNotSame(node, copy, "Node was not copied properly.");
  }

  @Test
  void testCopyFileToFile() throws Exception {
    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    // node to create
    Path filePath = Paths.get("apple", "banana", "carrot.txt");

    // create the node
    FileSystemTreeNode node = tree.createNodeAtPath(filePath, NodeType.FILE, false, true);

    File nodeFile = (File) node.getFileSystemObject();
    nodeFile.setContents(DataGenerator.randomArray(500));
    // copy node over self
    FileSystemTreeNode copy = tree.copyNode(filePath, filePath, false, false, false);
    File copyNodeFile = (File) copy.getFileSystemObject();
    Assertions.assertEquals("carrot__1.txt", copyNodeFile.getName(), "First copy was not correct");

    copy = tree.copyNode(filePath, filePath, false, false, false);
    copyNodeFile = (File) copy.getFileSystemObject();

    Assertions.assertEquals("carrot__2.txt", copyNodeFile.getName(),  "Second copy was not correct");

    Assertions.assertTrue(Arrays.equals(nodeFile.getContents(), copyNodeFile.getContents()));
    Assertions.assertNotSame(node, copy, "Node was not copied properly.");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false}) // six numbers
  void testCopyDirToDirWithMerge(boolean overwrite) throws Exception {
    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    // node to create
    Path filePath = Paths.get("kitchen", "recipesA", "carrotCake");
    Path filePath2 = Paths.get("kitchen", "recipesB", "carrotCake");

    // create the node
    tree.createNodeAtPath(filePath, NodeType.DIRECTORY, false, true);
    tree.createNodeAtPath(filePath2, NodeType.DIRECTORY, false, true);

    // ingredients
    ArrayList<Path> ingredients1 = new ArrayList<>();
    ingredients1.add(Paths.get("carrots"));
    ingredients1.add(Paths.get("eggs"));
    ingredients1.add(Paths.get("cinnamon"));
    ingredients1.add(Paths.get("frosting"));
    ingredients1.add(Paths.get("sugar"));
    ArrayList<Path> ingredients2 = new ArrayList<>();
    ingredients2.add(Paths.get("carrots"));
    ingredients2.add(Paths.get("milk"));
    ingredients2.add(Paths.get("honey"));
    ingredients2.add(Paths.get("frosting"));
    ingredients2.add(Paths.get("salt"));


    try {
      tree.selectWorkingNode(filePath, false);
      for (Path ingredient : ingredients1) {
        ((File) tree.createNodeAtPath(ingredient, NodeType.FILE, true, false).getFileSystemObject()).setContents(
            DataGenerator.randomArray(600));
      }
      tree.selectWorkingNode(filePath2, false);
      for (Path ingredient : ingredients2) {
        ((File) tree.createNodeAtPath(ingredient, NodeType.FILE, true, false).getFileSystemObject()).setContents(
            DataGenerator.randomArray(500));
      }
    } catch (FileAlreadyExistsException ex) {
      Assertions.fail("Could not create file", ex);
    }

    tree.selectWorkingNode(Paths.get("/"), false);

    FileSystemTreeNode copy = tree.copyNode(filePath, Paths.get("kitchen", "recipesB"), false, false, overwrite);
    Assertions.assertEquals(overwrite ? 8 : 10, copy.getChildren().size(),  "Did not correctly merge directories.");
  }

  @Test
  public void testSelectWorkingNode() throws Exception {
    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    Path filePath = Paths.get("apple", "banana", "carrot");
    tree.createNodeAtPath(filePath, NodeType.FILE, true, true);

    ConcurrentOperationTest concurrentOperationTest = new ConcurrentOperationTest(Executors.newFixedThreadPool(2));
    ArrayList<Runnable> runnableTasks = new ArrayList<>();

    runnableTasks.add(() -> {
      try {
        tree.selectWorkingNode(Paths.get("apple"), false);
        Thread.sleep(1000);
        Assertions.assertEquals(tree.getWorkingNode().getPath().toString(), "/apple");
        Path relativePath = Paths.get("carrot", "beet");
        FileSystemTreeNode relNode = tree.createNodeAtPath(relativePath, NodeType.FILE, true, true);

        Assertions.assertEquals(tree.getNodeAtPath(Paths.get("apple", "carrot", "beet"), false), relNode,
            "Create Relative Node not working properly");
      } catch (Exception ex) {
        Assertions.fail("Exception while selecting working node on thread 1", ex);
      }
    });

    runnableTasks.add(() -> {
      try {
        tree.selectWorkingNode(Paths.get("apple", "banana"), false);
        Thread.sleep(1000);
        Assertions.assertEquals(tree.getWorkingNode().getPath().toString(), "apple/banana");
        Path relativePath = Paths.get("celery");
        FileSystemTreeNode relNode = tree.createNodeAtPath(relativePath, NodeType.FILE, true, true);

        Assertions.assertEquals(tree.getNodeAtPath(Paths.get("apple", "banana", "celery"), false), relNode,
            "Create Relative Node not working properly");
      } catch (Exception ex) {
        Assertions.fail("Exception while selecting working node on thread 2", ex);
      }
    });


    concurrentOperationTest.submitTasks(runnableTasks, 10000);
  }

  /**
   * This tests that a node can be moved during a read operation
   */
  @Test
  void testMultiThreadMoveDuringRead() {
    Lock fileLock = new ReentrantLock();
    // conditional lock to hold back the move thread until the file is created. Likely not needed most runs, but possible.
    Condition fileExists = fileLock.newCondition();
    AtomicBoolean movedDuringRead = new AtomicBoolean(false);

    FileSystem tree = new DefaultFileSystem(new TestUserManager());
    Path filePath = Paths.get("apple", "banana", "carrot");
    byte[] writeBytes = DataGenerator.randomArray(2000);

    ConcurrentOperationTest concurrentOperationTest = new ConcurrentOperationTest(Executors.newFixedThreadPool(2));
    ArrayList<Runnable> runnableTasks = new ArrayList<>();

    startCurrentThreadStopwatch();
    // execute a long read operation
   runnableTasks.add(() -> {

      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        Assertions.fail("Failed to wait prior to making the file");
      }

      fileLock.lock();

      FileSystemTreeNode node;
      try {
        node = tree.createNodeAtPath(filePath, NodeType.FILE, true, true);
        LOG.info("Created movable file to read stream");
        // signal the other thread that the file is ready
      } catch (FileNotFoundException | FileAlreadyExistsException e) {
        Assertions.fail("Failed to create node at path: " + filePath.toString(), e);
        fileLock.unlock();
        return;
      }
      File file = (File) node.getFileSystemObject();
      file.setContents(writeBytes);

      fileExists.signal();
      fileLock.unlock();

      startCurrentThreadStopwatch();
      // ensure  we're about to read before signaling a move

      try {
        FileStreamReader reader = new DelayedStreamReader(new ByteArrayOutputStream(file.size()), 4);
        file.readContentStream(reader);
        Assertions.assertTrue(Arrays.equals(writeBytes, reader.getBytes()),
            "Byte arrays were not equal, read stream is broken");

      } catch (IOException ex) {
        Assertions.fail(ex);
      }
    });

    // execute a move operation
    runnableTasks.add(() -> {
      startCurrentThreadStopwatch();
      fileLock.lock();
      try {
        // force wait for file to be created by other thread
        while (!tree.nodeExists(filePath)) {
          LOG.info("Waiting for file " + filePath.toString() + " to exist...");
          if (!fileExists.await(10000, TimeUnit.MILLISECONDS)) {
            Assertions.fail("File at " + filePath.toString() + " was never created, timeout");
          }
        }
        LOG.info("File exists and is being read, moving to a new location during read...");
      } catch (InterruptedException e) {
        // unlock if an error occurs
        fileLock.unlock();
        Assertions.fail("Failed to wait for file to exist");
      }

      Path destPath = Paths.get("apple", "banana", "baboon");

      try {
        // move the node again to a new directory and verify
        FileSystemTreeNode movedNode = tree.moveNodeTo(filePath, destPath, false, false, false);
        Assertions.assertTrue(tree.nodeExists(destPath), "File was not moved properly while being read");

        LOG.info("File moved to a new location /" + movedNode.getPath());

        File movedFile = (File) movedNode.getFileSystemObject();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(movedFile.size());

        movedFile.readContentStream(buffer);
        Assertions.assertTrue(Arrays.equals(writeBytes, buffer.toByteArray()),
            "Byte arrays were not equal, read stream is broken");
        movedDuringRead.set(true);

      } catch (FileNotFoundException | FileAlreadyExistsException e) {
        Assertions.fail("Could not select or move node: " + e);
      } catch (IOException ex) {
        Assertions.fail(ex);
      } finally {
        // unlock no matter what
        stopCurrentThreadStopwatch("Move operation while reading");
        fileLock.unlock();
      }
    });

    concurrentOperationTest.submitTasks(runnableTasks, 10000);

    Assertions.assertTrue(movedDuringRead.get());
    stopCurrentThreadStopwatch("Read Stream with simultaneous move");
  }

  /**
   * Used to read a file slowly to prove concurrency correctness.
   */
  static class DelayedStreamReader extends FileStreamReader {
    private int _bytesRead = 0;
    public DelayedStreamReader(ByteArrayOutputStream outputStream, int chunkSize) {
      super(outputStream, chunkSize);
    }

    @Override
    public void read(ByteArrayInputStream inputStream) throws IOException {
      int chunkBytesRead;
      byte[] data = new byte[getChunkSize()];

      while ((chunkBytesRead = inputStream.read(data, 0, getChunkSize())) > 0) {
        getOutputStream().write(data, 0, chunkBytesRead);
        _bytesRead += chunkBytesRead;
        try {
          Thread.sleep(2);
        } catch (InterruptedException ex) {
          throw new IOException("Interrupted read",  ex);
        }
      }

      getOutputStream().flush();
    }

    @Override
    public int getBytesRead() {
      return _bytesRead;
    }
  }
}
