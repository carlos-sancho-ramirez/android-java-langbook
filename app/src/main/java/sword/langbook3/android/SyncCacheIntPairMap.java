package sword.langbook3.android;

import sword.collections.IntPairMap;
import sword.collections.IntToIntFunction;
import sword.collections.MutableIntPairMap;

public final class SyncCacheIntPairMap {

    private final IntToIntFunction _supplier;
    private final MutableIntPairMap _map;

    public SyncCacheIntPairMap(IntToIntFunction supplier) {
        _supplier = supplier;
        _map = MutableIntPairMap.empty();
    }

    public IntPairMap innerMap() {
        return _map;
    }

    public int get(int key) {
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
