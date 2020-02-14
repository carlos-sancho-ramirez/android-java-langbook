package sword.bitstream.huffman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import sword.collections.EmptyCollectionException;
import sword.collections.ImmutableSet;
import sword.collections.IntValueMap;
import sword.collections.MutableIntValueHashMap;
import sword.collections.MutableIntValueMap;
import sword.collections.SortFunction;

/**
 * Version of finite Huffman table that can be encoded within the stream.
 * <p>
 * This Huffman table is exhaustive, which means that there is no combination of bits
 * that is not included on it.
 * <p>
 * This Huffman table is finite, which means that there is a maximum amount of bits
 * defined for the encoded symbols. Thus, on iterating, there is always end.
 *
 * @param <E> Type of the symbol to encode or decode.
 */
public final class DefinedHuffmanTable<E> implements HuffmanTable<E> {
    private final int[] _levelIndexes;
    private final Object[] _symbols;
    private transient int _hashCode;

    // TODO: Check that there is not repeated symbols
    private void assertExhaustiveTable() {
        final int levelsLength = _levelIndexes.length;

        if (_levelIndexes.length > 0) {
            int remain = 1;
            for (int i = 1; i < levelsLength + 1; i++) {
                remain <<= 1;

                int thisLength = symbolsWithBits(i);
                remain -= thisLength;
                if (remain <= 0 && i != levelsLength) {
                    throw new IllegalArgumentException("Found symbols in the tree that never will be used");
                }
            }

            if (remain != 0) {
                throw new IllegalArgumentException("Provided tree is not exhaustive");
            }
        }
        else if (_symbols.length > 1) {
            throw new IllegalArgumentException("Impossible to have more than one symbol for 0 bits");
        }
    }

    // TODO: This is public just for testing reasons and should not be this way.
    public DefinedHuffmanTable(int[] levelIndexes, Object[] symbols) {
        _levelIndexes = levelIndexes;
        _symbols = symbols;

        assertExhaustiveTable();
    }

    /**
     * Create a DefinedHuffmanTable resulting of iterating over the given structure of symbols.
     * <p>
     * This is a complex method and should be avoided.
     * Try using {@link #withFrequencies(IntValueMap, SortFunction)} or {@link #from(Iterable, SortFunction)} instead.
     * <p>
     * It is expected here that the given table has its symbols sorted from most probable to less
     * probable in order to ensure an optimal encoding.
     * <p>
     * It is also expected that the symbols within the iterable are not repeated.
     * <p>
     * It is also expected that the given iterable is finite.
     *
     * @param table An {@link java.lang.Iterable} of iterable of symbols.
     *              The main iterable must contain all symbols grouped for the number of
     *              bits that each symbol should use when encoded depending on its
     *              appearing frequency. The order on each of the sub iterable is
     *              irrelevant in terms of optimizations.
     * @param <U> Type for the symbols within the table to generate.
     * @return A new DefinedHuffmanTable instance.
     * @see #withFrequencies(IntValueMap, SortFunction)
     * @see #from(Iterable, SortFunction)
     */
    public static <U> DefinedHuffmanTable<U> fromIterable(Iterable<Iterable<U>> table) {
        ArrayList<U> symbols = new ArrayList<>();
        ArrayList<Integer> indexes = new ArrayList<>();

        int bits = 0;
        int index = 0;
        for (Iterable<U> iterable : table) {
            if (bits != 0) {
                indexes.add(index);
            }

            for (U element : iterable) {
                symbols.add(element);
                index++;
            }

            bits++;
        }

        final int[] indexesArray = new int[indexes.size()];
        for (int i = 0; i < indexesArray.length; i++) {
            indexesArray[i] = indexes.get(i);
        }

        final Object[] symbolsArray = new Object[symbols.size()];
        for (int i = 0; i < symbolsArray.length; i++) {
            symbolsArray[i] = symbols.get(i);
        }

        return new DefinedHuffmanTable<>(indexesArray, symbolsArray);
    }

    private final class HuffmanLevelIterator implements Iterator<E> {
        private final int _last;
        private int _index;

        private HuffmanLevelIterator(int bits) {
            _last = (bits == _levelIndexes.length)? _symbols.length : _levelIndexes[bits];
            _index = (bits == 0)? 0 : _levelIndexes[bits - 1];
        }

        @Override
        public boolean hasNext() {
            return _index < _last;
        }

        @Override
        public E next() {
            if (_index >= _last) {
                throw new UnsupportedOperationException();
            }

            return (E) _symbols[_index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class LevelIterable implements Iterable<E> {

        private final int _bits;

        private LevelIterable(int bits) {
            _bits = bits;
        }

        @Override
        public HuffmanLevelIterator iterator() {
            return new HuffmanLevelIterator(_bits);
        }

        @Override
        public int hashCode() {
            return _bits;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof DefinedHuffmanTable.LevelIterable)) {
                return false;
            }

            final HuffmanLevelIterator thisIt = iterator();
            final HuffmanLevelIterator thatIt = ((LevelIterable) other).iterator();
            while (thisIt.hasNext()) {
                if (!thatIt.hasNext()) {
                    return false;
                }

                final Object thisObj = thisIt.next();
                final Object thatObj = thatIt.next();
                if (thisObj == null && thatObj != null || thisObj != null && !thisObj.equals(thatObj)) {
                    return false;
                }
            }

            return !thatIt.hasNext();
        }
    }

    private class TableIterator implements Iterator<Iterable<E>> {

        private int _bits;

        @Override
        public boolean hasNext() {
            return _bits <= _levelIndexes.length;
        }

        @Override
        public Iterable<E> next() {
            return new LevelIterable(_bits++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<Iterable<E>> iterator() {
        return new TableIterator();
    }

    @Override
    public int hashCode() {
        if (_hashCode == 0) {
            _hashCode = Arrays.hashCode(_symbols);
        }

        return _hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DefinedHuffmanTable)) {
            return false;
        }

        final DefinedHuffmanTable that = (DefinedHuffmanTable) other;
        return Arrays.equals(_levelIndexes, that._levelIndexes) && Arrays.equals(_symbols, that._symbols);
    }

    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder("\n");
        final int levels = _levelIndexes.length + 1;
        for (int bits = 0; bits < levels; bits++) {
            final int levelLength = symbolsWithBits(bits);
            str.append("[");
            for (int j = 0; j < levelLength; j++) {
                str.append("" + getSymbol(bits, j));
                if (j < levelLength - 1) {
                    str.append(", ");
                }
            }
            str.append("]\n");
        }

        return str.toString();
    }

    @Override
    public int symbolsWithBits(int bits) {
        final int levelIndex = (bits == 0)? 0 : _levelIndexes[bits - 1];
        final int nextLevelIndex = (_levelIndexes.length == bits)? _symbols.length : _levelIndexes[bits];
        return nextLevelIndex - levelIndex;
    }

    @Override
    public E getSymbol(int bits, int index) {
        final int offset = (bits == 0)? 0 : _levelIndexes[bits - 1];
        return (E) _symbols[offset + index];
    }

    private abstract static class Node {
        final int frequency;

        Node(int frequency) {
            this.frequency = frequency;
        }

        abstract void fillSymbolLengthMap(int[] lengths, int depth);
    }

    private static class Leaf extends Node {
        final int index;

        Leaf(int index, int frequency) {
            super(frequency);
            this.index = index;
        }

        @Override
        void fillSymbolLengthMap(int[] lengths, int depth) {
            lengths[index] = depth;
        }
    }

    private static class InnerNode extends Node {
        final Node left;
        final Node right;

        InnerNode(Node left, Node right) {
            super(left.frequency + right.frequency);
            this.left = left;
            this.right = right;
        }

        @Override
        void fillSymbolLengthMap(int[] lengths, int depth) {
            left.fillSymbolLengthMap(lengths, depth + 1);
            right.fillSymbolLengthMap(lengths, depth + 1);
        }
    }

    /**
     * Build a {@link DefinedHuffmanTable} based on the given map of frequencies.
     * This method will take for the map the most probable symbols and will assign
     * to them less bits when encoding, leaving the less probable symbols with the
     * biggest amount of bits. This will ensure less amount of data to be written
     * or read from stream, compressing the data.
     *
     * @param frequency Map of frequencies.
     *                  Key of this map are the symbols to be encoded or decoded.
     *                  Values of this map are the number or times this symbol is usually found.
     *                  The bigger the value the more probable the symbol is.
     *                  Values on the map must be all positive numbers. Zero is also not allowed.
     * @param sortFunction Comparator for the symbols. After finding the number
     *                   of bits that each symbol should have, this will be
     *                   used to provide an order of symbols within the symbols
     *                   with the same number of bits. Ordering the symbols
     *                   properly may optimize the way the table will be
     *                   written in a stream.
     * @param <E> Type of the symbol to encode.
     *            It is strongly recommended that this type has a proper {@link Object#hashCode}
     *            method implemented. That will ensure that the table will be always the same
     *            for the same frequency map.
     * @return A DefinedHuffmanTable optimized for the map of frequencies given.
     */
    public static <E> DefinedHuffmanTable<E> withFrequencies(IntValueMap<E> frequency, SortFunction<E> sortFunction) {
        final int length = frequency.size();
        if (length == 0) {
            throw new EmptyCollectionException();
        }
        else if (length == 1) {
            final Object[] keys = new Object[1];
            keys[0] = frequency.keyAt(0);
            return new DefinedHuffmanTable<>(new int[0], keys);
        }

        final ImmutableSet<E> keys = frequency.keySet().sort(sortFunction).toImmutable();
        final Node[] nodes = new Node[length];
        int nodeCount;
        for (nodeCount = 0; nodeCount < length; nodeCount++) {
            final E key = keys.valueAt(nodeCount);
            nodes[nodeCount] = new Leaf(nodeCount, frequency.get(key));
        }

        for (nodeCount = length; nodeCount > 1; nodeCount--) {
            int minIndex1 = nodeCount - 1;
            int minIndex2 = nodeCount - 2;
            int minFreq1 = nodes[minIndex1].frequency;
            int minFreq2 = nodes[minIndex2].frequency;
            if (minFreq2 < minFreq1) {
                int tempIndex = minIndex1;
                minIndex1 = minIndex2;
                minIndex2 = tempIndex;

                int tempFreq = minFreq1;
                minFreq1 = minFreq2;
                minFreq2 = tempFreq;
            }

            for (int index = nodeCount - 3; index >= 0; index--) {
                int freq = nodes[index].frequency;
                if (freq < minFreq1) {
                    minFreq2 = minFreq1;
                    minFreq1 = freq;

                    minIndex2 = minIndex1;
                    minIndex1 = index;
                }
                else if (freq < minFreq2) {
                    minFreq2 = freq;
                    minIndex2 = index;
                }
            }

            if (minIndex1 > minIndex2) {
                int temp = minIndex1;
                minIndex1 = minIndex2;
                minIndex2 = temp;
            }

            final Node newNode = new InnerNode(nodes[minIndex1], nodes[minIndex2]);
            nodes[minIndex1] = newNode;
            for (int index = minIndex2 + 1; index < nodeCount; index++) {
                nodes[index - 1] = nodes[index];
            }
        }
        final int[] bitLengths = new int[length];
        nodes[0].fillSymbolLengthMap(bitLengths, 0);

        int maxLength = 0;
        for (int bitLength : bitLengths) {
            if (bitLength > maxLength) {
                maxLength = bitLength;
            }
        }

        final int[] symbolsPerBitLength = new int[maxLength + 1];
        for (int bitLength : bitLengths) {
            symbolsPerBitLength[bitLength]++;
        }

        final int[] tableIndexes = new int[maxLength];
        int acc = symbolsPerBitLength[0];
        int temp;
        for (int bitLength = 1; bitLength <= maxLength; bitLength++) {
            temp = acc;
            tableIndexes[bitLength - 1] = acc;
            acc += symbolsPerBitLength[bitLength];
            symbolsPerBitLength[bitLength] = temp;
        }

        final Object[] symbols = new Object[length];
        for (int i = 0; i < length; i++) {
            final int bitLength = bitLengths[i];
            symbols[symbolsPerBitLength[bitLength]++] = keys.valueAt(i);
        }

        return new DefinedHuffmanTable<>(tableIndexes, symbols);
    }

    /**
     * Build a {@link DefinedHuffmanTable} using the given symbol array as base.
     * <p>
     * This method builds a map of frequencies counting all the symbols found and
     * call {@link #withFrequencies(IntValueMap, SortFunction)} in order to build the map.
     *
     * @param symbols Array of symbols from where the map of frequencies will be extracted.
     *                Thus, this map should contain a good sample of the kind of data to be
     *                compressed in an optimal way, or the whole data if it can fit in memory.
     * @param sortFunction Comparator for the symbols. After finding the number
     *                   of bits that each symbol should have, this will be
     *                   used to provide an order of symbols within the symbols
     *                   with the same number of bits. Ordering the symbols
     *                   properly may optimize the way the table will be
     *                   written in a stream.
     * @param <E> Type of the symbol to encode
     * @return A new {@link DefinedHuffmanTable} instance.
     */
    public static <E> DefinedHuffmanTable<E> from(Iterable<E> symbols, SortFunction<E> sortFunction) {
        final MutableIntValueMap<E> frequency = MutableIntValueHashMap.empty();
        for (E symbol : symbols) {
            final int mapValue = frequency.get(symbol, 0);
            frequency.put(symbol, mapValue + 1);
        }

        return withFrequencies(frequency, sortFunction);
    }
}
