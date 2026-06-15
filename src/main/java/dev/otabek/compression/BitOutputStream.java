package dev.otabek.compression;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes individual bits to an underlying byte stream, packing eight bits per
 * byte most-significant-bit first.
 *
 * <p>On {@link #close()} any partially filled final byte is padded with zero
 * bits. Those padding bits are harmless because the decoder stops once it has
 * produced the known number of characters rather than reading to end-of-stream.
 */
final class BitOutputStream implements Closeable {

    private static final int BITS_PER_BYTE = 8;

    private final OutputStream out;
    private int currentByte;
    private int bitsFilled;

    BitOutputStream(OutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException("out must not be null");
        }
        this.out = out;
    }

    /** Writes a single bit; {@code bit} must be 0 or 1. */
    void writeBit(int bit) throws IOException {
        if ((bit & ~1) != 0) {
            throw new IllegalArgumentException("bit must be 0 or 1, was " + bit);
        }
        currentByte = (currentByte << 1) | bit;
        bitsFilled++;
        if (bitsFilled == BITS_PER_BYTE) {
            out.write(currentByte);
            currentByte = 0;
            bitsFilled = 0;
        }
    }

    /** Writes each character of {@code code} (a string of '0'/'1') as a bit. */
    void writeBits(String code) throws IOException {
        for (int i = 0; i < code.length(); i++) {
            writeBit(code.charAt(i) == '0' ? 0 : 1);
        }
    }

    /**
     * Writes any buffered bits as a final zero-padded byte and flushes the
     * underlying stream, but does <em>not</em> close it. Use this when the
     * underlying stream is shared (e.g. the header was written to it first) and
     * its owner is responsible for closing.
     */
    void finish() throws IOException {
        if (bitsFilled > 0) {
            currentByte <<= (BITS_PER_BYTE - bitsFilled);
            out.write(currentByte);
            currentByte = 0;
            bitsFilled = 0;
        }
        out.flush();
    }

    /** Flushes any buffered bits (zero-padded) and closes the underlying stream. */
    @Override
    public void close() throws IOException {
        finish();
        out.close();
    }
}
