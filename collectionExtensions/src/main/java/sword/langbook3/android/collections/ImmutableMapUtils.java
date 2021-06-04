package sword.langbook3.android.collections;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableMap;
import sword.collections.Predicate;

public final class ImmutableMapUtils {

    public static <K, V> ImmutableMap<K, V> filterByKey(ImmutableMap<K, V> map, Predicate<K> predicate) {
        final ImmutableHashMap.Builder<K, V> builder = new ImmutableHashMap.Builder<>();
        for (K key : map.keySet()) {
            if (predicate.apply(key)) {
                builder.put(key, map.get(key));
            }
        }

        return builder.build();
    }

    public static <K, V> ImmutableMap<K, V> filterByKeyNot(ImmutableMap<K, V> map, Predicate<K> predicate) {
        final ImmutableHashMap.Builder<K, V> builder = new ImmutableHashMap.Builder<>();
        for (K key : map.keySet()) {
            if (!predicate.apply(key)) {
                builder.put(key, map.get(key));
            }
        }

        return builder.build();
    }

    private ImmutableMapUtils() {
    }
}
