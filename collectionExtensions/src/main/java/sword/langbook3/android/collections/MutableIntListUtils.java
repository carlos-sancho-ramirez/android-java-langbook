package sword.langbook3.android.collections;

import sword.collections.ImmutableIntList;
import sword.collections.MutableIntList;

public final class MutableIntListUtils {

    public static void prepend(MutableIntList list, int value) {
        final ImmutableIntList immutable = list.toImmutable();
        list.clear();
        list.append(value);
        for (int v : immutable) {
            list.append(v);
        }
    }

    private MutableIntListUtils() {
    }
}
