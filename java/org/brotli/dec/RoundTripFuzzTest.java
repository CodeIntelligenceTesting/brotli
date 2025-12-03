package org.brotli.dec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.brotli.wrapper.enc.Encoder;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueMedium;

public final class RoundTripFuzzTest {
  private static final int MAX_INPUT_BYTES = 1 << 18;
  private static final int MAX_DECODED_BYTES = 1 << 20;
  private static final boolean JNI_AVAILABLE;

  static {
    boolean loaded = false;
    String jniLibrary = System.getProperty("BROTLI_JNI_LIBRARY");
    if (jniLibrary != null && !jniLibrary.isEmpty()) {
      try {
        System.load(new File(jniLibrary).getAbsolutePath());
        loaded = true;
      } catch (UnsatisfiedLinkError ignored) {
      }
    }
    JNI_AVAILABLE = loaded;
  }

  public static void fuzzerTestOneInput(byte[] data) {
    if (!JNI_AVAILABLE || data == null || data.length > MAX_INPUT_BYTES) {
      return;
    }

    try {
      byte[] compressed = Encoder.compress(data, new Encoder.Parameters().setQuality(4));
      try (BrotliInputStream brotli =
          new BrotliInputStream(new ByteArrayInputStream(compressed))) {
        ByteArrayOutputStream decoded =
            new ByteArrayOutputStream(Math.min(data.length + 16, MAX_DECODED_BYTES));
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = brotli.read(buffer, 0, buffer.length)) != -1) {
          total += read;
          if (total > MAX_DECODED_BYTES) {
            return;
          }
          decoded.write(buffer, 0, read);
        }
        if (!Arrays.equals(data, decoded.toByteArray())) {
          throw new FuzzerSecurityIssueMedium("Round-trip mismatch");
        }
      }
    } catch (IOException | IllegalArgumentException | BrotliRuntimeException ignored) {
    }
  }
}
