package dev.otabek.compression;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Command-line entry point for the Huffman compression tool.
 *
 * <pre>{@code
 * compression-tool encode <input> <output>   # compress
 * compression-tool decode <input> <output>   # decompress
 * compression-tool freq   <input>            # print the character frequency table (step 1)
 * }</pre>
 *
 * <p>All text is decoded/encoded as UTF-8.
 */
public final class Main {

    /** Exit code for incorrect command-line usage (BSD sysexits EX_USAGE). */
    static final int EXIT_USAGE = 64;
    /** Exit code when the input file cannot be read (BSD sysexits EX_NOINPUT). */
    static final int EXIT_NO_INPUT = 66;
    /** Exit code for an I/O error while reading or writing (BSD sysexits EX_IOERR). */
    static final int EXIT_IO_ERROR = 74;

    private Main() {
        // Utility entry point; not instantiable.
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Runs the tool and returns a process exit code. Extracted from {@link #main}
     * so it can be exercised in tests without terminating the JVM.
     *
     * @param args command-line arguments
     * @param out  stream for normal output
     * @param err  stream for error messages
     * @return the process exit code
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            return usage(err);
        }

        return switch (args[0]) {
            case "encode" -> runEncode(args, err);
            case "decode" -> runDecode(args, err);
            case "freq" -> runFreq(args, out, err);
            default -> {
                err.println("Unknown command: " + args[0]);
                yield usage(err);
            }
        };
    }

    private static int runEncode(String[] args, PrintStream err) {
        if (args.length != 3) {
            return usage(err);
        }
        Path input = Path.of(args[1]);
        Path output = Path.of(args[2]);
        if (Files.notExists(input)) {
            err.println("Error: input file does not exist: " + args[1]);
            return EXIT_NO_INPUT;
        }
        try {
            HuffmanEncoder.encode(input, output, StandardCharsets.UTF_8);
            return 0;
        } catch (IOException e) {
            err.println("Error compressing '" + args[1] + "': " + e.getMessage());
            return EXIT_IO_ERROR;
        }
    }

    private static int runDecode(String[] args, PrintStream err) {
        if (args.length != 3) {
            return usage(err);
        }
        Path input = Path.of(args[1]);
        Path output = Path.of(args[2]);
        if (Files.notExists(input)) {
            err.println("Error: input file does not exist: " + args[1]);
            return EXIT_NO_INPUT;
        }
        try {
            HuffmanDecoder.decode(input, output, StandardCharsets.UTF_8);
            return 0;
        } catch (IOException e) {
            err.println("Error decompressing '" + args[1] + "': " + e.getMessage());
            return EXIT_IO_ERROR;
        }
    }

    private static int runFreq(String[] args, PrintStream out, PrintStream err) {
        if (args.length != 2) {
            return usage(err);
        }
        Path path = Path.of(args[1]);
        try {
            Map<Character, Long> frequencies =
                    FrequencyCounter.countFromFile(path, StandardCharsets.UTF_8);
            printFrequencies(frequencies, out);
            return 0;
        } catch (IOException e) {
            err.println("Error reading file '" + args[1] + "': " + e.getMessage());
            return EXIT_NO_INPUT;
        }
    }

    private static int usage(PrintStream err) {
        err.println("Usage:");
        err.println("  compression-tool encode <input> <output>   compress a text file");
        err.println("  compression-tool decode <input> <output>   restore a compressed file");
        err.println("  compression-tool freq   <input>            print the character frequencies");
        return EXIT_USAGE;
    }

    private static void printFrequencies(Map<Character, Long> frequencies, PrintStream out) {
        frequencies.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> out.printf("%-12s %d%n",
                        describe(entry.getKey()), entry.getValue()));
        out.println("---");
        out.println("distinct characters: " + frequencies.size());
    }

    /** Renders a character in a readable, unambiguous form for the report. */
    private static String describe(char c) {
        if (c == ' ') {
            return "' ' (space)";
        }
        if (Character.isISOControl(c) || Character.isWhitespace(c)) {
            return String.format("U+%04X", (int) c);
        }
        return "'" + c + "'";
    }
}
