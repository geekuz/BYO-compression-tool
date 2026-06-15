package dev.otabek.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FrequencyCounterTest {

    @Nested
    @DisplayName("count(String)")
    class CountString {

        @Test
        @DisplayName("returns empty map for empty text")
        void emptyText() {
            // Arrange
            String text = "";

            // Act
            Map<Character, Long> result = FrequencyCounter.count(text);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("counts repeated characters")
        void repeatedCharacters() {
            // Arrange
            String text = "aaabbc";

            // Act
            Map<Character, Long> result = FrequencyCounter.count(text);

            // Assert
            assertEquals(3L, result.get('a'));
            assertEquals(2L, result.get('b'));
            assertEquals(1L, result.get('c'));
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("counts whitespace and punctuation as characters")
        void whitespaceAndPunctuation() {
            // Arrange
            String text = "a a.";

            // Act
            Map<Character, Long> result = FrequencyCounter.count(text);

            // Assert
            assertEquals(2L, result.get('a'));
            assertEquals(1L, result.get(' '));
            assertEquals(1L, result.get('.'));
        }

        @Test
        @DisplayName("throws on null text")
        void nullText() {
            assertThrows(IllegalArgumentException.class, () -> FrequencyCounter.count((String) null));
        }
    }

    @Nested
    @DisplayName("count(Reader) over the challenge fixture")
    class CountFixture {

        // The known-good character counts for the Coding Challenges example text
        // (Project Gutenberg "Les Misérables"): see challenge step 1 verification.
        private static final long EXPECTED_T = 223000L;
        private static final long EXPECTED_X = 333L;

        @Test
        @DisplayName("'t' occurs 223000 times and 'X' occurs 333 times")
        void matchesChallengeVerificationCounts() throws IOException {
            // Arrange
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("test.txt")) {
                assertTrue(in != null, "test.txt must be on the classpath (src/main/resources)");
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);

                // Act
                Map<Character, Long> result = FrequencyCounter.count(reader);

                // Assert
                assertEquals(EXPECTED_T, result.get('t'));
                assertEquals(EXPECTED_X, result.get('X'));
            }
        }
    }
}
