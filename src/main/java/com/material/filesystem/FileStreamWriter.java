package com.material.filesystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * This class writes byte to a target byte array from a {@link ByteArrayInputStream}
 */
public class FileStreamWriter {
  private final ByteArrayInputStream _inputStream;
  private final int _contentWriteStartIdx;
  private final int _chunkSize;
  private byte[] _bytesWritten;

  /**
   * Constructor
   *
   * @param inputStream the input (source) bytes to write from
   * @param contentWriteStartIdx the start index of the current content to write into
   * @param chunkSize a chunk size to write
   */
  public FileStreamWriter(ByteArrayInputStream inputStream, int contentWriteStartIdx, int chunkSize) {
    _inputStream = inputStream;
    _contentWriteStartIdx = contentWriteStartIdx;
    _chunkSize = chunkSize;
  }

  /**
   * Performs the write loop
   * @param buffer the {@link ByteArrayOutputStream} to write to.
   * @param existingBytes the destination byte[]
   * @throws IOException if reading / writing throws an {@link IOException}
   */
  public void write(ByteArrayOutputStream buffer, byte[] existingBytes) throws IOException {
    int chunkBytesRead;
    byte[] data = new byte[_chunkSize];

    while ((chunkBytesRead = _inputStream.read(data, 0, _chunkSize)) > 0) {
      buffer.write(data, 0, chunkBytesRead);
    }

    buffer.flush();
    byte[] writeContents = buffer.toByteArray();

    if (_contentWriteStartIdx > existingBytes.length) {
      throw new ArrayIndexOutOfBoundsException("Write index must be within content bounds");
    }

    int len = writeContents.length;

    if (existingBytes.length < _contentWriteStartIdx + len) {
      _bytesWritten = new byte[_contentWriteStartIdx + len];
    } else {
      _bytesWritten = new byte[existingBytes.length];
    }
    // copy any existing content up to the write start index
    if (existingBytes.length > 0) {
      // copy existing bytes until the startIndex byte
      if (_contentWriteStartIdx > 0) {
        System.arraycopy(existingBytes, 0, _bytesWritten, 0, _contentWriteStartIdx);
      }

      if (_contentWriteStartIdx + len < existingBytes.length) {
        System.arraycopy(existingBytes, _contentWriteStartIdx + len, _bytesWritten, _contentWriteStartIdx + len,
            existingBytes.length - (_contentWriteStartIdx + len));
      }
    }

    if (len > 0) {
      // write the remaining
      System.arraycopy(writeContents, 0, _bytesWritten, _contentWriteStartIdx, len);
    }
  }

  public byte[] getBytesWritten() {
    return _bytesWritten;
  }
}
