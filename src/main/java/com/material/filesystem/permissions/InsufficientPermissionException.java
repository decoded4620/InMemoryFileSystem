package com.material.filesystem.permissions;

/**
 * Thrown if a user doesn't have the specified permissions to do something.
 */
public class InsufficientPermissionException extends RuntimeException {
  public InsufficientPermissionException() {
    super();
  }

  public InsufficientPermissionException(String message) {
    super(message);
  }
}
