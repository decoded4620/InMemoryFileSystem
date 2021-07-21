package com.material.filesystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;


public class File extends DefaultFileSystemObject {

  private byte[] _contents = new byte[0];

  private int _chunkSize = 4;

  public File(String name) {
    super(name);
  }

  public int size() {
    startRead();
    try {
      return _contents.length;
    } finally {
      completeRead();
    }
  }

  public void setChunkSize(int chunkSize) {
    startWrite();
    try {
      // force any readers to wait in case they will stream bytes.
      this._chunkSize = chunkSize;
    } finally {
      completeWrite();
    }
  }

  public byte[] getContentRange(int start, int length) {
    startRead();
    try {
      return Arrays.copyOfRange(_contents, start, Math.max(start + length, _contents.length));
    } finally {
      completeRead();
    }
  }

  public void readContentStream(ByteArrayOutputStream buffer) throws IOException {
    readContentStream(new FileStreamReader(buffer, _chunkSize));
  }

  public void readContentStream(FileStreamReader streamReader) throws IOException {
    readContentStream(streamReader, 0, _contents.length);
  }

  public void readContentStream(FileStreamReader streamReader, int offset, int length) throws IOException {
    startRead();
    try {
      streamReader.read(new ByteArrayInputStream(_contents, offset, length));
    } finally {
      completeRead();
    }
  }

  /**
   * Write content to a file as an input stream
   * @param inputStream A {@link ByteArrayInputStream} which will supply chunks to write to the file contents
   * @param contentWriteStartIdx the starting index to write. If contentWriteIdx is the current content length we append
   */
  public void writeContentStream(ByteArrayInputStream inputStream, int contentWriteStartIdx) throws IOException {
    writeContentStream(new FileStreamWriter(inputStream, contentWriteStartIdx, _chunkSize));
  }

  public void writeContentStream(FileStreamWriter writer) throws IOException {
    startWrite();
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      writer.write(buffer, _contents);
      _contents = writer.getBytesWritten();
      setLastUpdatedTime();
    } finally {
      completeWrite();
    }
  }

  /**
   * Read the content in  single chunk.
   * @return the byte[] array
   */
  public byte[] getContents() {
    startRead();
    try {
      return _contents;
    } finally {
      completeRead();
    }
  }

  public void setContents(byte[] bytes) {
    startWrite();
    try {
      _contents = bytes;
      setLastUpdatedTime();
    } finally {
      completeWrite();
    }
  }

  @Override
  public File copy() {
    startRead();
    try {
      File copy = new File(getName());
      byte[] contents = getContents();
      copy.setContents(Arrays.copyOf(contents, contents.length));
      copy.setChunkSize(_chunkSize);
      return copy;
    } finally {
      completeRead();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    File file = (File) o;
    return super.equals(o) && Arrays.equals(_contents, file._contents);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(_contents);
  }
}
