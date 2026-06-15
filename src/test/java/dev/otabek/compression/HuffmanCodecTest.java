package dev.otabek.compression;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HuffmanCodecTest {

    /** Encodes then decodes {@code content}, asserting the bytes survive unchanged. */
    private static void assertRoundTrips(String content, Path tempDir) throws IOException {
        Path original = tempDir.resolve("in.txt");
        Path compressed = tempDir.resolve("in.huf");
        Path restored = tempDir.resolve("out.txt");
        Files.writeString(original, content, StandardCharsets.UTF_8);

        HuffmanEncoder.encode(original, compressed, StandardCharsets.UTF_8);
        HuffmanDecoder.decode(compressed, restored, StandardCharsets.UTF_8);

        assertArrayEquals(
                Files.readAllBytes(original),
                Files.readAllBytes(restored),
                "round-trip must preserve content exactly");
    }

    @Test
    @DisplayName("typical English text round-trips and compresses")
    void englishText(@TempDir Path tempDir) throws IOException {
        String content = "the quick brown fox jumps over the lazy dog. ".repeat(100);
        assertRoundTrips(content, tempDir);

        assertTrue(Files.size(tempDir.resolve("in.huf")) < Files.size(tempDir.resolve("in.txt")),
                "compressed output should be smaller than the input");
    }

    @Test
    @DisplayName("an empty file round-trips to an empty file")
    void emptyFile(@TempDir Path tempDir) throws IOException {
        assertRoundTrips("", tempDir);
        assertEquals(0, Files.size(tempDir.resolve("out.txt")));
    }

    @Test
    @DisplayName("a single repeated character round-trips (degenerate single-leaf tree)")
    void singleCharacterRepeated(@TempDir Path tempDir) throws IOException {
        assertRoundTrips("aaaaaaaaaaaaaaaaaaaa", tempDir);
    }

    @Test
    @DisplayName("a single one-character file round-trips")
    void oneCharacter(@TempDir Path tempDir) throws IOException {
        assertRoundTrips("z", tempDir);
    }

    @Test
    @DisplayName("multi-byte UTF-8 text round-trips")
    void unicodeText(@TempDir Path tempDir) throws IOException {
        assertRoundTrips("Les Misérables — café, naïve, façade. 日本語のテキスト。\n".repeat(20), tempDir);
    }

    @Test
    @DisplayName("the challenge fixture (Les Misérables) round-trips and compresses well")
    void challengeFixtureRoundTrips(@TempDir Path tempDir) throws IOException {
        // Arrange: materialise the classpath fixture to a real file.
        Path original = tempDir.resolve("test.txt");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("test.txt")) {
            assertTrue(in != null, "test.txt must be on the classpath (src/main/resources)");
            Files.copy(in, original);
        }
        Path compressed = tempDir.resolve("test.huf");
        Path restored = tempDir.resolve("test.out.txt");

        // Act
        HuffmanEncoder.encode(original, compressed, StandardCharsets.UTF_8);
        HuffmanDecoder.decode(compressed, restored, StandardCharsets.UTF_8);

        // Assert
        assertArrayEquals(Files.readAllBytes(original), Files.readAllBytes(restored),
                "the decompressed fixture must match the original byte-for-byte");
        long originalSize = Files.size(original);
        long compressedSize = Files.size(compressed);
        assertTrue(compressedSize < originalSize * 0.7,
                "Huffman coding should compress English prose to well under 70% ("
                        + compressedSize + " vs " + originalSize + ")");
    }
}
