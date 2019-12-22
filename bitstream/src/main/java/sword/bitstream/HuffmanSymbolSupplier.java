package sword.bitstream;

import sword.bitstream.huffman.HuffmanTable;

import java.io.IOException;

public final class HuffmanSymbolSupplier<T> implements SupplierWithIOException<T> {

    private final HuffmanTable<T> _table;
    private final InputBitStream _stream;

    public HuffmanSymbolSupplier(HuffmanTable<T> table, InputBitStream stream) {
        _table = table;
        _stream = stream;
    }

    @Override
    public T apply() throws IOException {
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
