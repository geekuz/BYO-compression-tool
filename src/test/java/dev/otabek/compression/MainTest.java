package dev.otabek.compression;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    private static PrintStream silent() {
        return new PrintStream(new ByteArrayOutputStream());
    }

    @Test
    @DisplayName("returns usage exit code when no arguments are given")
    void noArguments() {
        int code = Main.run(new String[0], silent(), silent());
        assertEquals(Main.EXIT_USAGE, code);
    }

    @Test
    @DisplayName("returns usage exit code for an unknown command")
    void unknownCommand() {
        int code = Main.run(new String[] {"bogus", "x"}, silent(), silent());
        assertEquals(Main.EXIT_USAGE, code);
    }

    @Test
    @DisplayName("freq returns no-input exit code when the file does not exist")
    void freqMissingFile() {
        int code = Main.run(new String[] {"freq", "does-not-exist.txt"}, silent(), silent());
        assertEquals(Main.EXIT_NO_INPUT, code);
    }

    @Test
    @DisplayName("freq prints the frequency report and exits 0 for a valid file")
    void freqValidFile(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "aaab", StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Act
        int code = Main.run(new String[] {"freq", file.toString()},
                new PrintStream(out, true, StandardCharsets.UTF_8), silent());

        // Assert
        String report = out.toString(StandardCharsets.UTF_8);
        assertEquals(0, code);
        assertTrue(report.contains("'a'"), "report should mention character 'a'");
        assertTrue(report.contains("distinct characters: 2"), "report should count distinct chars");
    }

    @Test
    @DisplayName("encode then decode round-trips a file through the CLI")
    void encodeDecodeRoundTrip(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path original = tempDir.resolve("original.txt");
        Path compressed = tempDir.resolve("original.huf");
        Path restored = tempDir.resolve("restored.txt");
        Files.writeString(original,
                "the quick brown fox jumps over the lazy dog\n".repeat(50), StandardCharsets.UTF_8);

        // Act
        int encodeCode = Main.run(
                new String[] {"encode", original.toString(), compressed.toString()},
                silent(), silent());
        int decodeCode = Main.run(
                new String[] {"decode", compressed.toString(), restored.toString()},
                silent(), silent());

        // Assert
        assertEquals(0, encodeCode);
        assertEquals(0, decodeCode);
        assertArrayEquals(Files.readAllBytes(original), Files.readAllBytes(restored),
                "decoded output must match the original byte-for-byte");
        assertTrue(Files.size(compressed) < Files.size(original),
                "compressed file should be smaller than the original");
    }

    @Test
    @DisplayName("encode returns no-input exit code when the input is missing")
    void encodeMissingInput(@TempDir Path tempDir) {
        int code = Main.run(
                new String[] {"encode", "missing.txt", tempDir.resolve("o.huf").toString()},
                silent(), silent());
        assertEquals(Main.EXIT_NO_INPUT, code);
    }

    @Test
    @DisplayName("decode reports an I/O error when the input is not a HUF1 file")
    void decodeRejectsNonHufFile(@TempDir Path tempDir) throws IOException {
        Path notHuf = tempDir.resolve("plain.txt");
        Files.writeString(notHuf, "this is not compressed", StandardCharsets.UTF_8);
        int code = Main.run(
                new String[] {"decode", notHuf.toString(), tempDir.resolve("o.txt").toString()},
                silent(), silent());
        assertEquals(Main.EXIT_IO_ERROR, code);
    }
}
