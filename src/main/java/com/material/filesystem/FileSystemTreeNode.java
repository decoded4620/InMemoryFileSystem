package com.material.filesystem;

import java.nio.file.Path;
import java.util.Collection;


public interface FileSystemTreeNode {
  NodeType getNodeType();

  /**
   * Returns the name of this node.
   * @return a String
   */
  String getName();

  /**
   * Add a child node to this node.
   *
   * @param child the {@link FileSystemTreeNode} to add
   * @return true if the child was added.
   */
  boolean addChild(FileSystemTreeNode child);

  /**
   * Removes a direct child from this node.
   * @param child the child to remove.
   * @return true if the child was removed.
   */
  boolean removeChild(FileSystemTreeNode child);

  /**
   * Returns a direct child of this node by name, or null;
   * @param name the name of the child to return.
   * @return a {@link FileSystemTreeNode}
   */
  FileSystemTreeNode getChild(String name);

  /**
   * Returns the parent of this node.
   * @return a {@link FileSystemTreeNode}
   */
  FileSystemTreeNode getParent();

  /**
   * Returns the file associated with this node.
   * @return a {@link File}
   */
  FileSystemObject getFileSystemObject();

  /**
   * Returns the children of this node.
   * @return a {@link Collection} of FileSystemTreeNode
   */
  Collection<FileSystemTreeNode> getChildren();

  /**
   * Returns the path to this node, from the root of the tree.
   * @return the {@link Path} to this node, including the name of this node.
   */
  Path getPath();

  /**
   * Returns the size of this node. If it is a directory, it will be all the children below it, recursively
   *
   * @return the size of this node.
   */
  int getSize();
}
