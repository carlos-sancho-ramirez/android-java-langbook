package sword.langbook3.android.db;

import sword.collections.SortFunction;

public final class AcceptationIdComparator implements SortFunction<AcceptationId> {

    public static boolean compare(AcceptationId a, AcceptationId b) {
        return b != null && (a == null || a.key < b. key);
    }

    @Override
    public boolean lessThan(AcceptationId a, AcceptationId b) {
        return compare(a, b);
    }
}
