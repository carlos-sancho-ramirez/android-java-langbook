package sword.bitstream;

import java.io.IOException;

import sword.collections.Map;
import sword.collections.SortFunction;

public final class MapWriter<K, V> implements ProcedureWithIOException<Map<K, V>> {

    private final CollectionLengthEncoder _lengthEncoder;
    private final ProcedureWithIOException<K> _keyWriter;
    private final Procedure2WithIOException<K> _diffKeyWriter;
    private final SortFunction<K> _keySortFunction;
    private final ProcedureWithIOException<V> _valueWriter;

    MapWriter(CollectionLengthEncoder lengthEncoder,
                     ProcedureWithIOException<K> keyWriter,
                     Procedure2WithIOException<K> diffKeyWriter,
                     SortFunction<K> keySortFunction,
                     ProcedureWithIOException<V> valueWriter) {
        _lengthEncoder = lengthEncoder;
        _keyWriter = keyWriter;
        _diffKeyWriter = diffKeyWriter;
        _keySortFunction = keySortFunction;
        _valueWriter = valueWriter;
    }

    @Override
    public void apply(Map<K, V> map) throws IOException {
        final int mapSize = map.size();
        _lengthEncoder.encodeLength(mapSize);

        boolean first = true;
        K previous = null;
        for (K key : map.keySet().sort(_keySortFunction)) {
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
