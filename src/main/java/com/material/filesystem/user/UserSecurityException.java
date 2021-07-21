package com.material.filesystem.user;

public class UserSecurityException extends RuntimeException {
  public UserSecurityException(String message) {
    super(message);
  }

  public UserSecurityException(String message, Throwable cause) {
    super(message, cause);
  }
}
