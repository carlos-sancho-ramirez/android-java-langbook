package sword.bitstream;

import java.io.IOException;

import sword.collections.MutableIntPairMap;

public final class MutableIntPairMapSupplier implements SupplierWithIOException<MutableIntPairMap> {

    private final CollectionLengthDecoder _lengthDecoder;
    private final IntSupplierWithIOException _keySupplier;
    private final IntToIntFunctionWithIOException _diffKeySupplier;
    private final IntSupplierWithIOException _valueSupplier;

    MutableIntPairMapSupplier(CollectionLengthDecoder lengthDecoder,
                              IntSupplierWithIOException keySupplier,
                              IntToIntFunctionWithIOException diffKeySupplier,
                              IntSupplierWithIOException valueSupplier) {
        _lengthDecoder = lengthDecoder;
        _keySupplier = keySupplier;
        _diffKeySupplier = diffKeySupplier;
        _valueSupplier = valueSupplier;
    }

    @Override
    public MutableIntPairMap apply() throws IOException {
        final int length = _lengthDecoder.decodeLength();
        final MutableIntPairMap map = MutableIntPairMap.empty();

        int key = 0;
        for (int i = 0; i < length; i++) {
            key = (i == 0 || _diffKeySupplier == null)? _keySupplier.apply() : _diffKeySupplier.apply(key);
            map.put(key, _valueSupplier.apply());
        }

        return map;
    }
}
