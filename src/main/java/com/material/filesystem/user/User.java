package com.material.filesystem.user;

public class User {
  private final String _username;
  private final String _passwordHash;
  private final UserType _userType;

  public User(String username, String passwordHash, UserType userType) {
    _username = username;
    _passwordHash = passwordHash;
    _userType = userType;
  }

  public String getPasswordHash() {
    return _passwordHash;
  }

  public String getUsername() {
    return _username;
  }

  public UserType getUserType() {
    return _userType;
  }
}
