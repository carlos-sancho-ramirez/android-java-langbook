package sword.langbook3.android.collections;

import sword.collections.ImmutableList;

public final class StringUtils {

    public static ImmutableList<Character> stringToCharList(String text) {
        final int textLength = text.length();
        final ImmutableList.Builder<Character> builder = new ImmutableList.Builder<>((currentSize, newSize) -> Math.max(textLength, newSize));
        for (int i = 0; i < textLength; i++) {
            builder.append(text.charAt(i));
        }

        return builder.build();
    }

    private StringUtils() {
    }
}
