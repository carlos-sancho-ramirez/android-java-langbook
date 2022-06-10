package sword.langbook3.android.collections;

import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.collections.MutableHashSet;
import sword.collections.MutableList;
import sword.collections.MutableSet;
import sword.collections.Traversable;
import sword.collections.Traverser;

public final class TransformableUtils {

    public static <T> ImmutableList<T> takeToImmutableList(Traversable<T> traversable, int amount) {
        final Traverser<T> traverser = traversable.iterator();
        final MutableList<T> list = MutableList.empty((currentSize, newSize) -> Math.min(traversable.size(), newSize));
        for (int i = 0; i < amount && traverser.hasNext(); i++) {
            list.append(traverser.next());
        }

        return list.toImmutable();
    }

    public static <T> ImmutableSet<T> skip(Traversable<T> traversable, int amount) {
        final Traverser<T> traverser = traversable.iterator();
        final MutableSet<T> set = MutableHashSet.empty();
        for (int i = 0; i < amount && traverser.hasNext(); i++) {
            traverser.next();
        }

        while (traverser.hasNext()) {
            set.add(traverser.next());
        }

        return set.toImmutable();
    }

    private TransformableUtils() {
    }
}
