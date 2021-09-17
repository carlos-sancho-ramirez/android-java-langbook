package sword.langbook3.android.collections;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableMap;
import sword.collections.Map;
import sword.collections.Predicate;
import sword.collections.TransformerWithKey;

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

    public static <K, V> ImmutableMap<K, V> putAll(ImmutableMap<K, V> map, Map<K, V> other) {
        final TransformerWithKey<K, V> transformer = other.iterator();
        while (transformer.hasNext()) {
            final V value = transformer.next();
            map = map.put(transformer.key(), value);
        }

        return map;
    }

    private ImmutableMapUtils() {
    }
}
