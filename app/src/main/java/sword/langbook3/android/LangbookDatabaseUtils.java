package sword.langbook3.android;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;

public final class LangbookDatabaseUtils {

    private LangbookDatabaseUtils() {
    }

    /**
     * Apply the given conversion to the given text to generate a converted one.
     * @param pairs sorted set of pairs to be traversed in order to convert the <pre>text</pre> string.
     * @param text Text to be converted
     * @return The converted text, or null if text cannot be converted.
     */
    public static String convertText(ImmutableSet<ImmutablePair<String, String>> pairs, String text) {
        String result = "";
        while (text.length() > 0) {
            boolean found = false;
            for (ImmutablePair<String, String> pair : pairs) {
                if (text.startsWith(pair.left)) {
                    result += pair.right;
                    text = text.substring(pair.left.length());
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }

        return result;
    }

    /**
     * Apply the given conversion in the inverse order to find all original
     * strings that can be converted to the given text.
     *
     * @param pairs sorted set of pairs to be traversed in order to get the original text.
     * @param text Converted text to be analyzed
     * @return A set with all source texts that results in the given text once the conversion is applied. This will be empty is none, but never null.
     */
    public static ImmutableSet<String> findSourceTextsForConvertedText(ImmutableSet<ImmutablePair<String, String>> pairs, String text) {
        final ImmutableSet.Builder<String> builder = new ImmutableHashSet.Builder<>();
        if (text == null) {
            return builder.build();
        }

        for (ImmutablePair<String, String> pair : pairs) {
            if (pair.right.equals(text)) {
                builder.add(pair.left);
            }
            else if (text.startsWith(pair.right)) {
                for (String result : findSourceTextsForConvertedText(pairs, text.substring(pair.right.length()))) {
                    builder.add(pair.left + result);
                }
            }
        }

        return builder.build();
    }
}
