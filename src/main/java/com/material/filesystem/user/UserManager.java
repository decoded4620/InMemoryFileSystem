package com.material.filesystem.user;

/**
 * Basic interface for a naive user manager, enabling login / logout sessions
 */
public interface UserManager {
  /**
   * Login a user.
   *
   * @param username a User Name e.g. cpark
   * @param password a Password e.g. 123
   * @return a User if login was successful.
   * @throws UserSecurityException if the user cannot be logged in.
   */
  User login(String username, String password) throws UserSecurityException;

  /**
   * Logs the current user out.
   * @throws UserSecurityException if the current user cannot be logged out
   */
  void logout() throws UserSecurityException;

  /**
   * Validate there is a user logged in  on the current channel
   * @throws UserSecurityException if no user is logged in.
   */
  void checkLoggedIn() throws UserSecurityException;

  /**
   * Return the current user
   * @return a User
   */
  User currentUser();

  /**
   * Get a user by name. Used for setting permissions, etc.
   * @param userName a Username to fetch.
   * @return the user
   * @throws UserSecurityException if the user is not found
   */
  User getUser(String userName) throws UserSecurityException;
}
