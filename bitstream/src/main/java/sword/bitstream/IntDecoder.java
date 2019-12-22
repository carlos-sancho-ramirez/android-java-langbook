package sword.bitstream;

import java.io.IOException;

public final class IntDecoder implements IntSupplierWithIOException, IntToIntFunctionWithIOException {
    private final sword.bitstream.InputHuffmanStream _stream;

    public IntDecoder(InputHuffmanStream stream) {
        _stream = stream;
    }

    @Override
    public int apply() throws IOException {
        return _stream.readHuffmanSymbol(IntEncoder.integerTable);
    }

    @Override
    public int apply(int previous) throws IOException {
        return _stream.readHuffmanSymbol(IntEncoder.naturalTable) + previous + 1;
    }
}
