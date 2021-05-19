package sword.langbook3.android.collections;

import sword.collections.IntPairMap;
import sword.collections.IntPairMapGetter;
import sword.collections.IntToIntFunction;
import sword.collections.MutableIntPairMap;

public final class SyncCacheIntPairMap implements IntPairMapGetter {

    private final IntToIntFunction _supplier;
    private final MutableIntPairMap _map;

    public SyncCacheIntPairMap(IntToIntFunction supplier) {
        _supplier = supplier;
        _map = MutableIntPairMap.empty();
    }

    public IntPairMap innerMap() {
        return _map;
    }

    @Override
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
