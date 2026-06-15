package dev.otabek.compression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serialises and parses the file header that precedes the compressed payload.
 *
 * <p>Step 4 of the Huffman challenge. Rather than serialise the tree directly,
 * the header stores the character-frequency table; the decoder rebuilds an
 * identical tree from it using the same deterministic {@link HuffmanTree}
 * construction. This keeps the format simple and self-describing.
 *
 * <p>Binary layout (big-endian, written via {@link DataOutputStream}):
 * <pre>
 *   bytes 0..3 : magic 'H' 'U' 'F' '1'
 *   int        : number of distinct characters (N)
 *   N entries  : char (2 bytes) symbol, long (8 bytes) frequency
 * </pre>
 * The compressed bitstream begins immediately after the final entry. The total
 * number of encoded characters is the sum of all frequencies and is therefore
 * not stored separately.
 */
final class Header {

    private static final byte[] MAGIC = {'H', 'U', 'F', '1'};

    private Header() {
        // Utility class; not instantiable.
    }

    /**
     * Writes the header for the given frequency table.
     *
     * @param out         destination stream; not closed by this method
     * @param frequencies character occurrence counts
     * @throws IOException if writing fails
     */
    static void write(DataOutputStream out, Map<Character, Long> frequencies) throws IOException {
        out.write(MAGIC);
        out.writeInt(frequencies.size());
        for (Map.Entry<Character, Long> entry : frequencies.entrySet()) {
            out.writeChar(entry.getKey());
            out.writeLong(entry.getValue());
        }
    }

    /**
     * Reads and validates a header, returning its frequency table.
     *
     * @param in source stream positioned at the start of the header
     * @return the parsed frequencies, preserving file order
     * @throws IOException if reading fails or the magic/contents are invalid
     */
    static Map<Character, Long> read(DataInputStream in) throws IOException {
        byte[] magic = new byte[MAGIC.length];
        in.readFully(magic);
        for (int i = 0; i < MAGIC.length; i++) {
            if (magic[i] != MAGIC[i]) {
                throw new IOException("Not a HUF1 compressed file (bad magic bytes)");
            }
        }

        int count = in.readInt();
        if (count < 0) {
            throw new IOException("Corrupt header: negative entry count " + count);
        }

        Map<Character, Long> frequencies = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            char symbol = in.readChar();
            long frequency = in.readLong();
            if (frequency <= 0) {
                throw new IOException("Corrupt header: non-positive frequency " + frequency);
            }
            frequencies.put(symbol, frequency);
        }
        return frequencies;
    }
}
