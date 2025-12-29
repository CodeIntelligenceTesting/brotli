package org.brotli.dec;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;

public class RoundTripReproducerTest {

  private static final byte[] ORIGINAL_PAYLOAD = {
      0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x72, 0x65,
      0x67, 0x69, 0x73, 0x74, 0x65, 0x72, 0x20, 0x66,
      0x72, 0x65, 0x5f, 0x65, 0x74, 0x73, 0x70, 0x6f,
      0x5f
  };

  private static final byte[] COMPRESSED_PAYLOAD = {
      (byte) 0xa1, (byte) 0xc0, 0x00, (byte) 0xc0, 0x2f, 0x6e, 0x63, (byte) 0xba,
      (byte) 0x9e, (byte) 0xfb, (byte) 0xb9, (byte) 0xf4, 0x16, 0x54, 0x0b, 0x00,
      (byte) 0x89, 0x25, (byte) 0xb2, 0x28, 0x05, 0x7e, 0x20, (byte) 0xa2,
      (byte) 0xaa, 0x3b, 0x7f, 0x3b, 0x06
  };

  private static final byte[] RAW_DICTIONARY_CHUNK = {0x00, 0x00, 0x00, 0x00};

  @Test
  public void decompressionWithRawDictionaryChunkMatchesOriginal() throws IOException {
    byte[] decoded = decode(COMPRESSED_PAYLOAD, RAW_DICTIONARY_CHUNK);
    assertArrayEquals("Decoded bytes should match the original fuzz payload",
        ORIGINAL_PAYLOAD, decoded);
  }

  private static byte[] decode(byte[] compressed, byte[] dictionaryChunk) throws IOException {
    try (BrotliInputStream in = new BrotliInputStream(new ByteArrayInputStream(compressed))) {
      in.attachDictionaryChunk(dictionaryChunk);
      return in.readAllBytes();
    }
  }
}
