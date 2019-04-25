package sword.langbook3.android.collections;

import sword.collections.Function;
import sword.collections.MutableHashMap;

public final class SyncCacheMap<K, V> {

    private final Function<K, V> _supplier;
    private final MutableHashMap<K, V> _map;

    public SyncCacheMap(Function<K, V> supplier) {
        _supplier = supplier;
        _map = MutableHashMap.empty();
    }

    public V get(K key) {
        if (_map.keySet().contains(key)) {
            return _map.get(key);
        }
        else {
            final V value = _supplier.apply(key);
            _map.put(key, value);
            return value;
        }
    }

    public void clear() {
        _map.clear();
    }
}
