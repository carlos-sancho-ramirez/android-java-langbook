package sword.langbook3.android.collections;

import sword.collections.MutableSet;

public final class MutableSetUtils {

    public static <T> T pickLast(MutableSet<T> set) {
        final int size = set.size();
        final T result = set.valueAt(size - 1);
        set.removeAt(size - 1);
        return result;
    }

    private MutableSetUtils() {
    }
}
