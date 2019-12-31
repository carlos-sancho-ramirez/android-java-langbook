package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.IntHuffmanTable;
import sword.bitstream.huffman.RangedIntHuffmanTable;
import sword.collections.ImmutableIntRange;

public final class RangedIntSetEncoder implements CollectionLengthEncoder, IntProcedureWithIOException, IntProcedure2WithIOException {

    private final OutputHuffmanStream _stream;
    private final IntHuffmanTable _lengthTable;
    private final ImmutableIntRange _range;
    private int _length;
    private int _lastIndex;

    public RangedIntSetEncoder(OutputHuffmanStream stream, IntHuffmanTable lengthTable, ImmutableIntRange range) {
        if (range == null) {
            throw new IllegalArgumentException("range should not be null");
        }

        _stream = stream;
        _lengthTable = lengthTable;
        _range = range;
    }

    @Override
    public void apply(int element) throws IOException {
        final ImmutableIntRange range = new ImmutableIntRange(_range.min(), _range.max() - _length + 1);
        _stream.writeIntHuffmanSymbol(new RangedIntHuffmanTable(range), element);
        _lastIndex = 0;
    }

    @Override
    public void apply(int previous, int element) throws IOException {
        ++_lastIndex;
        final ImmutableIntRange range = new ImmutableIntRange(previous + 1, _range.max() - _length + _lastIndex + 1);
        _stream.writeIntHuffmanSymbol(new RangedIntHuffmanTable(range), element);
    }

    @Override
    public void encodeLength(int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("length should not be a negative number");
        }

        if (length > _range.size()) {
            throw new IllegalArgumentException("length should not be bigger than the amount of possible values within the range");
        }

        _length = length;
        _stream.writeIntHuffmanSymbol(_lengthTable, length);
    }
}
