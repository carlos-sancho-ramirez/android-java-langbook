package sword.langbook3.android;

import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;

public final class LangbookDatabaseUtils {

    private LangbookDatabaseUtils() {
    }

    /**
     * Apply the given conversion to the given text to generate a converted one.
     * @param pairs sorted set of pairs to be traversed in order to convert the <pre>text</pre> string.
     * @param text Text to be converted
     * @return The converted text, or null if text cannot be converted.
     */
    public static String convertText(ImmutableList<ImmutablePair<String, String>> pairs, String text) {
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
}
