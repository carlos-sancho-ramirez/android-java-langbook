package sword.langbook3.android.collections;

import sword.collections.ImmutableList;

public final class ImmutableListUtils {

    public static <E> ImmutableList<E> skipLast(ImmutableList<E> list, int length) {
        final int size = list.size();
        final ImmutableList.Builder<E> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < size - length; i++) {
            builder.append(list.valueAt(i));
        }

        return builder.build();
    }

    private ImmutableListUtils() {
    }
}
