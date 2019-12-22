package sword.bitstream;

import java.io.IOException;

public final class NatDecoder implements IntSupplierWithIOException, IntToIntFunctionWithIOException {
    private final sword.bitstream.InputHuffmanStream _stream;

    public NatDecoder(InputHuffmanStream stream) {
        _stream = stream;
    }

    @Override
    public int apply() throws IOException {
        return _stream.readHuffmanSymbol(NatEncoder.naturalTable);
    }

    @Override
    public int apply(int previous) throws IOException {
        return _stream.readHuffmanSymbol(NatEncoder.naturalTable) + previous + 1;
    }
}
