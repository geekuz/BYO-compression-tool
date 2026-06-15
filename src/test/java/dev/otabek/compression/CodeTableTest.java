package dev.otabek.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CodeTableTest {

    @Test
    @DisplayName("a single-leaf tree assigns the one-bit code \"0\"")
    void singleLeafGetsOneBitCode() {
        HuffmanNode root = HuffmanTree.build(Map.of('a', 7L)).orElseThrow();

        Map<Character, String> codes = CodeTable.from(root);

        assertEquals(Map.of('a', "0"), codes);
    }

    @Test
    @DisplayName("every character receives a code containing only 0 and 1")
    void codesAreBitStrings() {
        HuffmanNode root = HuffmanTree.build(Map.of('a', 5L, 'b', 2L, 'c', 1L)).orElseThrow();

        Map<Character, String> codes = CodeTable.from(root);

        assertEquals(3, codes.size());
        codes.values().forEach(code -> {
            assertFalse(code.isEmpty(), "code must not be empty");
            assertTrue(code.matches("[01]+"), "code must be a bit string, was: " + code);
        });
    }

    @Test
    @DisplayName("codes form a prefix-free set")
    void codesArePrefixFree() {
        HuffmanNode root =
                HuffmanTree.build(Map.of('a', 9L, 'b', 5L, 'c', 3L, 'd', 2L, 'e', 1L)).orElseThrow();

        Map<Character, String> codes = CodeTable.from(root);

        for (String a : codes.values()) {
            for (String b : codes.values()) {
                if (a != b) {
                    assertFalse(b.startsWith(a),
                            "code '" + a + "' is a prefix of '" + b + "'");
                }
            }
        }
    }

    @Test
    @DisplayName("rejects a null root")
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> CodeTable.from(null));
    }
}
