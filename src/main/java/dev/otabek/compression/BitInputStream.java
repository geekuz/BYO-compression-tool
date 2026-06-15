package dev.otabek.compression;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads individual bits from an underlying byte stream, unpacking eight bits
 * per byte most-significant-bit first. The mirror image of
 * {@link BitOutputStream}.
 */
final class BitInputStream implements Closeable {

    private static final int BITS_PER_BYTE = 8;

    private final InputStream in;
    private int currentByte;
    private int bitsRemaining;

    BitInputStream(InputStream in) {
        if (in == null) {
            throw new IllegalArgumentException("in must not be null");
        }
        this.in = in;
    }

    /**
     * Reads the next bit.
     *
     * @return {@code 0} or {@code 1}, or {@code -1} at end of stream
     * @throws IOException if reading fails
     */
    int readBit() throws IOException {
        if (bitsRemaining == 0) {
            currentByte = in.read();
            if (currentByte == -1) {
                return -1;
            }
            bitsRemaining = BITS_PER_BYTE;
        }
        bitsRemaining--;
        return (currentByte >> bitsRemaining) & 1;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
