package sword.bitstream;

import java.io.IOException;

import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.List;
import sword.collections.Map;
import sword.collections.SortFunction;

public interface OutputCollectionStream {

    default <K, V> void writeMap(
            CollectionLengthEncoder lengthEncoder,
            ProcedureWithIOException<K> keyWriter,
            Procedure2WithIOException<K> diffKeyWriter,
            SortFunction<K> keySortFunction,
            ProcedureWithIOException<V> valueWriter,
            Map<K, V> map) throws IOException {
        new MapWriter<>(lengthEncoder, keyWriter, diffKeyWriter, keySortFunction, valueWriter).apply(map);
    }

    default <V> void writeIntKeyMap(
            CollectionLengthEncoder lengthEncoder,
            IntProcedureWithIOException keyWriter,
            IntProcedure2WithIOException diffKeyWriter,
            ProcedureWithIOException<V> valueWriter,
            IntKeyMap<V> map) throws IOException {
        new IntKeyMapWriter<>(lengthEncoder, keyWriter, diffKeyWriter, valueWriter).apply(map);
    }

    default void writeIntPairMap(
            CollectionLengthEncoder lengthEncoder,
            IntProcedureWithIOException keyWriter,
            IntProcedure2WithIOException diffKeyWriter,
            IntProcedureWithIOException valueWriter,
            IntPairMap map) throws IOException {
        new IntPairMapWriter(lengthEncoder, keyWriter, diffKeyWriter, valueWriter).apply(map);
    }

    default <E> void writeSet(
            CollectionLengthEncoder lengthEncoder,
            ProcedureWithIOException<E> writer,
            Procedure2WithIOException<E> diffWriter,
            SortFunction<E> sortFunction,
            sword.collections.Set<E> set) throws IOException {

        final Object dummy = new Object();
        final sword.collections.Map<E, Object> map = set.assign(key -> dummy);

        final ProcedureWithIOException<Object> nullWriter = element -> { };
        writeMap(lengthEncoder, writer, diffWriter, sortFunction, nullWriter, map);
    }

    default <E> void writeIntSet(
            CollectionLengthEncoder lengthEncoder,
            IntProcedureWithIOException writer,
            IntProcedure2WithIOException diffWriter,
            IntSet set) throws IOException {

        final Object dummy = new Object();
        final IntKeyMap<Object> map = set.assign(key -> dummy);
        writeIntKeyMap(lengthEncoder, writer, diffWriter, element -> { }, map);
    }

    default <E> void writeList(
            CollectionLengthEncoder lengthEncoder,
            ProcedureWithIOException<E> writer,
            List<E> list) throws IOException {
        new TraversableWriter<>(lengthEncoder, writer).apply(list);
    }
}
