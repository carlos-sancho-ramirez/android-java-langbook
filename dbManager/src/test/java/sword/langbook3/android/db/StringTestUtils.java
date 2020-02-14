package sword.langbook3.android.db;

import sword.collections.ImmutableIntRange;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StringTestUtils {

    static ImmutableIntRange rangeOf(String text, String segment) {
        final int start = text.indexOf(segment);
        assertTrue(start >= 0, "'" + segment + "' not found in text '" + text + '\'');

        final int end = start + segment.length();
        return new ImmutableIntRange(start, end - 1);
    }

    private StringTestUtils() {
    }
}
