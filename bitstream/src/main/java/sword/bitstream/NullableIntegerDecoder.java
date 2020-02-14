package sword.bitstream;

import java.io.IOException;

import static sword.bitstream.NullableIntegerEncoder.integerTable;
import static sword.bitstream.NullableIntegerEncoder.naturalTable;

/**
 * Decode Integer values from the stream. This implementation allow having null values.
 */
public final class NullableIntegerDecoder implements SupplierWithIOException<Integer>, FunctionWithIOException<Integer, Integer> {

    private final InputHuffmanStream _stream;

    NullableIntegerDecoder(InputHuffmanStream stream) {
        _stream = stream;
    }

    @Override
    public Integer apply() throws IOException {
        final boolean isValid = _stream.readBoolean();
        return isValid? _stream.readIntHuffmanSymbol(integerTable) : null;
    }

    @Override
    public Integer apply(Integer previous) throws IOException {
        return (previous == null)? _stream.readIntHuffmanSymbol(integerTable) :
                _stream.readIntHuffmanSymbol(naturalTable) + previous + 1;
    }
}
