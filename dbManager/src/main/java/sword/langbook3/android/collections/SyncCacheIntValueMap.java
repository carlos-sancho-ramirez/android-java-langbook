package sword.langbook3.android.collections;

import sword.collections.IntResultFunction;
import sword.collections.MutableIntValueHashMap;

public final class SyncCacheIntValueMap<T> {

    private final IntResultFunction<T> _supplier;
    private final MutableIntValueHashMap<T> _map;

    public SyncCacheIntValueMap(IntResultFunction<T> supplier) {
        _supplier = supplier;
        _map = MutableIntValueHashMap.empty();
    }

    public int get(T key) {
        if (_map.keySet().contains(key)) {
            return _map.get(key);
        }
        else {
            final int value = _supplier.apply(key);
            _map.put(key, value);
            return value;
        }
    }
}
