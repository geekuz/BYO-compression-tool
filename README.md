# compression-tool

Build Your Own compression tool — a Huffman-coding based file compressor/decompressor.

## Requirements

- Java 21
- Maven 3.8+

## Project layout

```
pom.xml
src/
├── main/java/dev/otabek/compression/   # application code
├── main/resources/                     # runtime resources
├── test/java/dev/otabek/compression/   # unit tests (JUnit 5)
└── test/resources/                     # test fixtures (e.g. test.txt)
```

## Common commands

```bash
mvn compile          # compile sources
mvn test             # run tests + JaCoCo coverage report (target/site/jacoco)
mvn package          # build runnable fat JAR at target/compression-tool.jar
```

## Usage

```bash
# Compress a text file
java -jar target/compression-tool.jar encode <input> <output>

# Restore a compressed file
java -jar target/compression-tool.jar decode <input> <output>

# Print the character-frequency table (challenge step 1)
java -jar target/compression-tool.jar freq <input>
```

Example round-trip:

```bash
java -jar target/compression-tool.jar encode src/main/resources/test.txt test.huf
java -jar target/compression-tool.jar decode test.huf test.decoded.txt
cmp src/main/resources/test.txt test.decoded.txt   # identical
```

On the Project Gutenberg "Les Misérables" sample, the compressed file is ~57% of
the original size, and decoding reproduces the input byte-for-byte.

## How it works

The tool implements [Huffman coding](https://codingchallenges.fyi/challenges/challenge-huffman/):

1. **Frequency count** — `FrequencyCounter` scans the input and counts each character.
2. **Tree** — `HuffmanTree` greedily merges the two lowest-frequency nodes via a
   priority queue. Ties break on the smallest character in the subtree, so the
   tree is deterministic and the decoder can rebuild it exactly.
3. **Prefix codes** — `CodeTable` walks the tree (left = `0`, right = `1`) to map
   each character to a bit string.
4. **Encode** — `Header` writes the frequency table; `HuffmanEncoder` then streams
   the input again, emitting packed bits via `BitOutputStream`.
5. **Decode** — `HuffmanDecoder` reads the header, rebuilds the tree, and walks the
   bitstream (`BitInputStream`) emitting characters until the known character
   count is reached (trailing pad bits are ignored).

### Compressed file format (`HUF1`)

```
bytes 0..3 : magic 'H' 'U' 'F' '1'
int        : number of distinct characters (N)
N entries  : char (2 bytes) symbol + long (8 bytes) frequency
...        : packed bitstream (8 bits/byte, MSB-first, zero-padded final byte)
```

## Build details

- Java release: 21 (`maven.compiler.release`)
- Testing: JUnit 5 (Jupiter) via maven-surefire-plugin
- Packaging: maven-shade-plugin produces a runnable JAR (main class `dev.otabek.compression.Main`)
- Coverage: jacoco-maven-plugin, report at `target/site/jacoco/index.html`
