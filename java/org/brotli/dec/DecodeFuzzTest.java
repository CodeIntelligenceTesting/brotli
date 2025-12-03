package org.brotli.dec;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public final class DecodeFuzzTest {
  private static final int MAX_DECODED_BYTES = 1 << 20;
  private static final int BUFFER_SIZE = 4096;

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {

    try {
      BrotliInputStream decoder = new BrotliInputStream(new ByteArrayInputStream(data.consumeBytes(data.consumeInt(1, 1000))));
      decoder.enableLargeWindow();

      byte[] buffer = new byte[BUFFER_SIZE];
      int total = 0;
      int read;

      byte[] dic = data.consumeBytes(100);
      if (dic.length > 0) {
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
    } catch (NullPointerException | IOException | IllegalArgumentException | BrotliRuntimeException ignored) {
    }
  }
}
