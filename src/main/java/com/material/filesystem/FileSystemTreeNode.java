package com.material.filesystem;

import com.material.filesystem.permissions.Permission;
import com.material.filesystem.user.User;
import com.material.filesystem.user.UserType;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;


public interface FileSystemTreeNode {
  /**
   * Sets the owner of this file node.
   * @param user the Owner user
   */
  void setOwner(User user);

  /**
   * Get the user owner.
   * @return the owner
   */
  User getOwner();

  /**
   * Clear the permissions for a User on this node.
   * @param user the user.
   */
  void clearUserPermissions(User user);

  /**
   * Clear the user type permissions from this node.
   * @param userType the {@link UserType to clear permissions for
   */
  void clearUserTypePermissions(UserType userType);

  /**
   * A Map of user specific permissions for a file
   * @return a Set of permissions for the user
   */
  Set<Permission> getUserPermissions(User user);

  /**
   * Set permissions for a specific user on this node
   * @param user a User
   * @param permission the permission
   * @param recursive true to copy the permission down the tree
   */
  void setUserPermission(User user, Permission permission, boolean recursive);

  /**
   * A Map of User Type Specific Permissions for a file.
   * @param userType the type of user to store a permission on
   * @return a Map
   */
  Set<Permission> getUserTypePermissions(UserType userType);

  /**
   * Set User type permissions
   * @param userType a UserType
   * @param recursive true to copy the permission down the tree
   * @param permission a Permission
   */
  void setUserTypePermission(UserType userType, Permission permission, boolean recursive);

  /**
   * Returns true if this is a root node.
   * @return true if this is the file system root node.
   */
  boolean isRootNode();

  /**
   * Returns the {@link NodeType} for this node.
   * @return a NodeType
   */
  NodeType getNodeType();

  /**
   * Returns the name of this node.
   * @return a String
   */
  String getName();

  /**
   * Set the name of the underlying {@link FileSystemObject} for this node.
   * @param name a String name
   */
  void setName(String name);

  /**
   * Add a child node to this node.
   *
   * @param child the {@link FileSystemTreeNode} to add
   */
  void addChild(FileSystemTreeNode child);

  /**
   * Removes a direct child from this node.
   * @param child the child to remove.
   * @return the removed {@link FileSystemTreeNode} or null
   */
  FileSystemTreeNode removeChild(FileSystemTreeNode child);

  /**
   * Returns a direct child of this node by name, or null;
   * @param name the name of the child to return.
   * @return a {@link FileSystemTreeNode}
   */
  FileSystemTreeNode getChild(String name);

  /**
   * Returns true if this node contains a child with <pre>name</pre>
   * @param name the name of the child to check.
   * @return a boolean
   */
  boolean hasChild(String name);

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
   * Returns the size of this node. If it is a directory, it will include all the children nodes below it, recursively
   *
   * @return the size of this node.
   */
  int getSize();

  /**
   * Creates a copy of this node and all of its children, recursively.
   * @return a new {@link FileSystemTreeNode}, an exact copy
   */
  FileSystemTreeNode copy();

  /**
   * Retains a write lock on the FileSystemNode as a whole, to enable multiple transactional function calls
   */
  void retain();

  /**
   * Release the transactional write lock on the FileSystemNode, to complete transactional function calls.
   */
  void release();

  /**
   * Creates a new child node (only when creating, not when re-parenting). Permissions are copied at this time.
   * @param name the name of the child node to create
   * @param nodeType the node type
   * @return FileSystemTreeNode that was created.
   */
  FileSystemTreeNode createChildNode(String name, NodeType nodeType);
}
