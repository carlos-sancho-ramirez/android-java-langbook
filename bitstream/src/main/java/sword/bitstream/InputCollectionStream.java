package sword.bitstream;

import java.io.IOException;

import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.List;
import sword.collections.Map;
import sword.collections.Set;

public interface InputCollectionStream {

    default <K, V> Map<K, V> readMap(
            CollectionLengthDecoder lengthDecoder,
            SupplierWithIOException<K> keySupplier,
            FunctionWithIOException<K, K> diffKeySupplier,
            SupplierWithIOException<V> valueSupplier) throws IOException {
        return new MutableHashMapSupplier<>(lengthDecoder, keySupplier, diffKeySupplier, valueSupplier).apply();
    }

    default <V> IntKeyMap<V> readIntKeyMap(
            CollectionLengthDecoder lengthDecoder,
            IntSupplierWithIOException keySupplier,
            IntToIntFunctionWithIOException diffKeySupplier,
            SupplierWithIOException<V> valueSupplier) throws IOException {
        return new MutableIntKeyMapSupplier<>(lengthDecoder, keySupplier, diffKeySupplier, valueSupplier).apply();
    }

    default IntPairMap readIntPairMap(
            CollectionLengthDecoder lengthDecoder,
            IntSupplierWithIOException keySupplier,
            IntToIntFunctionWithIOException diffKeySupplier,
            IntSupplierWithIOException valueSupplier) throws IOException {
        return new MutableIntPairMapSupplier(lengthDecoder, keySupplier, diffKeySupplier, valueSupplier).apply();
    }

    default <E> Set<E> readSet(
            CollectionLengthDecoder lengthDecoder,
            SupplierWithIOException<E> supplier,
            FunctionWithIOException<E, E> diffSupplier) throws IOException {
        return readMap(lengthDecoder, supplier, diffSupplier, () -> null).keySet();
    }

    default IntSet readIntSet(
            CollectionLengthDecoder lengthDecoder,
            IntSupplierWithIOException supplier,
            IntToIntFunctionWithIOException diffSupplier) throws IOException {
        return readIntKeyMap(lengthDecoder, supplier, diffSupplier, () -> null).keySet();
    }

    default <E> List<E> readList(
            CollectionLengthDecoder lengthDecoder,
            SupplierWithIOException<E> supplier) throws IOException {
        return new MutableListSupplier<>(lengthDecoder, supplier).apply();
    }
}
