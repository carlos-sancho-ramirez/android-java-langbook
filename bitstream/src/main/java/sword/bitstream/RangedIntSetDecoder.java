package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.IntHuffmanTable;
import sword.bitstream.huffman.RangedIntHuffmanTable;
import sword.collections.ImmutableIntRange;

public final class RangedIntSetDecoder implements CollectionLengthDecoder, IntSupplierWithIOException, IntToIntFunctionWithIOException {
    private final InputHuffmanStream _stream;
    private final IntHuffmanTable _lengthTable;
    private final ImmutableIntRange _range;
    private int _length;
    private int _lastIndex;

    public RangedIntSetDecoder(InputHuffmanStream stream, IntHuffmanTable lengthTable, ImmutableIntRange range) {
        if (range == null) {
            throw new IllegalArgumentException("range should not be null");
        }

        _stream = stream;
        _lengthTable = lengthTable;
        _range = range;
    }

    @Override
    public int apply() throws IOException {
        _lastIndex = 0;
        final ImmutableIntRange range = new ImmutableIntRange(_range.min(), _range.max() - _length + 1);
        return _stream.readIntHuffmanSymbol(new RangedIntHuffmanTable(range));
    }

    @Override
    public int apply(int previous) throws IOException {
        ++_lastIndex;
        final ImmutableIntRange range = new ImmutableIntRange(previous + 1, _range.max() - _length + _lastIndex + 1);
        return _stream.readIntHuffmanSymbol(new RangedIntHuffmanTable(range));
    }

    @Override
    public int decodeLength() throws IOException {
        final int length = _stream.readIntHuffmanSymbol(_lengthTable);

        if (length < 0) {
            throw new IllegalArgumentException("length should not be a negative number");
        }

        if (length > _range.size()) {
            throw new IllegalArgumentException("length should not be bigger than the amount of possible values within the range");
        }

        _length = length;
        return length;
    }
}
