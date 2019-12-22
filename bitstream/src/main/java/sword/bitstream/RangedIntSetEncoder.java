package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.IntHuffmanTable;
import sword.bitstream.huffman.RangedIntHuffmanTable;

public final class RangedIntSetEncoder implements CollectionLengthEncoder, IntProcedureWithIOException, IntProcedure2WithIOException {

    private final OutputHuffmanStream _stream;
    private final IntHuffmanTable _lengthTable;
    private final int _min;
    private final int _max;
    private int _length;
    private int _lastIndex;

    public RangedIntSetEncoder(OutputHuffmanStream stream, IntHuffmanTable lengthTable, int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        _stream = stream;
        _lengthTable = lengthTable;
        _min = min;
        _max = max;
    }

    @Override
    public void apply(int element) throws IOException {
        RangedIntHuffmanTable table = new RangedIntHuffmanTable(_min, _max - _length + 1);
        _stream.writeIntHuffmanSymbol(table, element);
        _lastIndex = 0;
    }

    @Override
    public void apply(int previous, int element) throws IOException {
        ++_lastIndex;
        RangedIntHuffmanTable table = new RangedIntHuffmanTable(previous + 1, _max - _length + _lastIndex + 1);
        _stream.writeIntHuffmanSymbol(table, element);
    }

    @Override
    public void encodeLength(int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("length should not be a negative number");
        }

        if (length > _max - _min + 1) {
            throw new IllegalArgumentException("length should not be bigger than the amount of possible values within the range");
        }

        _length = length;
        _stream.writeIntHuffmanSymbol(_lengthTable, length);
    }
}
