package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.NaturalNumberHuffmanTable;

public final class NatEncoder implements IntProcedureWithIOException, IntProcedure2WithIOException {

    private static final int BIT_ALIGNMENT = 8;
    static final NaturalNumberHuffmanTable naturalTable = new NaturalNumberHuffmanTable(BIT_ALIGNMENT);
    private final OutputHuffmanStream _stream;

    public NatEncoder(OutputHuffmanStream stream) {
        _stream = stream;
    }

    @Override
    public void apply(int element) throws IOException {
        _stream.writeHuffmanSymbol(naturalTable, element);
    }

    @Override
    public void apply(int previous, int element) throws IOException {
        _stream.writeHuffmanSymbol(naturalTable, element - previous - 1);
    }
}
