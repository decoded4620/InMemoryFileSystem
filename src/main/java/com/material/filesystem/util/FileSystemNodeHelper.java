package com.material.filesystem.util;

import com.material.filesystem.Directory;
import com.material.filesystem.File;
import com.material.filesystem.FileSystemObject;
import com.material.filesystem.FileSystemTreeNode;
import com.material.filesystem.NodeType;
import com.material.filesystem.permissions.InsufficientPermissionException;
import com.material.filesystem.permissions.Permission;
import com.material.filesystem.user.UserManager;
import com.material.filesystem.user.UserType;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;


public class FileSystemNodeHelper {

  /**
   * Returns true if there exists a path from the ancestor candidate to the check node
   * @param ancestorCandidate the node that maybe an ancestor
   * @param checkNode the check node.
   * @return true if the ancestorCandidate has a path to checkNode.
   */
  public static boolean isAncestorOf(FileSystemTreeNode ancestorCandidate, FileSystemTreeNode checkNode) {
    if (checkNode != ancestorCandidate) {
      FileSystemTreeNode currNode = checkNode.getParent();

      while (currNode != null) {
        if (currNode == ancestorCandidate) {
          return true;
        }
        currNode = currNode.getParent();
      }

      return false;
    } else {
      return true;
    }
  }

  public static Optional<FileSystemTreeNode> walkToEndNodeInPath(Path path, FileSystemTreeNode startNode) {
    if (path == null) {
      return Optional.empty();
    }

    Iterator<Path> pathIt = path.iterator();

    FileSystemTreeNode node = startNode;

    while (node != null && pathIt.hasNext()) {
      Path part = pathIt.next();
      String partName = part.toString();
      node = partName.equals("..") ? (node.isRootNode() ? node : node.getParent()) : node.getChild(part.toString());
    }

    return Optional.ofNullable(node);
  }

  /**
   * Check if a node is an ancestor of another node
   * @param node node
   * @param maybeAncestor node that maybe an ancestor
   * @return true if it is an ancestor
   */
  public static boolean nodeIsAncestor(FileSystemTreeNode node, FileSystemTreeNode maybeAncestor) {

    if (node == null) {
      return false;
    }

    if (node == maybeAncestor) {
      return true;
    }
    // check if loop would be created or tree data would be lost by adding this node as a child
    FileSystemTreeNode checkNode = node.getParent();
    while (checkNode != null) {
      if (checkNode == maybeAncestor) {
        return true;
      }
      checkNode = checkNode.getParent();
    }

    return false;
  }

  public static FileSystemObject createFileSystemObject(String name, NodeType nodeType) {
    switch (nodeType) {
      case FILE:
        return new File(name);
      case DIRECTORY:
        return new Directory(name);
      default:
        throw new UnsupportedOperationException("Cannot create child node type: " + nodeType);
    }
  }

  /**
   * Validate the user has permission to perform a type of operation on a node.
   *
   * @param node a {@link FileSystemTreeNode}
   * @param permission a {@link Permission}
   * @throws InsufficientPermissionException if the user doesn't have permission
   */
  public static void checkUserPermission(UserManager userManager, FileSystemTreeNode node, Permission permission)
      throws InsufficientPermissionException {
    userManager.checkLoggedIn();
    // owner always has privileges
    if (node.getOwner() != userManager.currentUser() && !node.getUserTypePermissions(
        userManager.currentUser().getUserType()).contains(permission) && !node.getUserPermissions(
        userManager.currentUser()).contains(permission)) {
      throw new InsufficientPermissionException(
          "User " + userManager.currentUser().getUsername() + "/" + userManager.currentUser().getUserType()
              + "  does not have " + permission.name() + " permission on " + node.getName());
    }
  }

  public static void getNodeInfo(UserManager um, FileSystemTreeNode node, StringBuilder sb, boolean isVerbose,
      boolean printFullPath) {
    sb.append(isVerbose ? "\n" : " ").append(printFullPath ? '/' + node.getPath().toString() : node.getName());

    if (isVerbose) {
      sb.append(" ").append(node.getOwner().getUsername()).append(" ");
      // if verbose, prints the following format
      // nodeName -d[rwd][rw][r] - 1000 bytes (Jan 1 1970 22:22)
      Set<Permission> userPerms = node.getUserPermissions(um.currentUser());

      sb.append(" -");
      if (!userPerms.isEmpty()) {
        sb.append(node.getNodeType().getPrintableName()).append("[");
        userPerms.forEach(perm -> sb.append(perm.getPrintablePerm()));
        sb.append("]");
      }

      // user type
      sb.append("[");
      sb.append(um.currentUser().getUserType().getPrintableName());
      node.getUserTypePermissions(um.currentUser().getUserType()).forEach(perm -> sb.append(perm.getPrintablePerm()));
      sb.append("]");

      // guest perms
      sb.append("[");
      sb.append(UserType.GUEST.getPrintableName());
      node.getUserTypePermissions(UserType.GUEST).forEach(perm -> sb.append(perm.getPrintablePerm()));
      sb.append("]");

      // size
      sb.append(" - ");
      if (node.getNodeType() == NodeType.FILE) {
        sb.append(((File) node.getFileSystemObject()).size()).append(" bytes");
      }

      // date created, last updated
      final String created =
          DateFormat.getDateInstance(0).format(new Date(node.getFileSystemObject().getCreationTime()));
      final String lastUpdated =
          DateFormat.getDateInstance(0).format(new Date(node.getFileSystemObject().getLastUpdatedTime()));
      sb.append("(").append(created).append(",").append(lastUpdated).append(")");
    }
  }
}
