package com.gitlab.lae.gentext;

import java.nio.CharBuffer;
import java.util.*;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Character.toLowerCase;
import static java.lang.Math.min;
import static java.util.Arrays.binarySearch;
import static java.util.Collections.binarySearch;
import static java.util.Objects.requireNonNull;

/**
 * Text generator based on Programming Pearls - Column 15.
 */
public final class TextGenerator {

    private static final Pattern delimiter = Pattern.compile("\\s+");
    private static final char sep = ' ';

    /**
     * Source input with white spaces removed.
     */
    private final CharSequence source;

    /**
     * Sorted array of positions into {@link #source},
     * each position represents the start of the next word and
     * the end of the previous word.
     */
    private final int[] positions;

    /**
     * A word based suffix list view of {@link #source}.
     * Sorted by the first {@link #order} words.
     */
    private final List<CharBuffer> suffixes;

    /**
     * Given order-k, generate the next word base of the previous k words
     */
    private final int order;

    private TextGenerator(Readable readable, int order) {
        if (order <= 0) {
            throw new IllegalArgumentException("order=" + order);
        }
        List<Integer> positions = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        new Scanner(readable).useDelimiter(delimiter).forEachRemaining(word -> {
            positions.add(builder.length());
            builder.append(word).append(sep);
        });
        this.source = builder.toString();
        this.positions = positions.stream().mapToInt(i -> i).toArray();
        this.order = order;
        this.suffixes = new SuffixList(this.source, this.positions.clone());
        this.suffixes.sort((a, b) -> compare(a, b, order));
    }

    private static int compare(CharBuffer a, CharBuffer b, int count) {
        int k = count;
        int n = min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            char x = a.charAt(i);
            char y = b.charAt(i);
            int result = Character.compare(toLowerCase(x), toLowerCase(y));
            if (result != 0) {
                return result;
            }
            if (k > 0 && x == sep && --k == 0) {
                return 0;
            }
        }
        return a.length() - b.length();
    }

    /**
     * Loads the source text from the given readable,
     * the readable can be closed by the caller at the end of this call.
     *
     * @param order given order-k, generate the next word base of the previous k words
     */
    public static TextGenerator create(Readable readable, int order) {
        return new TextGenerator(readable, order);
    }

    /**
     * Generates some text randomly.
     *
     * @param random randomness provider
     * @param max    maximum number of words to return in the result
     */
    public String generate(IntSupplier random, int max) {
        int pos = positions[Math.abs(random.getAsInt()) % suffixes.size()];
        Optional<CharBuffer> word = words(pos).findFirst();
        if (!word.isPresent()) {
            throw new IllegalStateException();
        }
        return generate(random, max, word.get());
    }

    /**
     * Generates some text based on the given phrase.
     *
     * @param random randomness provider
     * @param max    maximum number of words to return in the result
     * @param phrase the phrase to start the generation with
     */
    public String generate(IntSupplier random, int max, String phrase) {
        CharBuffer buffer = CharBuffer.wrap(delimiter.matcher(phrase).replaceAll(String.valueOf(sep)));
        return generate(random, max, buffer);
    }

    private String generate(IntSupplier random, int max, CharBuffer phrase) {
        StringBuilder builder = new StringBuilder(phrase);
        for (int n = 0; n < max; n++) {

            int i = pickNextPhraseIndex(phrase, random);
            phrase = words(suffixes.get(i).position()).skip(1).limit(order)
                    .reduce(null, (a, b) -> a == null
                            ? b : CharBuffer.wrap(source, a.position(), b.limit()));

            if (phrase == null) {
                break;
            }

            CharBuffer next = words(suffixes.get(i).position())
                    .skip(order).findAny().orElse(null);

            if (next == null) {
                break;
            }

            builder.append(sep).append(next);
        }
        return builder.toString();
    }

    private Stream<CharBuffer> words(int position) {
        int index = binarySearch(positions, position);
        if (index < 0) {
            throw new IllegalStateException();
        }
        return IntStream.range(index, positions.length)
                .mapToObj(i -> {
                    int start = positions[i];
                    int end = (i == positions.length - 1 ? source.length() : positions[i + 1]) - 1;
                    return CharBuffer.wrap(source, start, end);
                });
    }

    private int pickNextPhraseIndex(CharBuffer phrase, IntSupplier random) {
        int index = binarySearch(suffixes, phrase, (a, b) -> compare(a, b, -1));
        if (index < 0) {
            index = -(index + 1); // See binarySearch() Javadoc
        }
        int result = index;
        for (int i = 0;
             index + i < suffixes.size() && isPrefix(suffixes.get(index + i), phrase);
             i++) {
            if (random.getAsInt() % (i + 1) == 0) {
                result = index + i;
            }
        }
        return result;
    }

    private static boolean isPrefix(CharBuffer source, CharBuffer prefix) {
        return source.remaining() >= prefix.remaining() &&
                compare(source.subSequence(0, prefix.length()), prefix, -1) == 0;
    }

    private class SuffixList
            extends AbstractList<CharBuffer>
            implements RandomAccess {

        private final CharSequence content;
        private final int[] positions;

        SuffixList(CharSequence content, int[] positions) {
            this.content = requireNonNull(content);
            this.positions = requireNonNull(positions);
        }

        @Override
        public int size() {
            return positions.length;
        }

        @Override
        public CharBuffer get(int index) {
            int i = positions[index];
            CharBuffer buffer = CharBuffer.wrap(content);
            buffer.position(i);
            return buffer;
        }

        @Override
        public CharBuffer set(int index, CharBuffer buffer) {
            positions[index] = buffer.position();
            return null;
        }
    }
}
