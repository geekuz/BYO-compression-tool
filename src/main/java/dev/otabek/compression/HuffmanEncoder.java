package dev.otabek.compression;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Compresses a text file using Huffman coding.
 *
 * <p>Step 4 of the Huffman challenge — the write side. The input is scanned
 * once to count character frequencies, those frequencies are written as the
 * {@link Header}, and then the input is streamed a second time, emitting each
 * character's prefix code as a packed bitstream. Reading twice keeps memory use
 * bounded regardless of file size.
 */
final class HuffmanEncoder {

    private HuffmanEncoder() {
        // Utility class; not instantiable.
    }

    /**
     * Compresses {@code input} into {@code output}.
     *
     * @param input   the source text file
     * @param output  the destination for the compressed data (overwritten)
     * @param charset the charset used to decode the input
     * @throws IOException if reading or writing fails
     */
    static void encode(Path input, Path output, Charset charset) throws IOException {
        Map<Character, Long> frequencies = FrequencyCounter.countFromFile(input, charset);

        try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(output))) {
            DataOutputStream dataOut = new DataOutputStream(fileOut);
            Header.write(dataOut, frequencies);
            dataOut.flush();

            Optional<HuffmanNode> root = HuffmanTree.build(frequencies);
            if (root.isEmpty()) {
                // Empty input: a valid header with no entries and no payload.
                return;
            }

            Map<Character, String> codes = CodeTable.from(root.get());
            BitOutputStream bitOut = new BitOutputStream(fileOut);
            try (Reader reader = Files.newBufferedReader(input, charset)) {
                int read;
                while ((read = reader.read()) != -1) {
                    bitOut.writeBits(codes.get((char) read));
                }
            }
            bitOut.finish();
        }
    }
}
