package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.IntegerNumberHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;

public final class IntEncoder implements IntProcedureWithIOException, IntProcedure2WithIOException {
    private static final int BIT_ALIGNMENT = 8;
    static final IntegerNumberHuffmanTable integerTable = new IntegerNumberHuffmanTable(BIT_ALIGNMENT);
    static final NaturalNumberHuffmanTable naturalTable = new NaturalNumberHuffmanTable(BIT_ALIGNMENT);

    private final sword.bitstream.OutputHuffmanStream _stream;

    public IntEncoder(OutputHuffmanStream stream) {
        _stream = stream;
    }

    @Override
    public void apply(int element) throws IOException {
        _stream.writeHuffmanSymbol(integerTable, element);
    }

    @Override
    public void apply(int previous, int element) throws IOException {
        _stream.writeHuffmanSymbol(naturalTable, element - previous - 1);
    }
}
