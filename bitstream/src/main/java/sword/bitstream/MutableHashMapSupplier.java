package sword.bitstream;

import java.io.IOException;
import sword.collections.MutableHashMap;

public final class MutableHashMapSupplier<K, V> implements SupplierWithIOException<MutableHashMap<K, V>> {

    private final CollectionLengthDecoder _lengthDecoder;
    private final SupplierWithIOException<K> _keySupplier;
    private final FunctionWithIOException<K, K> _diffKeySupplier;
    private final SupplierWithIOException<V> _valueSupplier;

    MutableHashMapSupplier(CollectionLengthDecoder lengthDecoder,
                           SupplierWithIOException<K> keySupplier,
                           FunctionWithIOException<K, K> diffKeySupplier,
                           SupplierWithIOException<V> valueSupplier) {
        _lengthDecoder = lengthDecoder;
        _keySupplier = keySupplier;
        _diffKeySupplier = diffKeySupplier;
        _valueSupplier = valueSupplier;
    }

    @Override
    public MutableHashMap<K, V> apply() throws IOException {
        final int length = _lengthDecoder.decodeLength();
        final MutableHashMap<K, V> map = MutableHashMap.empty();

        K key = null;
        for (int i = 0; i < length; i++) {
            key = (i == 0 || _diffKeySupplier == null)? _keySupplier.apply() : _diffKeySupplier.apply(key);
            map.put(key, _valueSupplier.apply());
        }

        return map;
    }
}
