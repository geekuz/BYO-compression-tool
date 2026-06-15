package dev.otabek.compression;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Counts how often each character occurs in a source of text.
 *
 * <p>Step 1 of the Huffman challenge: build the character frequency table
 * that later drives construction of the Huffman tree.
 */
public final class FrequencyCounter {

    private FrequencyCounter() {
        // Utility class; not instantiable.
    }

    /**
     * Counts character frequencies in the given text.
     *
     * @param text the text to scan; must not be {@code null}
     * @return an immutable map of character to occurrence count
     */
    public static Map<Character, Long> count(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        Map<Character, Long> frequencies = new HashMap<>();
        for (int i = 0; i < text.length(); i++) {
            frequencies.merge(text.charAt(i), 1L, Long::sum);
        }
        return Map.copyOf(frequencies);
    }

    /**
     * Counts character frequencies by streaming characters from a reader.
     * The reader is fully consumed but not closed.
     *
     * @param reader the character source; must not be {@code null}
     * @return an immutable map of character to occurrence count
     * @throws IOException if reading fails
     */
    public static Map<Character, Long> count(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException("reader must not be null");
        }
        Map<Character, Long> frequencies = new HashMap<>();
        int read;
        while ((read = reader.read()) != -1) {
            frequencies.merge((char) read, 1L, Long::sum);
        }
        return Map.copyOf(frequencies);
    }

    /**
     * Counts character frequencies in a file, decoding with the given charset.
     *
     * @param path    the file to read; must not be {@code null}
     * @param charset the charset used to decode bytes into characters
     * @return an immutable map of character to occurrence count
     * @throws IOException if the file cannot be read
     */
    public static Map<Character, Long> countFromFile(Path path, Charset charset) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (charset == null) {
            throw new IllegalArgumentException("charset must not be null");
        }
        try (Reader reader = Files.newBufferedReader(path, charset)) {
            return count(reader);
        }
    }
}
