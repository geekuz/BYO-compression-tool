package dev.otabek.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BitStreamTest {

    @Test
    @DisplayName("bits written are read back in the same order (MSB-first)")
    void roundTripExactByte() throws IOException {
        // Arrange: 1011 0010 == 0xB2
        String bits = "10110010";
        ByteArrayOutputStream sink = new ByteArrayOutputStream();

        // Act
        try (BitOutputStream out = new BitOutputStream(sink)) {
            out.writeBits(bits);
        }

        // Assert
        byte[] bytes = sink.toByteArray();
        assertEquals(1, bytes.length);
        assertEquals((byte) 0xB2, bytes[0]);
        assertEquals(bits, readBits(bytes, bits.length()));
    }

    @Test
    @DisplayName("a partial final byte is zero-padded but reads back correctly up to its length")
    void roundTripPartialByte() throws IOException {
        String bits = "101"; // 3 bits -> one byte 1010 0000
        ByteArrayOutputStream sink = new ByteArrayOutputStream();

        try (BitOutputStream out = new BitOutputStream(sink)) {
            out.writeBits(bits);
        }

        byte[] bytes = sink.toByteArray();
        assertEquals(1, bytes.length);
        assertEquals((byte) 0b10100000, bytes[0]);
        assertEquals(bits, readBits(bytes, bits.length()));
    }

    @Test
    @DisplayName("a long bit sequence spanning many bytes round-trips")
    void roundTripManyBytes() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append((i * 7 + 3) % 2);
        }
        String bits = sb.toString();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();

        try (BitOutputStream out = new BitOutputStream(sink)) {
            out.writeBits(bits);
        }

        assertEquals(bits, readBits(sink.toByteArray(), bits.length()));
    }

    @Test
    @DisplayName("readBit returns -1 at end of stream")
    void readReturnsMinusOneAtEof() throws IOException {
        try (BitInputStream in = new BitInputStream(new ByteArrayInputStream(new byte[0]))) {
            assertEquals(-1, in.readBit());
        }
    }

    @Test
    @DisplayName("writeBit rejects values other than 0 or 1")
    void writeBitRejectsBadValues() {
        BitOutputStream out = new BitOutputStream(new ByteArrayOutputStream());
        assertThrows(IllegalArgumentException.class, () -> out.writeBit(2));
    }

    private static String readBits(byte[] bytes, int count) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BitInputStream in = new BitInputStream(new ByteArrayInputStream(bytes))) {
            for (int i = 0; i < count; i++) {
                result.append(in.readBit());
            }
        }
        return result.toString();
    }
}
