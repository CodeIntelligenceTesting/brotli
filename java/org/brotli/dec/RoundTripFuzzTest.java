package org.brotli.dec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.brotli.wrapper.enc.Encoder;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueMedium;
import org.brotli.common.SharedDictionaryType;
import org.brotli.enc.PreparedDictionary;
import org.brotli.wrapper.common.BrotliCommon;
import org.brotli.wrapper.enc.BrotliOutputStream;

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

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) sb.append(String.format("%02x ", b));
    return sb.toString();
  }

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    if (!JNI_AVAILABLE) {
      return;
    }

    try {
      ByteBuffer dictionaryBuffer = null;
      PreparedDictionary preparedDictionary = null;
      byte[] dictionaryBytes = null;
      Encoder.Parameters params = new Encoder.Parameters();

      // Random parameters
      if (data.consumeBoolean()) {
        params.setQuality(data.consumeInt(0, 12));
      }
      if (data.consumeBoolean()) {
        params.setWindow(data.consumeInt(0, 15));
      }
      if (data.consumeBoolean()) {
        Encoder.Mode[] modes = Encoder.Mode.values();
        params.setMode(modes[data.consumeInt(0, modes.length - 1)]);
      }

      if (data.consumeBoolean()) {
        dictionaryBytes = data.consumeBytes(data.consumeInt(1, 100));
        dictionaryBuffer = BrotliCommon.makeNative(dictionaryBytes);
        try {
          preparedDictionary = Encoder.prepareDictionary(dictionaryBuffer, SharedDictionaryType.RAW);
        } catch (IllegalStateException e) {
          // OOM
          return;
        }
        dictionaryBuffer.clear();
      }

      byte[] payload = data.consumeBytes(data.consumeInt(1, 1000));
      byte[] compressed;
      if (preparedDictionary != null) {
        ByteArrayOutputStream dst = new ByteArrayOutputStream();
        int bufferSize = Math.max(payload.length, 1);
        try (BrotliOutputStream encoder =
                 new BrotliOutputStream(dst, params, bufferSize)) {
          encoder.attachDictionary(preparedDictionary);
          encoder.write(payload);
        }
        compressed = dst.toByteArray();
        dictionaryBuffer.clear();
      } else {
        compressed = Encoder.compress(payload, params);
      }
      BrotliInputStream decoder = new BrotliInputStream(new ByteArrayInputStream(compressed));

      if (dictionaryBytes != null) {
        decoder.attachDictionaryChunk(dictionaryBytes);
      }

      if (data.consumeBoolean()) {
        decoder.enableLargeWindow();
      }

      if (data.consumeBoolean()) {
        decoder.enableEagerOutput();
      }

      byte[] uncompressed = decoder.readAllBytes();

      if (!Arrays.equals(uncompressed, payload)) {
        System.err.println("original payload: " + bytesToHex(payload));
        System.err.println("uncompressed:     " + bytesToHex(uncompressed));
        System.err.println("compressed:       " + bytesToHex(compressed));
        if (dictionaryBytes != null) {
          System.err.println("dictionary: " + bytesToHex(dictionaryBytes));
        }
        throw new FuzzerSecurityIssueMedium("Round-trip mismatch");
      }
    } catch (IOException | IllegalArgumentException | BrotliRuntimeException ignored) {
    }
  }
}
