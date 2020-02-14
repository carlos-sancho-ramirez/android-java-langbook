package sword.bitstream;

import java.io.IOException;

import sword.collections.MutableList;

public final class MutableListSupplier<T> implements SupplierWithIOException<MutableList<T>> {

    private final CollectionLengthDecoder _lengthDecoder;
    private final SupplierWithIOException<T> _symbolSupplier;

    MutableListSupplier(CollectionLengthDecoder lengthDecoder, SupplierWithIOException<T> symbolSupplier) {
        _lengthDecoder = lengthDecoder;
        _symbolSupplier = symbolSupplier;
    }

    @Override
    public MutableList<T> apply() throws IOException {
        final int length = _lengthDecoder.decodeLength();
        final MutableList<T> result = MutableList.empty();
        for (int i = 0; i < length; i++) {
            result.append(_symbolSupplier.apply());
        }

        return result;
    }
}
