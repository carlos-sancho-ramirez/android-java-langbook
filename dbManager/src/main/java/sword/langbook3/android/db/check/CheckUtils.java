package sword.langbook3.android.db.check;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableSet;

final class CheckUtils {
    static <T> ImmutableSet<T> setOf() {
        return ImmutableHashSet.empty();
    }

    static <T> ImmutableSet<T> setOf(T a) {
        return new ImmutableHashSet.Builder<T>().add(a).build();
    }

    static <T> ImmutableSet<T> setOf(T a, T b) {
        return new ImmutableHashSet.Builder<T>().add(a).add(b).build();
    }

    static <T> ImmutableSet<T> setOf(T a, T b, T c) {
        return new ImmutableHashSet.Builder<T>().add(a).add(b).add(c).build();
    }

    private CheckUtils() {
    }
}
