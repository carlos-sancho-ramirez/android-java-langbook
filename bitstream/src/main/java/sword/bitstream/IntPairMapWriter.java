package sword.bitstream;

import java.io.IOException;

import sword.collections.IntPairMap;

public final class IntPairMapWriter implements ProcedureWithIOException<IntPairMap> {

    private final CollectionLengthEncoder _lengthEncoder;
    private final IntProcedureWithIOException _keyWriter;
    private final IntProcedure2WithIOException _diffKeyWriter;
    private final IntProcedureWithIOException _valueWriter;

    IntPairMapWriter(CollectionLengthEncoder lengthEncoder,
                     IntProcedureWithIOException keyWriter,
                     IntProcedure2WithIOException diffKeyWriter,
                     IntProcedureWithIOException valueWriter) {
        _lengthEncoder = lengthEncoder;
        _keyWriter = keyWriter;
        _diffKeyWriter = diffKeyWriter;
        _valueWriter = valueWriter;
    }

    @Override
    public void apply(IntPairMap map) throws IOException {
        final int mapSize = map.size();
        _lengthEncoder.encodeLength(mapSize);

        boolean first = true;
        int previous = 0;
        for (int key : map.keySet()) {
            if (_diffKeyWriter == null || first) {
                _keyWriter.apply(key);
                first = false;
            }
            else {
                _diffKeyWriter.apply(previous, key);
            }
            previous = key;

            _valueWriter.apply(map.get(key));
        }
    }
}
