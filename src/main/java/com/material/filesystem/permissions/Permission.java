package com.material.filesystem.permissions;

public enum Permission {
  READ, WRITE, DELETE;

  public String getPrintablePerm() {
    return this.name().toLowerCase().substring(0, 1);
  }
}
