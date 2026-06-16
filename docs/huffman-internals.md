# Huffman Internals — Deep Dive

A complete walkthrough of how this tool compresses and decompresses, written so
you (or anyone) can rebuild the full mental model later without re-reading every
source file. The [README](../README.md) has the quick overview; this is the
reference.

> **Source of truth:** the code in `src/main/java/dev/otabek/compression/`.
> If this doc and the code disagree, the code wins — update this doc.

---

## 1. The core idea

Normally every character costs a fixed width (8 or 16 bits). Huffman coding
instead gives **frequent characters short bit-codes and rare characters long
ones**. As long as common characters dominate the text, the total shrinks.

The codes are **prefix codes**: no character's code is a prefix of another's.
That single property is what lets the decoder read a continuous bitstream with
**no separators** and still know exactly where one character ends and the next
begins. Prefix-freeness falls out naturally from the construction: every
character lives at a *leaf* of a binary tree, and a code is the path from the
root to that leaf — so no code can be a prefix of another (a leaf is never on
the path to another leaf).

---

## 2. The central invariant (read this first)

Everything below hinges on one rule:

> **The encoder and decoder must build a byte-for-byte identical Huffman tree
> from the same frequency table.**

Because of this invariant, the compressed file stores **only the frequency
table**, not the tree and not the codes. The decoder reconstructs the tree by
running the *exact same* build algorithm on the *exact same* frequencies.

This is why tree construction must be **deterministic**. See §4.

---

## 3. Class map (who does what)

Each class maps to a step of the
[codingchallenges.fyi Huffman challenge](https://codingchallenges.fyi/challenges/challenge-huffman/).

| Class | Step | Responsibility |
|-------|------|----------------|
| `FrequencyCounter` | 1 | Count how often each character occurs |
| `HuffmanNode` | 2 | The two node shapes (`Leaf`, `Internal`) as a sealed type |
| `HuffmanTree` | 2 | Greedy tree construction from frequencies |
| `CodeTable` | 3 | Walk the tree → `{char → "0110…"}` code map |
| `Header` | 4 | Serialize/parse the `HUF1` file header (the frequency table) |
| `BitOutputStream` | 4 | Pack `0/1` bits into bytes, MSB-first |
| `BitInputStream` | 5 | Unpack bytes back into bits, MSB-first |
| `HuffmanEncoder` | 4 | Orchestrates the compress (write) side |
| `HuffmanDecoder` | 5 | Orchestrates the decompress (read) side |
| `Main` | — | CLI dispatch: `encode` / `decode` / `freq` |

Design style: many small, single-responsibility classes; utility classes have
private constructors; node types are immutable `record`s.

---

## 4. The determinism detail (why it actually works)

`HuffmanTree` seeds a `PriorityQueue` with one `Leaf` per character and
repeatedly merges the two lowest nodes. The comparator is:

```java
Comparator.comparingLong(HuffmanNode::frequency)   // primary: lowest freq first
          .thenComparing(HuffmanNode::minSymbol);   // tie-break: smallest char
```

- **Primary key — frequency.** Standard Huffman greedy choice.
- **Tie-breaker — `minSymbol`.** When two nodes have equal frequency, the queue
  would otherwise pull them in arbitrary order, producing a *different tree
  shape on different runs/JVMs*. Different shape → different codes → the decoder
  rebuilds a mismatched tree → garbage output.

`minSymbol` is the smallest character anywhere in a node's subtree. For a
`Leaf` it's the symbol itself; for an `Internal` node it's
`min(left.minSymbol, right.minSymbol)`, computed in `Internal.of(...)` so the
tie-breaker propagates correctly up the tree as nodes merge.

Result: the same frequency table always yields the same tree, so encode and
decode agree. **Do not remove or weaken the tie-breaker.**

`HuffmanNode` is a `sealed interface permits Leaf, Internal` — the compiler
knows there are exactly two cases, which makes the `instanceof`/`switch` logic
in `CodeTable` and `HuffmanDecoder` provably exhaustive.

---

## 5. The compressed file format (`HUF1`)

Written big-endian via `DataOutputStream` in `Header.write`, parsed by
`Header.read`:

```
┌─────────────────────────────────────────────────────────────┐
│ bytes 0..3 : magic  'H' 'U' 'F' '1'                          │  file-type marker
│ int (4 B)  : N = number of distinct characters               │
│ N entries  : char (2 B) symbol  +  long (8 B) frequency      │  the frequency table
│ …          : packed bitstream                                │  the payload
└─────────────────────────────────────────────────────────────┘
```

Notes:

- **Magic bytes** let `decode` reject non-`HUF1` input with a clear error
  instead of producing nonsense.
- **The total character count is NOT stored.** It is recovered as the *sum of
  all frequencies*. The decoder uses that sum to know exactly when to stop (see
  §7), which is also why trailing pad bits are harmless.
- The payload is `8 bits/byte, MSB-first, with the final byte zero-padded`.
- `Header.read` validates against corruption: rejects a negative entry count and
  any non-positive frequency.

---

## 6. ENCODING — step by step (`HuffmanEncoder.encode`)

The input file is read **twice on purpose** — this keeps memory bounded no
matter how large the file is (we never hold the whole file in memory).

```
PASS 1 ──► FrequencyCounter.countFromFile(input)
              → {char → freq}, e.g. {'a':50, 'b':12, ' ':40, …}
              (streams char-by-char; merge(ch, 1, Long::sum))

WRITE  ──► Header.write(dataOut, frequencies)        // header goes out first
              → magic + N + (char,freq)×N

BUILD  ──► HuffmanTree.build(frequencies)            // Optional<HuffmanNode>
              → empty input ⇒ Optional.empty() ⇒ header-only file, return

CODES  ──► CodeTable.from(root)                      // {char → "0110…"}
              → DFS: left appends '0', right appends '1';
                path at each leaf = that char's code

PASS 2 ──► re-read input; for each char:
              bitOut.writeBits(codes.get(char))      // BitOutputStream
           bitOut.finish()                            // flush + zero-pad last byte
```

### Bit packing (`BitOutputStream`)

- `writeBit`: `currentByte = (currentByte << 1) | bit`, increment a counter,
  and flush the byte to disk once 8 bits are buffered. **MSB-first.**
- `writeBits(String)`: writes each `'0'`/`'1'` char of a code as a bit.
- `finish()`: if a partial byte remains, left-shift it to **pad with trailing
  zero bits**, write it, then flush. Crucially it **does not close** the
  underlying stream — that stream is shared with the header writer, and
  `HuffmanEncoder`'s try-with-resources owns the close.

---

## 7. DECODING — step by step (`HuffmanDecoder.decode`)

The exact inverse:

```
READ   ──► Header.read(dataIn)                       // validates magic + entries
              → {char → freq}
           total = Σ freq                             // how many chars to emit
              → total == 0 ⇒ empty file, return

BUILD  ──► HuffmanTree.build(frequencies)            // SAME algorithm, SAME tie-break
              → tree identical to the encoder's        ⇒ see §2 invariant

WALK   ──► writeDecoded(BitInputStream, root, total, writer)
```

### The walk (`writeDecoded`)

```java
node = root;
while (emitted < total) {
    int bit = bitIn.readBit();                 // 0 or 1, or -1 at EOF
    node = (bit == 0) ? node.left() : node.right();   // 0=left, 1=right
    if (node is Leaf) {
        out.write(leaf.symbol());
        emitted++;
        node = root;                           // restart for the next char
    }
}
```

- Start at the root; each bit selects a child (**`0` → left, `1` → right**,
  mirroring how codes were assigned in `CodeTable`).
- Landing on a **leaf** means a complete character has been decoded — emit it
  and reset to the root. Prefix-freeness guarantees a leaf is reached exactly at
  a character boundary, with no ambiguity.

### Why decoding stops cleanly

The loop is bounded by `emitted < total`, **not** by end-of-stream. Once `total`
characters are produced it halts immediately — so the **trailing zero-pad bits**
in the final byte are simply never read. If the stream runs dry *before* `total`
characters are decoded, that means a truncated/corrupt payload and an
`IOException` is thrown.

### Bit unpacking (`BitInputStream`)

The mirror of `BitOutputStream`: when its bit buffer empties it reads the next
byte, then hands out bits MSB-first via `(currentByte >> bitsRemaining) & 1`.
Returns `-1` at end of stream.

---

## 8. Edge cases (handled explicitly — don't regress these)

| Input | What happens |
|-------|--------------|
| **Empty file** | `frequencies` is empty → header with `N=0`, no payload. Decode sees `total == 0` and writes an empty file. Round-trips correctly. |
| **One distinct char** (e.g. `"aaaa"`) | The tree is a single `Leaf`, so the root-to-leaf path is empty and an empty code is unusable. `CodeTable` assigns that char the literal code `"0"`. On decode, `HuffmanDecoder` detects a `Leaf` root and emits the symbol `total` times — there are no branch bits to read. |
| **Non-`HUF1` file passed to decode** | `Header.read` checks magic bytes and throws a clear `IOException`. |
| **Corrupt header** | Negative entry count or non-positive frequency → `IOException`. |
| **Truncated payload** | Bitstream ends before `total` chars decoded → `IOException` naming how many were produced. |

---

## 9. End-to-end round trip (one picture)

```
ORIGINAL TEXT
   │  FrequencyCounter (pass 1)
   ▼
{char → freq} ──Header.write──► [magic][N][char,freq]…        (to file)
   │  HuffmanTree.build  (deterministic, minSymbol tie-break)
   ▼
Huffman tree ──CodeTable──► {char → "0110…"}
   │  pass 2: lookup + BitOutputStream  (MSB-first, zero-padded)
   ▼
COMPRESSED FILE = header + packed bitstream
─────────────────────────── decode ───────────────────────────
header ──Header.read──► {char → freq} ──HuffmanTree.build──► IDENTICAL tree
payload ──BitInputStream──► walk tree (0=left, 1=right), emit on leaf, repeat
stop after Σ freq chars  ──►  ORIGINAL TEXT  (exact, byte-for-byte)
```

On the Project Gutenberg "Les Misérables" sample the compressed file is ~57% of
the original, and decoding reproduces the input exactly.

---

## 10. Where to look when…

| You want to… | Go to |
|--------------|-------|
| Change what counts as a "character" / charset handling | `FrequencyCounter`, `HuffmanEncoder`/`HuffmanDecoder` (charset args) |
| Change the file format | `Header` (and bump the magic from `HUF1`) |
| Understand/modify tie-breaking & tree shape | `HuffmanTree.ORDER`, `HuffmanNode.minSymbol` |
| Change code assignment (e.g. canonical codes) | `CodeTable` |
| Touch bit packing/padding | `BitOutputStream` / `BitInputStream` |
| Add a CLI subcommand | `Main` |
| See round-trip / property tests | `src/test/java/.../HuffmanCodecTest.java` |

---

## 11. A note on testing

`HuffmanCodecTest` exercises full encode→decode round trips (the property that
matters: output equals input). Other tests cover the pieces in isolation
(`FrequencyCounterTest`, `HuffmanTreeTest`, `CodeTableTest`, `HeaderTest`,
`BitStreamTest`). Run everything with:

```bash
mvn test    # JaCoCo coverage report at target/site/jacoco/index.html
```

When changing any of the determinism-sensitive code (§4) or the file format
(§5), the round-trip tests are your safety net — a mismatched tree or format
will surface there immediately.
