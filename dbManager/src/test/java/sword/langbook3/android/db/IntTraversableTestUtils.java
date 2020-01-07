package sword.langbook3.android.db;

import sword.collections.IntTraversable;

import static org.junit.jupiter.api.Assertions.fail;
import static sword.langbook3.android.db.SizableTestUtils.assertSize;

final class IntTraversableTestUtils {

    static int getSingleValue(IntTraversable traversable) {
        assertSize(1, traversable);
        return traversable.valueAt(0);
    }

    static void assertSingleValue(int expectedValue, IntTraversable traversable) {
        final int actualValue = getSingleValue(traversable);
        if (expectedValue != actualValue) {
            fail("Single value in the collection was expected to be " + expectedValue + ", but it was " + actualValue);
        }
    }

    static void assertContains(int expected, IntTraversable traversable) {
        if (!traversable.contains(expected)) {
            fail("Value " + expected + " not contained in the collection");
        }
    }
}
