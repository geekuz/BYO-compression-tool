package dev.otabek.compression;

/**
 * A node in a Huffman tree.
 *
 * <p>The tree is a closed hierarchy of exactly two shapes: a {@link Leaf} that
 * carries a single character, and an {@link Internal} node that joins two
 * subtrees. Every node knows its cumulative {@link #frequency()} and the
 * smallest character contained in its subtree ({@link #minSymbol()}), which is
 * used as a deterministic tie-breaker when two nodes have equal frequency.
 * Deterministic ordering is what lets the encoder and decoder rebuild the exact
 * same tree from the same frequency table.
 */
sealed interface HuffmanNode permits HuffmanNode.Leaf, HuffmanNode.Internal {

    /** The total occurrence count of every character in this subtree. */
    long frequency();

    /** The smallest character (by code-unit value) contained in this subtree. */
    char minSymbol();

    /** A leaf holding one character and how often it occurs. */
    record Leaf(char symbol, long frequency) implements HuffmanNode {
        @Override
        public char minSymbol() {
            return symbol;
        }
    }

    /** An internal node joining two subtrees. */
    record Internal(HuffmanNode left, HuffmanNode right, long frequency, char minSymbol)
            implements HuffmanNode {

        /** Joins two children, summing frequencies and tracking the smallest symbol. */
        static Internal of(HuffmanNode left, HuffmanNode right) {
            return new Internal(
                    left,
                    right,
                    left.frequency() + right.frequency(),
                    (char) Math.min(left.minSymbol(), right.minSymbol()));
        }
    }
}
