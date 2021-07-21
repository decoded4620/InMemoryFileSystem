package com.material.filesystem.user;

/**
 * No Security implementation for testing only. Always uses the ADMIN USER
 */
public class TestUserManager implements UserManager {
  public static final User TEST_ADMIN_USER = new User("testAdmin", "abc", UserType.ADMIN);
  private final ThreadLocal<User> _currentUser = ThreadLocal.withInitial(() -> TEST_ADMIN_USER);

  public TestUserManager() {
  }

  @Override
  public void logout() {
    // no op
    _currentUser.set(null);
  }

  @Override
  public User login(String username, String password) {
    return _currentUser.get();
  }

  @Override
  public void checkLoggedIn() throws UserSecurityException {
    // noop  for basic testing
  }

  @Override
  public User currentUser() {
    return _currentUser.get();
  }

  @Override
  public User getUser(String userName) throws UserSecurityException {
    return TEST_ADMIN_USER;
  }
}
