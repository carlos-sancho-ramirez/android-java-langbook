package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.IntHuffmanTable;
import sword.collections.IntTraversable;

public final class IntHuffmanSymbolWriter implements IntProcedureWithIOException {
    private final IntHuffmanTable _table;
    private final OutputBitStream _stream;

    public IntHuffmanSymbolWriter(IntHuffmanTable table, OutputBitStream stream) {
        _table = table;
        _stream = stream;
    }

    @Override
    public void apply(int symbol) throws IOException {
        int bits = 0;
        int acc = 0;
        for (IntTraversable level : _table) {
            for (int element : level) {
                if (symbol == element) {
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

        throw new IllegalArgumentException("Symbol <" + symbol + "> is not included in the given Huffman table");
    }
}
