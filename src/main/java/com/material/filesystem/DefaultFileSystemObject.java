package com.material.filesystem;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Default {@link FileSystemObject} implementation
 */
public abstract class DefaultFileSystemObject implements FileSystemObject {
  private final ReadWriteLock _readWriteLock = new ReentrantReadWriteLock();
  private final Lock _readLock = _readWriteLock.readLock();
  private final Lock _writeLock = _readWriteLock.readLock();
  private final long _createdTime = System.currentTimeMillis();
  private long _lastUpdatedTime = System.currentTimeMillis();
  private volatile String _name;

  public DefaultFileSystemObject(String name) {
    _name = name;
  }

  protected void startRead() {
    _readLock.lock();
  }

  protected void completeRead() {
    _readLock.unlock();
  }

  protected void startWrite() {
    _writeLock.lock();
  }

  protected void completeWrite() {
    _writeLock.unlock();
  }

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    _name = name;
    setLastUpdatedTime();
  }

  @Override
  public long getCreationTime() {
    return _createdTime;
  }

  @Override
  public void setLastUpdatedTime() {
    _lastUpdatedTime = System.currentTimeMillis();
  }

  @Override
  public long getLastUpdatedTime() {
    return _lastUpdatedTime;
  }

  @Override
  public abstract FileSystemObject copy();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultFileSystemObject)) {
      return false;
    }
    DefaultFileSystemObject that = (DefaultFileSystemObject) o;
    return _name.equals(that._name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name);
  }
}
