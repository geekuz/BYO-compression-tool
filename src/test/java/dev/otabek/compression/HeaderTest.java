package dev.otabek.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HeaderTest {

    @Test
    @DisplayName("a frequency table survives a write/read round-trip")
    void roundTrip() throws IOException {
        Map<Character, Long> frequencies = Map.of('a', 100L, 'b', 50L, '\n', 7L);

        Map<Character, Long> parsed = roundTripThrough(frequencies);

        assertEquals(frequencies, parsed);
    }

    @Test
    @DisplayName("an empty frequency table round-trips to an empty map")
    void emptyTable() throws IOException {
        assertEquals(Map.of(), roundTripThrough(Map.of()));
    }

    @Test
    @DisplayName("reading a stream without the magic bytes fails")
    void rejectsBadMagic() {
        byte[] notHuf = "XXXX....".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(notHuf));

        IOException ex = assertThrows(IOException.class, () -> Header.read(in));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("magic"));
    }

    private static Map<Character, Long> roundTripThrough(Map<Character, Long> frequencies)
            throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(sink)) {
            Header.write(out, frequencies);
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(sink.toByteArray()))) {
            return Header.read(in);
        }
    }
}
