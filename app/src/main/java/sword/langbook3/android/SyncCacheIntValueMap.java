package sword.langbook3.android;

import sword.collections.IntResultFunction;
import sword.collections.MutableIntValueMap;

public final class SyncCacheIntValueMap<T> {

    private final IntResultFunction<T> _supplier;
    private final MutableIntValueMap<T> _map;

    public SyncCacheIntValueMap(IntResultFunction<T> supplier) {
        _supplier = supplier;
        _map = MutableIntValueMap.empty();
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
