package com.material.filesystem.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Naive implementation of a {@link UserManager} which reads the users text file
 * creates an in memory user db, and supports naive login and logout.
 *
 * NOTE: Security is low here due to time constraints.
 * NOTE: User Groups not supported yet due to time constraints
 */
public class DefaultUserManager implements UserManager {
  public static final User GUEST_USER = new User("guest", "", UserType.GUEST);
  private static final Logger LOG = LoggerFactory.getLogger(DefaultUserManager.class);
  public static User ROOT_USER;
  private final ThreadLocal<User> _currentUser = ThreadLocal.withInitial(() -> GUEST_USER);
  private final Map<String, User> _userDb = new HashMap<>();
  private final Map<String, User> _loggedInUsers = new HashMap<>();

  private final Lock _loggedInUsersLock = new ReentrantLock();

  // demonstration purpose only
  private final boolean _allowDuplicateSessions;

  public DefaultUserManager() {
    this(false);
  }

  public DefaultUserManager(boolean allowDuplicateSessions) {
    _allowDuplicateSessions = allowDuplicateSessions;
    // the stream holding the file content
    InputStream is = getClass().getClassLoader().getResourceAsStream("users.txt");
    if (is != null) {

      StringBuilder userTextBuilder = new StringBuilder();
      try (Reader reader = new BufferedReader(
          new InputStreamReader(is, Charset.forName(StandardCharsets.UTF_8.name())))) {
        int c = 0;
        while ((c = reader.read()) != -1) {
          userTextBuilder.append((char) c);
        }

        String usersTxt = userTextBuilder.toString();

        String[] userLines = usersTxt.split("\n");
        Arrays.stream(userLines).map(line -> line.split(" ")).filter(parts -> {
          boolean validLength = parts.length == 3;
          if (!validLength) {
            // TODO redact
            LOG.error("users.txt entry invalid: " + Arrays.toString(parts));
          }
          return validLength;
        }).forEach(parts -> {
          User user = new User(parts[0], parts[1], UserType.valueOf(parts[2]));
          if ("root".equals(user.getUsername())) {
            ROOT_USER = user;
          }
          _userDb.put(parts[0], user);
        });

        if (!_userDb.isEmpty()) {
          LOG.info("Loaded " + _userDb.size() + " authorized users");
        }

        _userDb.put("guest", GUEST_USER);
      } catch (IOException ex) {
        // no users loaded
        throw new UserSecurityException("No Users were loaded", ex);
      }
    }
  }

  // NOTE - normally the client would create the hash before passing on the network
  // but this was faster for demonstration purpose
  public User login(String username, String password) throws UserSecurityException {
    if (username == null || password == null || "".equals(username)) {
      throw new UserSecurityException("Username and/or password was invalid");
    }

    if (_currentUser.get() == null || _currentUser.get() == GUEST_USER) {
      String passwordMd5 = "".equals(password) ? "" : DigestUtils.md5Hex(password);
      _loggedInUsersLock.lock();
      try {
        if (_userDb.containsKey(username)) {
          User user = _userDb.get(username);
          if (_allowDuplicateSessions || !_loggedInUsers.containsKey(username)) {
            if (passwordMd5.equals(user.getPasswordHash())) {
              _currentUser.set(user);
              _loggedInUsers.put(username, user);
            } else {
              throw new UserSecurityException("Username and/or password was invalid");
            }
          } else {
            throw new UserSecurityException("User " + username
                + " is already logged in in another terminal, logout first, or enable multiple sessions");
          }
          return user;
        } else {
          throw new UserSecurityException("Username and/or password was invalid");
        }
      } finally {
        _loggedInUsersLock.unlock();
      }
    } else {
      throw new UserSecurityException("A user is already logged in on this channel, logout first");
    }
  }

  @Override
  public void logout() throws UserSecurityException {
    if (_currentUser.get() == null) {
      throw new UserSecurityException("User not logged in");
    } else {
      _loggedInUsersLock.lock();
      try {
        User user = _currentUser.get();

        if (user != GUEST_USER) {
          if (_loggedInUsers.get(user.getUsername()) != user) {
            throw new UserSecurityException(
                "User objects are not the same for " + user.getUsername() + " someone is trying to hack!");
          }
          _loggedInUsers.remove(user.getUsername());
          _currentUser.set(GUEST_USER);
          LOG.info("User " + user.getUsername() + " is logged out");
        } else {
          throw new UserSecurityException("Cannot logout the guest user");
        }
      } finally {
        _loggedInUsersLock.unlock();
      }
    }
  }

  @Override
  public void checkLoggedIn() throws UserSecurityException {
    if (_currentUser.get() == GUEST_USER) {
      throw new UserSecurityException("User not logged in");
    }
  }

  @Override
  public User currentUser() {
    return _currentUser.get();
  }

  @Override
  public User getUser(String userName) throws UserSecurityException {
    if (!_userDb.containsKey(userName)) {
      throw new UserSecurityException("User " + userName + " was not found");
    }

    if (!_currentUser.get().getUsername().equals(userName) && _currentUser.get().getUserType() != UserType.ADMIN
        && _currentUser.get().getUserType() != UserType.ROOT) {
      throw new UserSecurityException(
          "Only admin users can get other users info. Users must be logged in to get their own info");
    }

    return _userDb.get(userName);
  }
}
