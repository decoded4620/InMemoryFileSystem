package com.material.filesystem.user;

public enum UserType {
  ROOT, ADMIN, USER, GUEST;

  public String getPrintableName() {
    return name().substring(0, 1).toLowerCase();
  }
}
