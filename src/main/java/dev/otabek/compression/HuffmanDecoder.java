package dev.otabek.compression;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Decompresses a file produced by {@link HuffmanEncoder}.
 *
 * <p>Step 5 of the Huffman challenge — the read side. The {@link Header}'s
 * frequency table is used to rebuild a tree identical to the one used when
 * encoding. Bits are then read one at a time, descending left on {@code 0} and
 * right on {@code 1}; each time a leaf is reached its character is emitted and
 * the walk restarts from the root. Decoding stops once the known number of
 * characters (the sum of all frequencies) has been produced, so the trailing
 * zero-padding bits in the final byte are simply ignored.
 */
final class HuffmanDecoder {

    private HuffmanDecoder() {
        // Utility class; not instantiable.
    }

    /**
     * Decompresses {@code input} into {@code output}.
     *
     * @param input   the compressed source file
     * @param output  the destination for the restored text (overwritten)
     * @param charset the charset used to encode the output
     * @throws IOException if reading/writing fails or the input is not valid
     */
    static void decode(Path input, Path output, Charset charset) throws IOException {
        try (InputStream fileIn = new BufferedInputStream(Files.newInputStream(input));
                Writer writer = Files.newBufferedWriter(output, charset)) {

            DataInputStream dataIn = new DataInputStream(fileIn);
            Map<Character, Long> frequencies = Header.read(dataIn);

            long total = frequencies.values().stream().mapToLong(Long::longValue).sum();
            if (total == 0) {
                return; // empty input round-trips to an empty file
            }

            HuffmanNode root = HuffmanTree.build(frequencies).orElseThrow();
            writeDecoded(new BitInputStream(fileIn), root, total, writer);
        }
    }

    private static void writeDecoded(BitInputStream bitIn, HuffmanNode root, long total, Writer out)
            throws IOException {
        // A single-character input has a leaf root: emit that symbol `total` times.
        if (root instanceof HuffmanNode.Leaf leaf) {
            for (long i = 0; i < total; i++) {
                out.write(leaf.symbol());
            }
            return;
        }

        long emitted = 0;
        HuffmanNode node = root;
        while (emitted < total) {
            int bit = bitIn.readBit();
            if (bit == -1) {
                throw new IOException("Corrupt payload: stream ended before "
                        + total + " characters were decoded (" + emitted + " produced)");
            }
            HuffmanNode.Internal internal = (HuffmanNode.Internal) node;
            node = (bit == 0) ? internal.left() : internal.right();
            if (node instanceof HuffmanNode.Leaf leaf) {
                out.write(leaf.symbol());
                emitted++;
                node = root;
            }
        }
    }
}
