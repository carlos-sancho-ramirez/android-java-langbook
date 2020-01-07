package sword.langbook3.android.db;

import sword.collections.IntSet;

import static org.junit.jupiter.api.Assertions.fail;

final class IntSetTestUtils {

    static void assertEqualSet(IntSet expected, IntSet actual) {
        if (!expected.equalSet(actual)) {
            fail("Sets do not match");
        }
    }
}
