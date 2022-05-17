package sword.langbook3.android.collections;

import sword.collections.ImmutableList;
import sword.collections.MutableList;
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

    private TransformableUtils() {
    }
}
