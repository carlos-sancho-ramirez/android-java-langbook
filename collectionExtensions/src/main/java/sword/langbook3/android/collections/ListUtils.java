package sword.langbook3.android.collections;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.List;

public final class ListUtils {
    public static <T> List<T> slice(List<T> list, ImmutableIntRange range) {
        final int listSize = list.size();
        final ImmutableList.Builder<T> builder = new ImmutableList.Builder<>();
        for (int index : range) {
            if (index < listSize) {
                builder.append(list.valueAt(index));
            }
        }

        return builder.build();
    }

    private ListUtils() {
    }
}
