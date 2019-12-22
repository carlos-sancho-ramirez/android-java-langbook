package sword.bitstream;

import sword.bitstream.huffman.HuffmanTable;

import java.io.IOException;

public final class HuffmanSymbolWriter<T> implements ProcedureWithIOException<T> {

    private final HuffmanTable<T> _table;
    private final OutputBitStream _stream;

    public HuffmanSymbolWriter(HuffmanTable<T> table, OutputBitStream stream) {
        _table = table;
        _stream = stream;
    }

    @Override
    public void apply(T symbol) throws IOException {
        int bits = 0;
        int acc = 0;
        for (Iterable<T> level : _table) {
            for (T element : level) {
                if (symbol == null && element == null || symbol != null && symbol.equals(element)) {
                    if (bits > 0) {
                        for (int i = bits - 1; i >= 0; i--) {
                            _stream.writeBoolean((acc & (1 << i)) != 0);
                        }
                    }
                    return;
                }
                acc++;
            }
            acc <<= 1;
            bits++;
        }

        final String symbolString = (symbol != null)? symbol.toString() : "null";
        throw new IllegalArgumentException("Symbol <" + symbolString + "> is not included in the given Huffman table");
    }
}
