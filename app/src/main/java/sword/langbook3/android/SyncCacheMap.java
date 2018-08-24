package sword.langbook3.android;

import sword.collections.Function;
import sword.collections.Map;
import sword.collections.MutableMap;

public final class SyncCacheMap<K, V> {

    private final Function<K, V> _supplier;
    private final MutableMap<K, V> _map;

    public SyncCacheMap(Function<K, V> supplier) {
        _supplier = supplier;
        _map = MutableMap.empty();
    }

    public Map<K, V> innerMap() {
        return _map;
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
}
