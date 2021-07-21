package com.material.filesystem;

/**
 * This interface represents some object in the file system. This can be a File, Directory, or some type of link.
 */
public interface FileSystemObject {
  /**
   * Set upon creation, returns the creation time, in ms since 1970
   */
  long getCreationTime();

  /**
   * Set each time object is updated, current time  in ms since 1970
   */
  void setLastUpdatedTime();

  /**
   * returns the time of last update, in ms since 1970
   * @return the last updated time
   */
  long getLastUpdatedTime();

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
   * Creates a copy of this File System Object
   * @return FileSystemObject
   */
  FileSystemObject copy();
}
