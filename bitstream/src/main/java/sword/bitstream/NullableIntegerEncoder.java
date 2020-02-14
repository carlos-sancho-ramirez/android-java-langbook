package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.IntNumberHuffmanTable;
import sword.bitstream.huffman.NatNumberHuffmanTable;
import sword.collections.SortFunction;

/**
 * Encode Integer values into the stream. This implementation allow having null values.
 */
public final class NullableIntegerEncoder implements SortFunction<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private static final int BIT_ALIGNMENT = 8;
    static final IntNumberHuffmanTable integerTable = new IntNumberHuffmanTable(BIT_ALIGNMENT);
    static final NatNumberHuffmanTable naturalTable = new NatNumberHuffmanTable(BIT_ALIGNMENT);

    private final OutputHuffmanStream _stream;

    NullableIntegerEncoder(OutputHuffmanStream stream) {
        _stream = stream;
    }

    @Override
    public boolean lessThan(Integer a, Integer b) {
        return b != null && (a == null || a < b);
    }

    @Override
    public void apply(Integer element) throws IOException {
        if (element == null) {
            _stream.writeBoolean(false);
        }
        else {
            _stream.writeBoolean(true);
            _stream.writeIntHuffmanSymbol(integerTable, element);
        }
    }

    @Override
    public void apply(Integer previous, Integer element) throws IOException {
        if (previous == null) {
            _stream.writeIntHuffmanSymbol(integerTable, element);
        }
        else {
            _stream.writeIntHuffmanSymbol(naturalTable, element - previous - 1);
        }
    }
}
