package com.material.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.regex.Pattern;


public interface FileSystem {
  /**
   * Select a new current working node for the current thread using absolute path
   * @param path the path to the node
   * @throws IOException if the working node  path is not a directory
   */
  void selectWorkingNode(Path path) throws IOException;

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
   * @param absolutePath the absolute path of the node to remove
   * @return true if the node was removed, false otherwise.
   * @throws FileNotFoundException if the node at path was not found
   */
  boolean removeNodeAtPath(Path absolutePath) throws FileNotFoundException;

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
   * Get a Node by absolute path
   * @param absolutePath the path
   * @return the node found at the supplied {@link Path}, from the root
   * @throws FileNotFoundException if the node is not found
   */
  FileSystemTreeNode getNodeAtPath(Path absolutePath) throws FileNotFoundException;

  /**
   * Get a Node by path. The path can be relative or absolute. If relative, uses the current working Node
   * @param path the {@link Path}
   * @param relative if true, finds the node relative to the working path
   * @return the node found at the {@link Path} relative to the current working node.
   * @throws FileNotFoundException if the node cannot be found.
   */
  FileSystemTreeNode getNodeAtPath(Path path, boolean relative) throws FileNotFoundException;

  /**
   * Create a node by absolute path.
   * @param absolutePath the absolute {@link Path}
   * @param nodeType the type of node to create
   * @return the created {@link FileSystemTreeNode}
   * @throws FileAlreadyExistsException if the node already exists.
   * @see NodeType
   */
  FileSystemTreeNode createNodeAtPath(Path absolutePath, NodeType nodeType) throws FileAlreadyExistsException;

  /**
   * Creates a node at a specific path. The path leaf must be under a directory.
   *
   * @param path the source path
   * @param nodeType the type of file node to create.
   * @param relative true if this is a relative path from the current working directory.
   * @return the created {@link DefaultFileSystemTreeNode}
   * @throws FileAlreadyExistsException if the node already exists
   * @see NodeType
   */
  FileSystemTreeNode createNodeAtPath(Path path, NodeType nodeType, boolean relative)
      throws FileAlreadyExistsException;

  /**
   * Move node which can be a file or directory under another node which is a directory
   * @param path the source path
   * @param destination the destination directory path
   * @param relative true for relative pathing
   * @param overwrite true to overwrite any existing node
   * @throws FileNotFoundException if the source or destination directory is not found.
   */
  void moveNodeToDir(Path path, Path destination, boolean relative, boolean overwrite) throws FileAlreadyExistsException, FileNotFoundException;

  /**
   * Move a node from source path to destination path using absolute paths
   * @param oldNodePath the current path of the node
   * @param newNodePath the destination path of the node. If the node being moved is a file, then it will be moved to the directory above the new path name leaf.
   *                    For instance, if we hve a path dir1/dir2/file1 and we move it to dir1/dir2/file2 it will be renamed to file2 in the same dir. If
   *                    we have dir1/dir2/file1 and we move it to dir1/dir3, the new file will be dir1/dir3/file1
   * @param overwrite true to overwrite any existing node, otherwise  FileAlreadyExistsException is thrown
   * @throws FileAlreadyExistsException if the file exists and overwrite is false
   * @throws FileNotFoundException if the source file or destination parent is not found.
   */
  void moveNodeTo(Path oldNodePath, Path newNodePath, boolean overwrite) throws FileAlreadyExistsException, FileNotFoundException;

  /**
   * Move a node from source path to destination path.
   * @param source the source path
   * @param destination the destination path
   * @param sourceRelative true to use relative path for source
   * @param destinationRelative true to use relative path for destination
   * @param overwrite true to overwrite any existing node, otherwise  FileAlreadyExistsException is thrown
   * @throws FileAlreadyExistsException if the file exists and overwrite is false
   * @throws FileNotFoundException if the source file or destination parent is not found.
   */
  void moveNodeTo(Path source, Path destination, boolean sourceRelative, boolean destinationRelative, boolean overwrite) throws FileAlreadyExistsException, FileNotFoundException;

  /**
   * Copy nodes using absolute paths.
   * @param source the source path
   * @param destination the destination path
   */
  void copyNode(Path source, Path destination);

  /**
   * Copy a node from source to destination. Can either be relative or absolute paths.
   * @param source the source path
   * @param destination the destination path
   * @param sourceRelative true to use relative source path, false for absolute
   * @param destinationRelative true to use relative destination path, false for absolute
   */
  void copyNode(Path source, Path destination, boolean sourceRelative, boolean destinationRelative);

  /**
   * Merge  directory contents from source to destination, using aboslute paths.
   *
   * @param source the source path
   * @param destination the destination path
   * @see DefaultFileSystem#mergeDirectoryNodes(Path, Path, boolean, boolean)
   */
  void mergeDirectoryNodes(Path source, Path destination);

  /**
   * Merges the contents of the source directory into the destination directory. Leaves the source directory
   * empty. Renames files to avoid collision. Source and destination paths can either (or both) be absolute or relative
   * to the current working directory node.
   *
   * @param source the source path, must be a directory
   * @param destination the destination path, must be a directory
   * @param relativeSource true if the source path is relative
   * @param relativeDestination true if the  destination path is relative
   */
  void mergeDirectoryNodes(Path source, Path destination, boolean relativeSource, boolean relativeDestination);
}
