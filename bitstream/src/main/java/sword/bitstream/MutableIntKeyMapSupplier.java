package sword.bitstream;

import java.io.IOException;

import sword.collections.MutableIntKeyMap;

public final class MutableIntKeyMapSupplier<V> implements SupplierWithIOException<MutableIntKeyMap<V>> {

    private final CollectionLengthDecoder _lengthDecoder;
    private final IntSupplierWithIOException _keySupplier;
    private final IntToIntFunctionWithIOException _diffKeySupplier;
    private final SupplierWithIOException<V> _valueSupplier;

    MutableIntKeyMapSupplier(CollectionLengthDecoder lengthDecoder,
                             IntSupplierWithIOException keySupplier,
                             IntToIntFunctionWithIOException diffKeySupplier,
                             SupplierWithIOException<V> valueSupplier) {
        _lengthDecoder = lengthDecoder;
        _keySupplier = keySupplier;
        _diffKeySupplier = diffKeySupplier;
        _valueSupplier = valueSupplier;
    }

    @Override
    public MutableIntKeyMap<V> apply() throws IOException {
        final int length = _lengthDecoder.decodeLength();
        final MutableIntKeyMap<V> map = MutableIntKeyMap.empty();

        int key = 0;
        for (int i = 0; i < length; i++) {
            key = (i == 0 || _diffKeySupplier == null)? _keySupplier.apply() : _diffKeySupplier.apply(key);
            map.put(key, _valueSupplier.apply());
        }

        return map;
    }
}
