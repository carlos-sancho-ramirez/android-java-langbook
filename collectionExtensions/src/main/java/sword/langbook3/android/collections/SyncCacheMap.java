package sword.langbook3.android.collections;

import sword.collections.Function;
import sword.collections.MutableHashMap;
import sword.collections.MutableMap;

public final class SyncCacheMap<K, V> {

    private final MutableMap<K, V> _map;
    private final Function<? super K, ? extends V> _supplier;

    public SyncCacheMap(Function<? super K, ? extends V> supplier) {
        _map = MutableHashMap.empty();
        _supplier = supplier;
    }

    public SyncCacheMap(MutableMap<K, V> map, Function<? super K, ? extends V> supplier) {
        _map = map;
        _supplier = supplier;
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
