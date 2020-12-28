package sword.langbook3.android.db;

import sword.collections.SortFunction;

public final class AlphabetIdComparator implements SortFunction<AlphabetId> {
    @Override
    public boolean lessThan(AlphabetId a, AlphabetId b) {
        return b != null && (a == null || a.key < b. key);
    }
}
