package dev.otabek.compression;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Builds a Huffman tree from a character-frequency table.
 *
 * <p>Step 2 of the Huffman challenge. The classic greedy algorithm: seed a
 * priority queue with one leaf per character, then repeatedly remove the two
 * lowest-frequency nodes and join them under a new internal node until a single
 * root remains.
 *
 * <p>Ties (equal frequency) are broken by the smallest character in the
 * subtree, so the construction is fully deterministic. That determinism is
 * essential: the decoder rebuilds the tree from the same frequency table and
 * must arrive at an identical shape, otherwise the codes would not match.
 */
final class HuffmanTree {

    /** Lowest frequency first; ties broken by smallest contained character. */
    private static final Comparator<HuffmanNode> ORDER =
            Comparator.comparingLong(HuffmanNode::frequency).thenComparing(HuffmanNode::minSymbol);

    private HuffmanTree() {
        // Utility class; not instantiable.
    }

    /**
     * Builds the Huffman tree for the given frequencies.
     *
     * @param frequencies character occurrence counts; must not be {@code null}
     * @return the tree root, or empty if {@code frequencies} is empty
     */
    static Optional<HuffmanNode> build(Map<Character, Long> frequencies) {
        if (frequencies == null) {
            throw new IllegalArgumentException("frequencies must not be null");
        }
        if (frequencies.isEmpty()) {
            return Optional.empty();
        }

        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>(ORDER);
        frequencies.forEach((symbol, count) -> queue.add(new HuffmanNode.Leaf(symbol, count)));

        while (queue.size() > 1) {
            HuffmanNode left = queue.poll();
            HuffmanNode right = queue.poll();
            queue.add(HuffmanNode.Internal.of(left, right));
        }

        return Optional.of(queue.poll());
    }
}
