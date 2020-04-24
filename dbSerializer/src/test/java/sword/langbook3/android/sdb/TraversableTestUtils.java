package sword.langbook3.android.sdb;

import sword.collections.Traversable;

import static org.junit.jupiter.api.Assertions.fail;
import static sword.langbook3.android.collections.EqualUtils.equal;
import static sword.langbook3.android.sdb.SizableTestUtils.assertSize;

final class TraversableTestUtils {

    static <E> E getSingleValue(Traversable<E> traversable) {
        assertSize(1, traversable);
        return traversable.valueAt(0);
    }

    static <E> void assertContainsOnly(E expectedValue, Traversable<E> actual) {
        final E actualValue = getSingleValue(actual);
        if (!equal(expectedValue, actualValue)) {
            fail("Single value in the collection was expected to be " + expectedValue + ", but it was " + actualValue);
        }
    }

    private TraversableTestUtils() {
    }
}
