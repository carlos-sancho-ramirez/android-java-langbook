package sword.langbook3.android.db;

import sword.collections.Traversable;

import static sword.langbook3.android.db.SizableTestUtils.assertSize;

final class TraversableTestUtils {

    static <E> E getSingleValue(Traversable<E> traversable) {
        assertSize(1, traversable);
        return traversable.valueAt(0);
    }
}
