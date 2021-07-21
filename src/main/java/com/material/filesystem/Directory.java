package com.material.filesystem;

public class Directory extends DefaultFileSystemObject {
  public Directory(String name) {
    super(name);
  }

  @Override
  public Directory copy() {
    return new Directory(getName());
  }
}
