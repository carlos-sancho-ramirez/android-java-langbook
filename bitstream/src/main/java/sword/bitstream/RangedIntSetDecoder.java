package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.IntHuffmanTable;
import sword.bitstream.huffman.RangedIntHuffmanTable;

public final class RangedIntSetDecoder implements CollectionLengthDecoder, IntSupplierWithIOException, IntToIntFunctionWithIOException {
    private final InputHuffmanStream _stream;
    private final IntHuffmanTable _lengthTable;
    private final int _min;
    private final int _max;
    private int _length;
    private int _lastIndex;

    public RangedIntSetDecoder(InputHuffmanStream stream, IntHuffmanTable lengthTable, int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        _stream = stream;
        _lengthTable = lengthTable;
        _min = min;
        _max = max;
    }

    @Override
    public int apply() throws IOException {
        RangedIntHuffmanTable table = new RangedIntHuffmanTable(_min, _max - _length + 1);
        _lastIndex = 0;
        return _stream.readIntHuffmanSymbol(table);
    }

    @Override
    public int apply(int previous) throws IOException {
        ++_lastIndex;
        RangedIntHuffmanTable table = new RangedIntHuffmanTable(previous + 1, _max - _length + _lastIndex + 1);
        return _stream.readIntHuffmanSymbol(table);
    }

    @Override
    public int decodeLength() throws IOException {
        final int length = _stream.readIntHuffmanSymbol(_lengthTable);

        if (length < 0) {
            throw new IllegalArgumentException("length should not be a negative number");
        }

        if (length > _max - _min + 1) {
            throw new IllegalArgumentException("length should not be bigger than the amount of possible values within the range");
        }

        _length = length;
        return length;
    }
}
