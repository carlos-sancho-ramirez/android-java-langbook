package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.IntHuffmanTable;

public final class IntHuffmanSymbolSupplier implements IntSupplierWithIOException {
    private final IntHuffmanTable _table;
    private final InputBitStream _stream;

    IntHuffmanSymbolSupplier(IntHuffmanTable table, InputBitStream stream) {
        _table = table;
        _stream = stream;
    }

    @Override
    public int apply() throws IOException {
        if (_table == null) {
            throw new IllegalArgumentException();
        }

        if (_table.symbolsWithBits(0) > 0) {
            return _table.getSymbol(0, 0);
        }

        int value = 0;
        int base = 0;
        int bits = 1;

        while (true) {
            value = (value << 1) + (_stream.readBoolean() ? 1 : 0);
            base <<= 1;
            final int levelLength = _table.symbolsWithBits(bits);
            final int levelIndex = value - base;
            if (levelIndex < levelLength) {
                return _table.getSymbol(bits, levelIndex);
            }

            base += levelLength;
            bits++;
        }
    }
}
