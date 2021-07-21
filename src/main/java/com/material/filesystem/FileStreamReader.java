package com.material.filesystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class FileStreamReader {
  private final ByteArrayOutputStream _outputStream;
  private final int _chunkSize;
  private int _bytesRead = 0;

  public FileStreamReader(ByteArrayOutputStream outputStream, int chunkSize) {
    _outputStream = outputStream;
    _chunkSize = chunkSize;
  }

  public void read(ByteArrayInputStream inputStream) throws IOException {
    int chunkBytesRead;
    byte[] data = new byte[_chunkSize];

    while ((chunkBytesRead = inputStream.read(data, 0, _chunkSize)) > 0) {
      _outputStream.write(data, 0, chunkBytesRead);
      _bytesRead += chunkBytesRead;
    }

    _outputStream.flush();
  }

  public ByteArrayOutputStream getOutputStream() {
    return _outputStream;
  }

  public int getChunkSize() {
    return _chunkSize;
  }

  public byte[] getBytes() {
    return _outputStream.toByteArray();
  }

  public int getBytesRead() {
    return _bytesRead;
  }
}
