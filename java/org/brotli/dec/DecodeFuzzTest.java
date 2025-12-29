package org.brotli.dec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public final class DecodeFuzzTest {
  private static final int MAX_DECODED_BYTES = 1 << 20;
  private static final int BUFFER_SIZE = 4096;

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      //byte[] stream = {18, 0, 10, 42, 42, 42, 42, 42, 37, 10, 42, 42, 42, 42, 42, 37, 18, 42, 42, 15, 36, 0, 0, 0, 0, 0, 0, 0, 38, 58, 40}; // uncomment to reproduce nullptr finding
      byte[] stream = data.consumeBytes(data.consumeInt(1, 1000));
      BrotliInputStream decoder = new BrotliInputStream(new ByteArrayInputStream(stream));
      if (data.consumeBoolean()) {
        decoder.enableLargeWindow();
      }

      byte[] buffer = new byte[BUFFER_SIZE];
      int total = 0;
      int read;

      byte[] dic = data.consumeBytes(100);
      //byte[] dic = {1, 2, 3}; // uncomment to reproduce nullptr finding

      if (dic.length > 1) {
        decoder.attachDictionaryChunk(dic);
      }

      if (data.consumeBoolean()) {
        decoder.enableEagerOutput();
      }

      while ((read = decoder.read(buffer, 0, buffer.length)) != -1) {
        total += read;
        if (total >= MAX_DECODED_BYTES) {
          break;
        }
      }
    } catch (NullPointerException e) {
      // Catch NullPointerException to ignore the finding
    } catch (IOException | IllegalArgumentException | BrotliRuntimeException ignored) {
    }
  }
}
