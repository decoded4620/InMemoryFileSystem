package com.material.filesystem.util;

import com.material.filesystem.FileSystemTreeNode;
import com.material.filesystem.NodeType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This class represents information about a file system operation between one or two nodes, including the {@link OperationType},
 * the {@link NodeType} of source and/or target nodes, the {@link OperationInfoType} describing if the operation is unary or binary.
 *
 * Combining this information enables us to carefully decide if the operation is legal.
 */
public class FileSystemOperationInfo {
  private static final Map<NodeTypeRelationship, Set<OperationType>> OPERATION_SUPPORT_MAP;
  private final OperationInfoType _operationInfoType;
  private final FileSystemTreeNode _sourceNode;
  private final FileSystemTreeNode _targetNode;
  private final NodeType _sourceType;
  private final NodeType _targetType;

  /**
   * Constructor
   *
   * @param sourceNode the source node for the operation.
   * @param targetNode the target node for the operation.
   */
  public FileSystemOperationInfo(FileSystemTreeNode sourceNode, FileSystemTreeNode targetNode) {
    _sourceNode = sourceNode;
    _targetNode = targetNode;

    boolean sourceExists = _sourceNode != null;
    boolean targetExists = _targetNode != null;
    boolean bothExist = sourceExists && targetExists;

    if (bothExist) {
      _operationInfoType = OperationInfoType.BINARY;
      _sourceType = _sourceNode.getNodeType();
      _targetType = _targetNode.getNodeType();
    } else if (targetExists) {
      _operationInfoType = OperationInfoType.TARGET_UNARY;
      // TODO - this should probably be illegal since unary operations should always just have a source node.
      _sourceType = NodeType.NONE;
      _targetType = _targetNode.getNodeType();
    } else if (sourceExists) {
      _operationInfoType = OperationInfoType.SOURCE_UNARY;
      _sourceType = _sourceNode.getNodeType();
      _targetType = NodeType.NONE;
    } else {
      _operationInfoType = OperationInfoType.NONE;
      _sourceType = NodeType.NONE;
      _targetType = NodeType.NONE;
    }
  }

  /**
   * Returns true of the node type relationship and operation type amount to a supported operation. If not, the operation
   * will be discarded.
   *
   * @param nodeTypeRelationship the {@link NodeTypeRelationship}
   * @param operationType the {@link OperationType}
   * @return true if the operation is supported.
   */
  public boolean isOperationTypeSupportedForNodes(NodeTypeRelationship nodeTypeRelationship,
      OperationType operationType) {
    return OPERATION_SUPPORT_MAP.get(nodeTypeRelationship).contains(operationType);
  }

  /**
   * Get the type relationship for this operation info
   *
   * @return the {@link NodeTypeRelationship}
   */
  public NodeTypeRelationship getNodeTypeRelationship() {

    switch (_sourceType) {
      case DIRECTORY:
        switch (_targetType) {
          case DIRECTORY:
            return NodeTypeRelationship.DIR_TO_DIR;
          case SYMBOLIC_LINK:
            return NodeTypeRelationship.DIR_TO_SYM_LINK;
          case HARDLINK:
            return NodeTypeRelationship.DIR_TO_HARD_LINK;
          case FILE:
            return NodeTypeRelationship.DIR_TO_FILE;
          case NONE:
          default:
            return NodeTypeRelationship.DIR_TO_NONE;
        }
      case FILE:
        switch (_targetType) {
          case DIRECTORY:
            return NodeTypeRelationship.FILE_TO_DIR;
          case FILE:
            return NodeTypeRelationship.FILE_TO_FILE;
          case SYMBOLIC_LINK:
            return NodeTypeRelationship.FILE_TO_SYM_LINK;
          case HARDLINK:
            return NodeTypeRelationship.FILE_TO_HARD_LINK;
          case NONE:
          default:
            return NodeTypeRelationship.FILE_TO_NONE;
        }
      case SYMBOLIC_LINK:
        switch (_targetType) {
          case DIRECTORY:
            return NodeTypeRelationship.SYM_LINK_TO_DIR;
          case FILE:
            return NodeTypeRelationship.SYM_LINK_TO_FILE;
          case SYMBOLIC_LINK:
            return NodeTypeRelationship.SYM_LINK_TO_SYM_LINK;
          case HARDLINK:
            return NodeTypeRelationship.SYM_LINK_TO_HARD_LINK;
          case NONE:
          default:
            return NodeTypeRelationship.SYM_LINK_TO_NONE;
        }
      case HARDLINK:
        switch (_targetType) {
          case DIRECTORY:
            return NodeTypeRelationship.HARD_LINK_TO_DIR;
          case FILE:
            return NodeTypeRelationship.HARD_LINK_TO_FILE;
          case SYMBOLIC_LINK:
            return NodeTypeRelationship.HARD_LINK_TO_SYM_LINK;
          case HARDLINK:
            return NodeTypeRelationship.HARD_LINK_TO_HARD_LINK;
          case NONE:
          default:
            return NodeTypeRelationship.HARD_LINK_TO_NONE;
        }
        // TODO - decide if NONE -> Other types should ever be a thing.
      case NONE:
      default:
        return NodeTypeRelationship.NONE;
    }
  }

  public FileSystemTreeNode getSourceNode() {
    return _sourceNode;
  }

  public FileSystemTreeNode getTargetNode() {
    return _targetNode;
  }

  public NodeType getSourceType() {
    return _sourceType;
  }

  public NodeType getTargetType() {
    return _targetType;
  }

  public OperationInfoType getOperationInfoType() {
    return _operationInfoType;
  }

  public boolean isOperationLegal(OperationType operationType) {
    return isOperationTypeSupportedForNodes(getNodeTypeRelationship(), operationType);
  }

  public enum OperationInfoType {
    SOURCE_UNARY, TARGET_UNARY, BINARY, NONE
  }

  public enum OperationType {
    CREATE, COPY, MOVE, DELETE, MODIFY
  }

  public enum NodeTypeRelationship {
    FILE_TO_FILE,
    FILE_TO_DIR,
    FILE_TO_SYM_LINK,
    FILE_TO_HARD_LINK,
    FILE_TO_NONE,
    DIR_TO_DIR,
    DIR_TO_SYM_LINK,
    DIR_TO_HARD_LINK,
    DIR_TO_FILE,
    DIR_TO_NONE,
    SYM_LINK_TO_SYM_LINK,
    SYM_LINK_TO_HARD_LINK,
    SYM_LINK_TO_FILE,
    SYM_LINK_TO_DIR,
    SYM_LINK_TO_NONE,
    HARD_LINK_TO_FILE,
    HARD_LINK_TO_DIR,
    HARD_LINK_TO_SYM_LINK,
    HARD_LINK_TO_HARD_LINK,
    HARD_LINK_TO_NONE,
    NONE
  }

  static {
    OPERATION_SUPPORT_MAP = new HashMap<>();

    // binary op types
    HashSet<OperationType> binaryOps = new HashSet<>(Arrays.asList(OperationType.MOVE, OperationType.COPY));

    // unary op types
    HashSet<OperationType> unaryOps = new HashSet<>(
        Arrays.asList(OperationType.DELETE, OperationType.CREATE, OperationType.COPY, OperationType.MODIFY));

    HashSet<OperationType> allOps = new HashSet<>(binaryOps);
    allOps.addAll(unaryOps);

    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.FILE_TO_FILE, binaryOps);
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.FILE_TO_DIR, binaryOps);
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.FILE_TO_SYM_LINK, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.FILE_TO_HARD_LINK, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.FILE_TO_NONE, allOps);
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.DIR_TO_DIR, binaryOps);
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.DIR_TO_SYM_LINK, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.DIR_TO_HARD_LINK, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.DIR_TO_FILE, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.DIR_TO_NONE, unaryOps);
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.SYM_LINK_TO_SYM_LINK, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.SYM_LINK_TO_HARD_LINK, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.SYM_LINK_TO_FILE, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.SYM_LINK_TO_DIR, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.SYM_LINK_TO_NONE, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.HARD_LINK_TO_FILE, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.HARD_LINK_TO_DIR, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.HARD_LINK_TO_SYM_LINK, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.HARD_LINK_TO_HARD_LINK, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.HARD_LINK_TO_NONE, Collections.emptySet());
    OPERATION_SUPPORT_MAP.put(NodeTypeRelationship.NONE, Collections.emptySet());
  }
}
