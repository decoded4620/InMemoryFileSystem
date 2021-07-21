package com.material.filesystem;

import java.util.Locale;


/**
 * This refers to the type of node, e.g. what file system object type it represents
 * if NONE, it means a non-existing node.
 */
public enum NodeType {
  FILE, DIRECTORY, SYMBOLIC_LINK, HARDLINK, NONE;
  public String getPrintableName() {
    return name().substring(0, 1).toLowerCase();
  }
}
