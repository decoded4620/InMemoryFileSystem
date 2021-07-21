package com.material.filesystem;

import com.material.filesystem.permissions.Permission;
import com.material.filesystem.user.UserManager;
import com.material.filesystem.user.UserType;
import com.material.filesystem.util.FileSystemNodeHelper;
import com.material.filesystem.util.FileSystemOperationInfo;
import com.material.filesystem.util.NameCollisionResolver;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultFileSystem implements FileSystem {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultFileSystem.class);
  // root node is immutable
  private final FileSystemTreeNode _root = new DefaultFileSystemTreeNode();
  private final ThreadLocal<FileSystemTreeNode> _workingNodeThreadLocal = ThreadLocal.withInitial(() -> _root);
  private final NameCollisionResolver _nameCollisionResolver = new NameCollisionResolver();
  private final UserManager _userManager;

  public DefaultFileSystem(UserManager userManager) {
    _userManager = userManager;

    // default use all permissions on root node for ADMIN user.
    Arrays.asList(Permission.READ, Permission.WRITE, Permission.DELETE)
        .forEach(permission -> _root.setUserTypePermission(UserType.ADMIN, permission, false));

    // default use all permissions on root node for ADMIN user.
    Arrays.asList(Permission.READ, Permission.WRITE, Permission.DELETE)
        .forEach(permission -> _root.setUserTypePermission(UserType.ROOT, permission, false));

    // default to use only read for GUEST user
    _root.setUserTypePermission(UserType.GUEST, Permission.READ, false);

    // default User type to read
    _root.setUserTypePermission(UserType.USER, Permission.READ, false);
  }

  @Override
  public FileSystemTreeNode getRoot() {
    return _root;
  }

  @Override
  public int size() {
    return 1 + _root.getSize();
  }

  /**
   * Build the next path part
   * @param currentPath the current path
   * @param nextNode the next path
   * @return a String
   */
  private static String buildChildPath(String currentPath, FileSystemTreeNode nextNode) {
    return currentPath + '/' + nextNode.getFileSystemObject().getName();
  }

  @Override
  public void selectWorkingNode(Path path, boolean relative) throws IOException {
    LOG.debug("Try selecting working node: " + path.toString());
    FileSystemTreeNode node = getNodeAtPath(path, relative);
    FileSystemNodeHelper.checkUserPermission(_userManager, node, Permission.READ);

    if (node.getNodeType() == NodeType.DIRECTORY) {
      _workingNodeThreadLocal.set(node);
      LOG.debug(
          "Selected working node: " + node.getPath().toString() + " for thread " + Thread.currentThread().getName());
    } else {
      throw new FileSystemException("Not a directory: " + path.toString());
    }
  }

  @Override
  public FileSystemTreeNode getWorkingNode() {
    FileSystemTreeNode node = _workingNodeThreadLocal.get();
    FileSystemNodeHelper.checkUserPermission(_userManager, node, Permission.READ);
    return node;
  }

  /**
   * Find first node matching the specified pattern, from  specific node.
   * @param pattern the Pattern to search
   * @param currentNode the current node to search from
   * @param currentPath the current path
   * @return FileSystemTreeNode if found
   */
  private FileSystemTreeNode findFirstMatchingNode(Pattern pattern, FileSystemTreeNode currentNode,
      String currentPath) {

    FileSystemNodeHelper.checkUserPermission(_userManager, currentNode, Permission.READ);

    // build up the path to the current node;
    if (pattern.matcher(currentPath).matches()) {
      return currentNode;
    } else {
      if (currentNode.getNodeType().equals(NodeType.DIRECTORY)) {
        return currentNode.getChildren().stream().map(child -> {
          final String childPath = buildChildPath(currentPath, child);
          return findFirstMatchingNode(pattern, child, childPath);
        }).findFirst().orElse(null);
      }
      return null;
    }
  }

  @Override
  public FileSystemTreeNode findFirstNodeMatching(Pattern pattern) {
    return findFirstMatchingNode(pattern, getWorkingNode(), getWorkingNode().getPath().toString());
  }

  /**
   * Find all nodes matching the specified pattern, from  specific node.
   * @param pattern the Pattern to search
   * @param currentNode the current node to search from
   * @param currentPath the current path
   * @param matchingNodes matching nodes
   */
  private void findAllNodesMatching(Pattern pattern, FileSystemTreeNode currentNode, String currentPath,
      List<FileSystemTreeNode> matchingNodes) {
    // build up the path to the current node;
    FileSystemNodeHelper.checkUserPermission(_userManager, currentNode, Permission.READ);

    if (pattern.matcher(currentPath).matches()) {
      matchingNodes.add(currentNode);
    }

    if (currentNode.getNodeType() == NodeType.DIRECTORY) {
      currentNode.getChildren().forEach(childNode -> {
        final String childPath = buildChildPath(currentPath, childNode);
        findAllNodesMatching(pattern, childNode, childPath, matchingNodes);
      });
    }
  }

  @Override
  public Collection<FileSystemTreeNode> findAllNodesMatching(Pattern pattern) {
    List<FileSystemTreeNode> collectedMatchingNodes = new LinkedList<>();
    findAllNodesMatching(pattern, getWorkingNode(), getWorkingNode().getPath().toString(), collectedMatchingNodes);
    return collectedMatchingNodes;
  }

  @Override
  public boolean removeNodeAtPath(Path path, boolean relative) throws FileNotFoundException {
    FileSystemTreeNode node = FileSystemNodeHelper.walkToEndNodeInPath(path, relative ? getWorkingNode() : _root)
        .orElseThrow(() -> new FileNotFoundException(
            "Cannot remove node at " + path.toString() + " because one of the directories or files did not exist"));

    if (node.isRootNode()) {
      throw new UnsupportedOperationException("Cannot delete the root node");
    }

    FileSystemOperationInfo operationInfo = new FileSystemOperationInfo(node, null);

    // check if move is legal
    if (operationInfo.isOperationLegal(FileSystemOperationInfo.OperationType.DELETE)) {

      FileSystemNodeHelper.checkUserPermission(_userManager, node.getParent(), Permission.WRITE);

      // pre-walk all nodes including and below 'node' to check if user can delete them.
      checkNodeSubtreePermissions(node, Permission.DELETE);
      if (FileSystemNodeHelper.isAncestorOf(node, _workingNodeThreadLocal.get())) {
        try {
          selectWorkingNode(node.getParent().getPath(), false);
        } catch(IOException ex) {
          try {
            selectWorkingNode(Paths.get("/"), false);
          } catch(IOException ex2) {
            // should not happen
            LOG.error("Bad things happened:", ex2);
          }
        }
      }
      return node.getParent().removeChild(node) != null;
    } else {
      throw new UnsupportedOperationException(
          "Move operation is not supported for " + operationInfo.getNodeTypeRelationship());
    }
  }

  /**
   * Validate a permission on the entire subtree for a node.
   * @param nodeSubtree the subtree
   * @param permissions  the permission to check
   */
  void checkNodeSubtreePermissions(FileSystemTreeNode nodeSubtree, Permission... permissions) {
    for (Permission perm : permissions) {
      FileSystemNodeHelper.checkUserPermission(_userManager, nodeSubtree, perm);
    }
    try {
      nodeSubtree.getChildren().forEach(child -> checkNodeSubtreePermissions(child, permissions));
    } catch (IllegalStateException ex) {
      // skip this node
    }
  }

  @Override
  public boolean nodeExists(Path absolutePath) {
    return nodeExists(absolutePath, false);
  }

  @Override
  public boolean nodeExists(Path path, boolean isRelative) {
    try {
      getNodeAtPath(path, isRelative);
      return true;
    } catch (FileNotFoundException e) {
      return false;
    }
  }

  @Override
  public FileSystemTreeNode getNodeAtPath(Path path, boolean relative) throws FileNotFoundException {
    return FileSystemNodeHelper.walkToEndNodeInPath(path, relative ? getWorkingNode() : _root)
        .orElseThrow(() -> new FileNotFoundException("Could not find node"));
  }

  @Override
  public FileSystemTreeNode createNodeAtPath(Path path, NodeType nodeType, boolean relative, boolean createNonLeafNodes)
      throws FileAlreadyExistsException, FileNotFoundException {

    FileSystemTreeNode node = relative ? getWorkingNode() : _root;
    FileSystemTreeNode createdNode = null;

    Iterator<Path> pathIt = path.iterator();
    while (pathIt.hasNext()) {
      final String partName = pathIt.next().toString();

      // checks write permission at each node in the path
      FileSystemNodeHelper.checkUserPermission(_userManager, node, Permission.WRITE);

      FileSystemTreeNode nextNodeInPath =
          partName.equals("..") ? (node.isRootNode() ? _root : node.getParent()) : node.getChild(partName);

      if (pathIt.hasNext()) {

        // we're not at the end, so the current path part should represent a directory
        if (nextNodeInPath == null) {
          // the directory doesn't exist, create it
          if (createNonLeafNodes) {
            // create a child and use it for the next node
            node = node.createChildNode(partName, NodeType.DIRECTORY);
            node.setOwner(_userManager.currentUser());
          } else {
            throw new FileNotFoundException("Directory: " + partName + " does not exist");
          }
        } else {
          if (nextNodeInPath.getNodeType() == NodeType.DIRECTORY) {
            node = nextNodeInPath;
          } else {
            throw new IllegalStateException("Current node is not a directory");
          }
        }
      } else {
        // the current part is the node we're creating
        if (nextNodeInPath != null) {
          throw new FileAlreadyExistsException("File " + path.toString() + " already exists");
        } else {
          node = node.createChildNode(partName, nodeType);
          node.setOwner(_userManager.currentUser());
          createdNode = node;
        }
      }
    }

    return createdNode;
  }

  private FileSystemTreeNode mergeDirectoryContents(FileSystemTreeNode sourceNode, FileSystemTreeNode targetNode,
      boolean overwriteExistingFiles) throws FileNotFoundException, FileAlreadyExistsException, UnsupportedOperationException {
    if (sourceNode.getNodeType() != NodeType.DIRECTORY || targetNode.getNodeType() != NodeType.DIRECTORY) {
      throw new UnsupportedOperationException("Both nodes must be directories");
    }

    // do nothing
    if (targetNode == sourceNode) {
      throw new UnsupportedOperationException("Source and target were the same, unsupported");
    }

    targetNode.retain();

    try {
      for (FileSystemTreeNode child : sourceNode.getChildren()) {
        moveNodeTo(child.getPath(), targetNode.getPath(), false, false, overwriteExistingFiles);
      }
    } finally {
      targetNode.release();
    }

    return targetNode;
  }

  private FileSystemTreeNode placeFileNodeInDirectoryNode(FileSystemTreeNode fileNode, FileSystemTreeNode dirNode,
      boolean overwriteExistingNode) {
    dirNode.retain();
    try {
      String destinationNodeName = fileNode.getName();
      if (!overwriteExistingNode) {
        // resolve the name collision
        while (dirNode.hasChild(destinationNodeName)) {
          destinationNodeName = _nameCollisionResolver.resolve(destinationNodeName);
        }
        // rename the node
        fileNode.setName(destinationNodeName);
      } else {
        if (dirNode.hasChild(destinationNodeName)) {
          // file nodes must have a parent, add this new node, but remove the existing one
          dirNode.removeChild(dirNode.getChild(destinationNodeName));
        }
      }
      // do the placement
      dirNode.addChild(fileNode);
      return fileNode;
    } finally {
      dirNode.release();
    }
  }

  private FileSystemTreeNode placeDirectoryNodeInsideDirectoryNode(FileSystemTreeNode sourceNode,
      FileSystemTreeNode destinationNode, boolean overwriteExistingFiles) throws FileAlreadyExistsException, FileNotFoundException {
    if (sourceNode.getNodeType() != NodeType.DIRECTORY || destinationNode.getNodeType() != NodeType.DIRECTORY) {
      throw new IllegalArgumentException("Both node types must be directories");
    }
    destinationNode.retain();
    try {
      if (destinationNode.hasChild(sourceNode.getName())) {
        FileSystemTreeNode existingChild = destinationNode.getChild(sourceNode.getName());

        if (existingChild.getNodeType() == NodeType.FILE) {
          throw new UnsupportedOperationException("Cannot place directory node over a file node of the same name");
        }

        FileSystemTreeNode merged = mergeDirectoryContents(sourceNode, existingChild, overwriteExistingFiles);
        //  only do this if merge was successful. Otherwise, leave the files that weren't' moved under original section
        if (sourceNode.getParent() != null) {
          sourceNode.getParent().removeChild(sourceNode);
        }

        return merged;
      } else {
        destinationNode.addChild(sourceNode);
        return destinationNode;
      }
    } finally {
      destinationNode.release();
    }
  }

  private FileSystemTreeNode getNodeAtPathOrNull(Path sourcePath, boolean relative) {
    try {
      return getNodeAtPath(sourcePath, relative);
    } catch (FileNotFoundException ex) {
      return null;
    }
  }

  @Override
  public FileSystemTreeNode moveNodeTo(Path sourcePath, Path destPath, boolean sourceRelative,
      boolean destinationRelative, boolean overwrite)
      throws FileAlreadyExistsException, FileNotFoundException, UnsupportedOperationException {

    FileSystemOperationInfo operationInfo = new FileSystemOperationInfo(getNodeAtPathOrNull(sourcePath, sourceRelative),
        getNodeAtPathOrNull(destPath, destinationRelative));

    // base case
    if (operationInfo.getSourceNode() == operationInfo.getTargetNode()) {
      LOG.warn("No Files were moved, source and target node are the same node");
      return operationInfo.getSourceNode();
    }

    // check if move is legal - NOTE Permissions not included here.
    if (operationInfo.isOperationLegal(FileSystemOperationInfo.OperationType.MOVE)) {
      switch (operationInfo.getNodeTypeRelationship()) {
        case FILE_TO_FILE:
          return placeFileNodeInDirectoryNode(operationInfo.getSourceNode(), operationInfo.getTargetNode().getParent(),
              overwrite);
        case FILE_TO_DIR:
          return placeFileNodeInDirectoryNode(operationInfo.getSourceNode(), operationInfo.getTargetNode(), overwrite);
        case DIR_TO_DIR:
          return placeDirectoryNodeInsideDirectoryNode(operationInfo.getSourceNode(), operationInfo.getTargetNode(),
              overwrite);
        case FILE_TO_NONE:
          // this is move and rename operation, ensure file name uses dest path file name
          FileSystemTreeNode destinationParent = getNodeAtPath(destPath.getParent(), destinationRelative);
          operationInfo.getSourceNode().retain();
          try {
            operationInfo.getSourceNode().setName(destPath.getFileName().toString());
            return placeFileNodeInDirectoryNode(operationInfo.getSourceNode(), destinationParent, overwrite);
          } finally {
            operationInfo.getSourceNode().release();
          }
        case DIR_TO_FILE:
          throw new UnsupportedOperationException("Cannot move a directory into a file");
        default:
          throw new UnsupportedOperationException(
              "Other unsupported operations: " + operationInfo.getNodeTypeRelationship());
      }
    } else {
      throw new UnsupportedOperationException(
          "Move operation is not supported for " + operationInfo.getNodeTypeRelationship());
    }
  }

  @Override
  public FileSystemTreeNode copyNode(Path sourcePath, Path destPath, boolean sourceRelative,
      boolean destinationRelative, boolean overwrite) throws FileNotFoundException {

    FileSystemOperationInfo operationInfo = new FileSystemOperationInfo(getNodeAtPathOrNull(sourcePath, sourceRelative),
        getNodeAtPathOrNull(destPath, destinationRelative));

    // check if move is legal
    if (!operationInfo.isOperationLegal(FileSystemOperationInfo.OperationType.COPY)) {
      throw new UnsupportedOperationException(
          "Move operation is not supported for " + operationInfo.getNodeTypeRelationship());
    }

    // get source and destination nodes.
    FileSystemTreeNode sourceNode = operationInfo.getSourceNode();

    FileSystemNodeHelper.checkUserPermission(_userManager, sourceNode, Permission.READ);
    Optional<FileSystemTreeNode> maybeDestinationNode = Optional.ofNullable(operationInfo.getTargetNode());

    maybeDestinationNode.ifPresent(
        destinationNode -> FileSystemNodeHelper.checkUserPermission(_userManager, destinationNode, Permission.WRITE));

    FileSystemTreeNode destinationNode;
    FileSystemTreeNode copyNode;

    switch (operationInfo.getNodeTypeRelationship()) {
      case FILE_TO_FILE:
        // this is a move and rename operation
        FileSystemTreeNode destinationParentDirectory = Optional.ofNullable(maybeDestinationNode.orElseThrow(
            () -> new FileNotFoundException("Destination " + destPath + " was not found, and may have been deleted"))
            .getParent()).orElseThrow(() -> new FileNotFoundException(
          "Destination node " + destPath.getParent() + " was not found, and may have been deleted"));

        FileSystemNodeHelper.checkUserPermission(_userManager, destinationParentDirectory, Permission.WRITE);

        try {
          // start a transaction here, so no writes can take place on the destination directory node
          destinationParentDirectory.retain();
          copyNode = sourceNode.copy();
          copyNode.setOwner(_userManager.currentUser());
          if (!sourcePath.getFileName().equals(destPath.getFileName())) {
            copyNode.getFileSystemObject().setName(destPath.getFileName().toString());
          }
          return placeFileNodeInDirectoryNode(copyNode, destinationParentDirectory, overwrite);
        } finally {
          destinationParentDirectory.retain();
        }
      case FILE_TO_DIR:
        destinationNode = maybeDestinationNode.orElseThrow(() -> new IllegalStateException(
            "Destination node was null, but file operation expects that it was not null"));

        destinationNode.retain();
        try {
          // copy the file and put it inside the destination directory, update the name to the destination name
          copyNode = sourceNode.copy();
          copyNode.setOwner(_userManager.currentUser());
          return placeFileNodeInDirectoryNode(copyNode, destinationNode, overwrite);
        } finally {
          destinationNode.release();
        }
      case FILE_TO_NONE:
        // check if the parent path is found. It means we're copying to a file destination which doesn't yet exist.
        // If not found, this will throw another FileNotFoundException and exit here.
        FileSystemTreeNode destinationParent = getNodeAtPath(destPath.getParent(), false);
        destinationParent.retain();
        try {
          // copy the file and put it inside the destination directory, update the name to the destination name
          copyNode = sourceNode.copy();
          copyNode.setOwner(_userManager.currentUser());
          return placeFileNodeInDirectoryNode(copyNode, destinationParent, overwrite);
        } finally {
          destinationParent.release();
        }

      case DIR_TO_DIR:
        destinationNode = maybeDestinationNode.orElseThrow(() -> new IllegalStateException(
            "Destination node was null, but file operation expects that it was not null"));
        destinationNode.retain();
        sourceNode.retain();
        try {
          if (nodeExists(Paths.get(destinationNode.getPath().toString(), sourceNode.getName()))) {
            // if the destination directory already contains a node with matching name
            // it must be a directory, and we'll merge it.
            FileSystemTreeNode destChildWithMatchingName = destinationNode.getChild(sourceNode.getName());
            FileSystemOperationInfo info = new FileSystemOperationInfo(sourceNode, destChildWithMatchingName);

            if (info.isOperationLegal(FileSystemOperationInfo.OperationType.COPY)) {
              // merge the contents of these directories.
              for (FileSystemTreeNode child : sourceNode.getChildren()) {
                copyNode(child.getPath(), Paths.get(destChildWithMatchingName.getPath().toString(), child.getName()),
                    false, false, overwrite);
              }

              return destChildWithMatchingName;
            } else {
              throw new UnsupportedOperationException("Illegal operation: Cannot copy a directory over a file");
            }
          } else {
            copyNode = sourceNode.copy();
            copyNode.setOwner(_userManager.currentUser());
            destinationNode.addChild(copyNode);
            return copyNode;
          }
        } finally {
          destinationNode.release();
          sourceNode.release();
        }
    }

    return null;
  }
}
