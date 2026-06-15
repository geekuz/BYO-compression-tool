package dev.otabek.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HuffmanTreeTest {

    @Test
    @DisplayName("returns empty for an empty frequency table")
    void emptyFrequencies() {
        assertTrue(HuffmanTree.build(Map.of()).isEmpty());
    }

    @Test
    @DisplayName("a single character produces a leaf root")
    void singleCharacter() {
        Optional<HuffmanNode> root = HuffmanTree.build(Map.of('a', 5L));

        HuffmanNode.Leaf leaf = assertInstanceOf(HuffmanNode.Leaf.class, root.orElseThrow());
        assertEquals('a', leaf.symbol());
        assertEquals(5L, leaf.frequency());
    }

    @Test
    @DisplayName("root frequency equals the sum of all character frequencies")
    void rootFrequencyIsTotal() {
        HuffmanNode root = HuffmanTree.build(Map.of('a', 3L, 'b', 2L, 'c', 1L)).orElseThrow();

        assertEquals(6L, root.frequency());
        assertInstanceOf(HuffmanNode.Internal.class, root);
    }

    @Test
    @DisplayName("construction is deterministic: equal frequencies yield the same tree")
    void deterministicForEqualFrequencies() {
        Map<Character, Long> frequencies = Map.of('a', 1L, 'b', 1L, 'c', 1L, 'd', 1L);

        HuffmanNode first = HuffmanTree.build(frequencies).orElseThrow();
        HuffmanNode second = HuffmanTree.build(frequencies).orElseThrow();

        // Records implement structural equality, so identical trees are equal.
        assertEquals(first, second);
    }

    @Test
    @DisplayName("rarer characters end up deeper than common ones")
    void rarerCharactersAreDeeper() {
        // 'a' is overwhelmingly common; it should sit at depth 1 with the shortest code.
        HuffmanNode root =
                HuffmanTree.build(Map.of('a', 100L, 'b', 5L, 'c', 4L, 'd', 3L)).orElseThrow();
        Map<Character, String> codes = CodeTable.from(root);

        assertEquals(1, codes.get('a').length());
        assertTrue(codes.get('b').length() > codes.get('a').length());
        assertTrue(codes.get('d').length() >= codes.get('b').length());
    }

    @Test
    @DisplayName("rejects a null frequency table")
    void rejectsNull() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> HuffmanTree.build(null));
    }

    @Test
    @DisplayName("the empty tree is a distinct, well-defined optional")
    void emptyIsEmptyOptional() {
        assertSame(Optional.empty(), HuffmanTree.build(Map.of()));
    }
}
