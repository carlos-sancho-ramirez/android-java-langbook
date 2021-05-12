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

    public static void set(MutableIntList list, int index, int value) {
        final ImmutableIntList immutable = list.toImmutable();
        final int size = immutable.size();
        list.clear();
        for (int i = 0; i < size; i++) {
            if (i == index) {
                list.append(value);
            }
            else {
                list.append(immutable.valueAt(i));
            }
        }
    }

    private MutableIntListUtils() {
    }
}
