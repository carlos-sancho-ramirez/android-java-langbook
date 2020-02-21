package sword.langbook3.android.sdb;

import sword.collections.IntKeyMap;

import static org.junit.jupiter.api.Assertions.fail;
import static sword.langbook3.android.collections.EqualUtils.equal;
import static sword.langbook3.android.sdb.SizableTestUtils.assertSize;

final class IntKeyMapTestUtils {

    static <V> void assertSinglePair(int expectedKey, V expectedValue, IntKeyMap<V> map) {
        assertSize(1, map);
        final int actualKey = map.keyAt(0);
        if (expectedKey != actualKey) {
            fail("Single key in map was expected to be " + expectedKey + ", but it was " + actualKey);
        }

        final V actualValue = map.valueAt(0);
        if (!equal(expectedValue, actualValue)) {
            fail("Single value in map was expected to be " + expectedValue + ", but it was " + actualValue);
        }
    }

    static <V> void assertEqualMap(IntKeyMap<V> expected, IntKeyMap<V> actual) {
        if (!expected.equalMap(actual)) {
            fail("Maps do not match");
        }
    }

    private IntKeyMapTestUtils() {
    }
}
