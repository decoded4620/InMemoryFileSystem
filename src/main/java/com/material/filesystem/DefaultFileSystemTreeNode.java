package com.material.filesystem;

import com.material.filesystem.permissions.Permission;
import com.material.filesystem.user.DefaultUserManager;
import com.material.filesystem.user.User;
import com.material.filesystem.user.UserType;
import com.material.filesystem.util.FileSystemNodeHelper;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultFileSystemTreeNode implements FileSystemTreeNode {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultFileSystemTreeNode.class);
  private final List<FileSystemTreeNode> _children = new CopyOnWriteArrayList<>();
  private final Map<String, FileSystemTreeNode> _childMap = new ConcurrentHashMap<>();

  // NOTE - chose to keep these Node local to reduce any contention under stress
  private final Map<User, Set<Permission>> _userPermissions = new ConcurrentHashMap<>();
  private final Map<UserType, Set<Permission>> _userTypePermissions = new ConcurrentHashMap<>();

  // TODO - do better here
  private User _owner = DefaultUserManager.ROOT_USER;

  private final NodeType _nodeType;
  private final boolean _isRootNode;

  private final ReentrantReadWriteLock.WriteLock _transactionLock = new ReentrantReadWriteLock().writeLock();

  private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();

  private final Lock _readLock = _rwLock.readLock();
  private final Lock _writeLock = _rwLock.writeLock();

  // file associated with this tree node, can be a directory.
  private final FileSystemObject _fileSystemObject;
  private DefaultFileSystemTreeNode _parent = null;

  /**
   * Root Constructor
   */
  public DefaultFileSystemTreeNode() {
    this(new Directory("/"), NodeType.DIRECTORY);
  }


  public DefaultFileSystemTreeNode(FileSystemObject fileSystemObject, NodeType nodeType) {
    _fileSystemObject = fileSystemObject;
    _nodeType = nodeType;
    _isRootNode = _nodeType == NodeType.DIRECTORY && _fileSystemObject.getName().equals("/");
  }

  @Override
  public void setOwner(User owner) {
    startWrite();
    _owner = owner;
    completeWrite();
  }

  @Override
  public User getOwner() {
    startRead();
    try {
      return _owner;
    } finally {
      completeRead();
    }
  }

  @Override
  public boolean isRootNode() {
    return _isRootNode;
  }

  @Override
  public NodeType getNodeType() {
    return _nodeType;
  }

  @Override
  public void clearUserPermissions(User user) {
    startWrite();
    try {
      _userPermissions.computeIfAbsent(user, (u) -> new HashSet<>()).clear();
    } finally {
      completeWrite();
    }
  }

  @Override
  public void clearUserTypePermissions(UserType userType) {
    startWrite();
    try {
      _userTypePermissions.computeIfAbsent(userType, (u) -> new HashSet<>()).clear();
    } finally {
      completeWrite();
    }
  }

  @Override
  public Set<Permission> getUserPermissions(User user) {
    startRead();
    try {
      _userPermissions.computeIfAbsent(user, (u) -> new HashSet<>());
      return _userPermissions.get(user);
    } finally {
      completeRead();
    }
  }

  @Override
  public void setUserPermission(User user, Permission permission, boolean recursive) {
    startWrite();
    try {
      _userPermissions.computeIfAbsent(user, (u) -> new HashSet<>());
      _userPermissions.get(user).add(permission);

      if (recursive) {
        _children.forEach(child -> child.setUserPermission(user, permission, true));
      }
    } finally {
      completeWrite();
    }
  }

  @Override
  public Set<Permission> getUserTypePermissions(UserType userType) {
    startRead();
    try {
      _userTypePermissions.computeIfAbsent(userType, (ut) -> new HashSet<>());
      return _userTypePermissions.get(userType);
    } finally {
      completeRead();
    }
  }

  @Override
  public void setUserTypePermission(UserType userType, Permission permission, boolean recursive) {
    startWrite();
    try {
      _userTypePermissions.computeIfAbsent(userType, (ut) -> new HashSet<>());
      _userTypePermissions.get(userType).add(permission);

      if (recursive) {
        _children.forEach(child -> child.setUserTypePermission(userType, permission, true));
      }
    } finally {
      completeWrite();
    }
  }

  @Override
  public void retain() {
    _transactionLock.lock();
  }

  @Override
  public void release() {
    _transactionLock.unlock();
  }

  /**
   * Internal name function, not protected by read lock.
   * @return String, the name
   */
  private String internalGetName() {
    return _fileSystemObject.getName();
  }

  @Override
  public String getName() {
    try {
      startRead();
      return internalGetName();
    } finally {
      completeRead();
    }
  }

  @Override
  public void setName(String name) {
    startRead();
    try {
      _fileSystemObject.setName(name);
    } finally {
      completeRead();
    }
  }

  @Override
  public void addChild(FileSystemTreeNode child) {
    if (_nodeType != NodeType.DIRECTORY) {
      throw new UnsupportedOperationException("Only directory nodes can have children");
    }

    if (FileSystemNodeHelper.nodeIsAncestor(this, child)) {
      throw new UnsupportedOperationException("Cannot add a node that is in the parent path as a child");
    }

    String childName = child.getName();
    if (!_childMap.containsKey(childName)) {
      startWrite();

      try {
        // first try setting the parent
        DefaultFileSystemTreeNode currentParent = (DefaultFileSystemTreeNode) child.getParent();
        ((DefaultFileSystemTreeNode) child).setParent(this);

        // then try removing the child link via the old parent
        if (currentParent != null && currentParent != this) {
          currentParent.removeChild(child);
        }
        // if successful, then add to the child map
        _children.add(child);
        _childMap.put(childName, child);
      } finally {
        completeWrite();
      }
    } else {
      throw new UnsupportedOperationException(child.getName() + " is already a child of " + getName());
    }
  }

  @Override
  public FileSystemTreeNode removeChild(FileSystemTreeNode child) {
    if (_nodeType != NodeType.DIRECTORY) {
      throw new UnsupportedOperationException("Cannot remove children from a non-directory node");
    }

    String existingName = child.getName();

    if (_childMap.containsKey(existingName)) {
      try {
        startWrite();
        //first try setting the parent (this will fail if the child is immutable
        ((DefaultFileSystemTreeNode) child).setParent(null);
        FileSystemTreeNode removedChild = _childMap.remove(existingName);
        if (!_children.remove(removedChild)) {
          LOG.warn("Did not remove child node: " + existingName + " from " + internalGetName());
        }
        return removedChild;
      } finally {
        completeWrite();
      }
    }

    return null;
  }

  @Override
  public boolean hasChild(String name) {
    return _childMap.containsKey(name);
  }

  @Override
  public FileSystemTreeNode getChild(String name) {
    return _childMap.getOrDefault(name, null);
  }

  @Override
  public FileSystemTreeNode getParent() {
    startRead();
    try {
      return _parent;
    } finally {
      completeRead();
    }
  }

  /**
   * Sets the parent of this {@link DefaultFileSystemTreeNode} to solidify its position in the tree.
   * NOTE: This locks the read:ock (not writeLock). This enables the node to be moved while its being read.
   * @param parent the parent to set on this node.
   */
  private void setParent(FileSystemTreeNode parent) {
    if (_isRootNode) {
      throw new UnsupportedOperationException("Cannot move the root node '/' under any other node");
    }

    if (_parent != parent) {
      // See doc above about READ vs WRITE
      startRead();
      try {
        // NOTE try catch here in case other work added in the future.
        _parent = (DefaultFileSystemTreeNode) parent;
      } finally {
        completeRead();
      }
    }
  }

  @Override
  public FileSystemObject getFileSystemObject() {
    return _fileSystemObject;
  }

  @Override
  public Collection<FileSystemTreeNode> getChildren() {
    if (_nodeType == NodeType.DIRECTORY) {
      startRead();
      try {
        // return a copy so it cannot modify the original
        return new ArrayList<>(_children);
      } finally {
        completeRead();
      }
    } else {
      // TODO - handle this better
      throw new IllegalStateException("Not a directory");
    }
  }

  @Override
  public Path getPath() {
    String pathString = getPathString();
    if (pathString.charAt(0) == '/') {
      pathString = pathString.substring(1);
    }
    final String[] pathParts = pathString.split("/");
    // use the first token, and the remaining tokens to build the path
    return Paths.get(pathParts[0], Arrays.copyOfRange(pathParts, 1, pathParts.length));
  }

  @Override
  public int getSize() {
    if (_nodeType == NodeType.DIRECTORY) {
      startRead();
      try {
        return _children.size() + _children.stream().map(FileSystemTreeNode::getSize).reduce(0, Integer::sum);
      } finally {
        completeRead();
      }
    }

    return 0;
  }

  @Override
  public FileSystemTreeNode copy() {
    startRead();
    try {
      // base cases
      DefaultFileSystemTreeNode copy = new DefaultFileSystemTreeNode(getFileSystemObject().copy(), _nodeType);
      copyUserTypePermissionToNode(copy, false);
      copyUserPermissionsToNode(copy, false);

      if (_nodeType == NodeType.DIRECTORY) {
        // if it is a directory, copy the children recursively.
        getChildren().stream().map(FileSystemTreeNode::copy).forEach(copy::addChild);
      }
      return copy;
    } finally {
      completeRead();
    }
  }

  @Override
  public FileSystemTreeNode createChildNode(String name, NodeType nodeType) {
    DefaultFileSystemTreeNode node =
        new DefaultFileSystemTreeNode(FileSystemNodeHelper.createFileSystemObject(name, nodeType), nodeType);
    copyUserTypePermissionToNode(node, false);
    copyUserPermissionsToNode(node, false);
    addChild(node);
    return node;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultFileSystemTreeNode that = (DefaultFileSystemTreeNode) o;
    return _nodeType == that._nodeType && _fileSystemObject.equals(that._fileSystemObject);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_nodeType, _fileSystemObject);
  }

  void copyUserTypePermissionToNode(FileSystemTreeNode toNode, boolean recursive) {
    if (toNode != null) {
      _userTypePermissions.forEach(
          (userType, permSet) -> permSet.forEach(perm -> toNode.setUserTypePermission(userType, perm, recursive)));
    } else {
      throw new IllegalArgumentException("Cannot copy permissions to null node");
    }
  }

  void copyUserPermissionsToNode(FileSystemTreeNode toNode, boolean recursive) {
    if (toNode != null) {
      _userPermissions.forEach(
          (user, permSet) -> permSet.forEach(perm -> toNode.setUserPermission(user, perm, recursive)));
    } else {
      throw new IllegalArgumentException("Cannot copy permissions to null node");
    }
  }

  /**
   * Returns the path as a string
   * @return a String, the absolute path to this node.
   */
  private String getPathString() {
    startRead();
    try {
      String myPath = internalGetName();
      if (_parent != null) {
        myPath = _parent.getPathString() + '/' + myPath;
      }
      return myPath;
    } finally {
      completeRead();
    }
  }

  private void startRead() {
    _readLock.lock();
  }

  private void completeRead() {
    _readLock.unlock();
  }

  private void startWrite() {
    // writing is protected by the transaction lock
    retain();
    try {
      _writeLock.lock();
    } finally {
      release();
    }
  }

  private void completeWrite() {
    retain();
    try {
      _writeLock.unlock();
    } finally {
      release();
    }
  }
}
