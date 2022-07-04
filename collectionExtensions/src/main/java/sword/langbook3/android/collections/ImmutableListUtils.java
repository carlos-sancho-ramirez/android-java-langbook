package sword.langbook3.android.collections;

import sword.collections.ImmutableList;

public final class ImmutableListUtils {

    public static <T, U extends T> ImmutableList<T> appendAll(ImmutableList<T> list, Iterable<U> that) {
        if (that != null) {
            for (U item : that) {
                list = list.append(item);
            }
        }

        return list;
    }

    private ImmutableListUtils() {
    }
}
