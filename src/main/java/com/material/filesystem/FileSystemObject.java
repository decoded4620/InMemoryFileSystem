package com.material.filesystem;

public interface FileSystemObject {
  /**
   * The name for this file system object.
   * @return aa String
   */
  String getName();

  /**
   * Update the name of this file system object.
   * @param name the new name.
   */
  void setName(String name);

  /**
   * Disable mutation on this FileSystemObject, this includes name change or content changes.
   * @return this FilelSystemObject
   */
  FileSystemObject makeImmutable();

  /**
   * Returns <pre>true</pre> if this object is immutable.
   * @return a boolean
   */
  boolean isImmutable();
}
