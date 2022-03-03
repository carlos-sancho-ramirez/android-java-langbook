package sword.langbook3.android.collections;

import sword.collections.EmptyCollectionException;
import sword.collections.Traversable;

public final class TraversableUtils {

    /**
     * Retrieves the first element in the collection.
     *
     * @param traversable Collection to be read.
     * @param <T> Type of the element
     * @return The first element in the collection in the traversing order.
     * @throws EmptyCollectionException if the collection is empty.
     */
    public static <T> T first(Traversable<T> traversable) throws EmptyCollectionException {
        final int size = traversable.size();
        if (size == 0) {
            throw new EmptyCollectionException();
        }

        return traversable.valueAt(0);
    }

    /**
     * Retrieves the last element in the collection.
     *
     * @param traversable Collection to be read.
     * @param <T> Type of the element
     * @return The last element in the collection in the traversing order.
     * @throws EmptyCollectionException if the collection is empty.
     */
    public static <T> T last(Traversable<T> traversable) throws EmptyCollectionException {
        // According to the collection, size and valueAt will be quick operations,
        // but some others (linked lists, for example) will not.
        // This implementation is optimized for indexed collections,
        // but maybe the iterator should be used in case of linked lists,
        // or even a different approach for trees.

        final int size = traversable.size();
        if (size == 0) {
            throw new EmptyCollectionException();
        }

        return traversable.valueAt(size - 1);
    }

    private TraversableUtils() {
    }
}