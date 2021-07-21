package com.material.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.regex.Pattern;


public interface FileSystem {

  /**
   * Select a new current working node for the current thread.
   * @param path the path to the node
   * @param relative if true, relative to the current working node
   * @throws IOException if the working node  path is not a directory
   */
  void selectWorkingNode(Path path, boolean relative) throws IOException;

  /**
   * Get the current working node, for the current thread.
   * @return FileSystemTreeNode
   */
  FileSystemTreeNode getWorkingNode();

  /**
   * Returns the root node of the tree
   * @return the root {@link DefaultFileSystemTreeNode}
   */
  FileSystemTreeNode getRoot();

  /**
   * Get the size of the file system (total nodes)
   * @return an int, the count of nodes in the tree
   */
  int size();

  /**
   * Find first node matching the specified pattern, from  specific node.
   * @param pattern the Pattern to search
   * @return FileSystemTreeNode if found
   */
  FileSystemTreeNode findFirstNodeMatching(Pattern pattern);

  /**
   * Finds a collection of nodes matching the specified {@link Pattern}
   * @param pattern a {@link Pattern}
   * @return a {@link Collection} of {@link DefaultFileSystemTreeNode}
   */
  Collection<FileSystemTreeNode> findAllNodesMatching(Pattern pattern);

  /**
   * Remove a node at a path
   *
   * @param path the node to remove
   * @param relative true if the path is relative to the current working node
   * @return true if the node was removed, false otherwise.
   * @throws FileNotFoundException if the node at path was not found
   */
  boolean removeNodeAtPath(Path path, boolean relative) throws FileNotFoundException;

  /**
   * Check existence of a Node using absolute path
   * @param absolutePath absolute path to the node to check
   * @return boolean, true if the node exists, false otherwise.
   */
  boolean nodeExists(Path absolutePath);

  /**
   * Check existence of a Node
   * @param path path to the node to check
   * @param isRelative true if is a relative path
   * @return boolean, true if the node exists, false otherwise.
   */
  boolean nodeExists(Path path, boolean isRelative);

  /**
   * Get a Node by path. The path can be relative or absolute. If relative, uses the current working Node
   * @param path the {@link Path}
   * @param relative if true, finds the node relative to the working path
   * @return the node found at the {@link Path} relative to the current working node.
   * @throws FileNotFoundException if the node cannot be found.
   */
  FileSystemTreeNode getNodeAtPath(Path path, boolean relative) throws FileNotFoundException;

  /**
   * Creates a node at a specific path. The path leaf must be under a directory.
   *
   * @param path the source path
   * @param nodeType the type of file node to create.
   * @param relative true if this is a relative path from the current working directory.
   * @param createNonLeafNodes if true, create non-leaf nodes up until the path end. Always creates the leaf node in this case.
   *                           If false, throw a FileNotFoundException if a directory is not found.
   * @return the created {@link DefaultFileSystemTreeNode}
   * @throws FileAlreadyExistsException if the node already exists
   * @see NodeType
   */
  FileSystemTreeNode createNodeAtPath(Path path, NodeType nodeType, boolean relative, boolean createNonLeafNodes)
      throws FileAlreadyExistsException, FileNotFoundException;

  /**
   * Move a node from source path to destination path.
   * @param sourcePath the source path
   * @param destPath the destination path
   * @param sourceRelative true to use relative path for source
   * @param destinationRelative true to use relative path for destination
   * @param overwrite true to overwrite any existing node, otherwise  FileAlreadyExistsException is thrown
   * @return FileSystemTreeNode the node once move is complete, if no exception is thrown
   * @throws FileAlreadyExistsException if the file exists and overwrite is false
   * @throws FileNotFoundException if the source file or destination parent is not found.
   */
  FileSystemTreeNode moveNodeTo(Path sourcePath, Path destPath, boolean sourceRelative, boolean destinationRelative,
      boolean overwrite) throws FileAlreadyExistsException, FileNotFoundException;

  /**
   * Copy a node from source to destination. Can either be relative or absolute paths.
   * @param sourcePath the source path
   * @param destPath the destination path
   * @param sourceRelative true to use relative source path, false for absolute
   * @param destinationRelative true to use relative destination path, false for absolute
   * @param overwrite true to overwrite any existing nodes.
   * @throws FileNotFoundException if the node to be copied is not found, or if some directory leading to the
   *                               destination node is not found.
   */
  FileSystemTreeNode copyNode(Path sourcePath, Path destPath, boolean sourceRelative, boolean destinationRelative,
      boolean overwrite) throws FileNotFoundException;
}
