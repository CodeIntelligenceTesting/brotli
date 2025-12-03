package org.brotli.dec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream;
import org.apache.commons.compress.compressors.brotli.BrotliUtils;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueMedium;

public final class DiffFuzzTest {
  private static final int MAX_DECODED_BYTES = 1 << 20;
  private static final int BUFFER_SIZE = 4096;
  private static final boolean APACHE_AVAILABLE;

  static {
    boolean available;
    try {
      available = BrotliUtils.isBrotliCompressionAvailable();
    } catch (RuntimeException | LinkageError ignore) {
      available = false;
    }
    APACHE_AVAILABLE = available;
  }

  public static void fuzzerTestOneInput(byte[] data) {
    if (data == null || data.length == 0 ) {
      return;
    }

    if (!APACHE_AVAILABLE) {
      assert(false);
    }

    byte[] ours = null;
    byte[] apache = null;
    boolean oursSuccess = false;
    boolean apacheSuccess = false;

    // Brotli
    try {
      ours = decompressWithBrotli(data);
      oursSuccess = true;
    } catch (IOException | IllegalArgumentException | BrotliRuntimeException | IllegalStateException ignore) {
    }

    // Apache
    try {
      apache = decompressWithApache(data);
      apacheSuccess = true;
    } catch (IOException | IllegalArgumentException | IllegalStateException | BrotliRuntimeException | LinkageError ignore) {
    }

    if (!oursSuccess || !apacheSuccess) {
      return;
    }

    // Compare decompressed bytes
    if (!Arrays.equals(ours, apache)) {
      throw new FuzzerSecurityIssueMedium("Apache Commons and Brotli decoder outputs differ");
    }
  }

  private static byte[] decompressWithBrotli(byte[] data) throws IOException {
    try (BrotliInputStream brotli = new BrotliInputStream(new ByteArrayInputStream(data))) {
      brotli.enableLargeWindow();
      return readAll(brotli);
    }
  }

  private static byte[] decompressWithApache(byte[] data) throws IOException {
    try (BrotliCompressorInputStream brotli =
        new BrotliCompressorInputStream(new ByteArrayInputStream(data))) {
      return readAll(brotli);
    }
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream decoded = new ByteArrayOutputStream(Math.min(1024, MAX_DECODED_BYTES));
    byte[] buffer = new byte[BUFFER_SIZE];
    int total = 0;
    int read;
    while ((read = in.read(buffer, 0, buffer.length)) != -1) {
      total += read;
      /*if (total > MAX_DECODED_BYTES) {
        throw new FuzzerSecurityIssueMedium("Decoded output too large " + total + " > " + MAX_DECODED_BYTES);
      }*/
      decoded.write(buffer, 0, read);
    }
    return decoded.toByteArray();
  }
}
