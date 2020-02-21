package sword.langbook3.android.sdb;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.IntSet;

import static org.junit.jupiter.api.Assertions.fail;

final class IntSetTestUtils {

    static void assertEqualSet(IntSet expected, IntSet actual) {
        if (!expected.equalSet(actual)) {
            fail("Sets do not match");
        }
    }

    static ImmutableIntSet intSetOf(int... values) {
        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        for (int value : values) {
            builder.add(value);
        }
        return builder.build();
    }

    private IntSetTestUtils() {
    }
}
