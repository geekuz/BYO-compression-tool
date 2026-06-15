package dev.otabek.compression;

import java.util.HashMap;
import java.util.Map;

/**
 * Derives the prefix-code table from a Huffman tree.
 *
 * <p>Step 3 of the Huffman challenge. Walking the tree from the root, a left
 * branch appends a {@code 0} and a right branch appends a {@code 1}; the bits
 * accumulated on the path to each leaf form that character's code. Codes are
 * returned as strings of {@code '0'}/{@code '1'} characters for clarity.
 *
 * <p>A tree consisting of a single leaf (the input used only one distinct
 * character) is a special case: the path to that leaf is empty, so the
 * character is assigned the one-bit code {@code "0"} to keep it representable.
 */
final class CodeTable {

    private CodeTable() {
        // Utility class; not instantiable.
    }

    /**
     * Builds the character-to-code mapping for the given tree.
     *
     * @param root the Huffman tree root; must not be {@code null}
     * @return an immutable map of character to its bit-string code
     */
    static Map<Character, String> from(HuffmanNode root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        Map<Character, String> codes = new HashMap<>();
        if (root instanceof HuffmanNode.Leaf leaf) {
            codes.put(leaf.symbol(), "0");
        } else {
            assignCodes(root, new StringBuilder(), codes);
        }
        return Map.copyOf(codes);
    }

    private static void assignCodes(
            HuffmanNode node, StringBuilder path, Map<Character, String> codes) {
        if (node instanceof HuffmanNode.Leaf leaf) {
            codes.put(leaf.symbol(), path.toString());
            return;
        }
        HuffmanNode.Internal internal = (HuffmanNode.Internal) node;
        assignCodes(internal.left(), path.append('0'), codes);
        path.deleteCharAt(path.length() - 1);
        assignCodes(internal.right(), path.append('1'), codes);
        path.deleteCharAt(path.length() - 1);
    }
}
