package sword.bitstream.huffman;

import java.util.Arrays;

import sword.collections.EmptyCollectionException;
import sword.collections.ImmutableIntSet;
import sword.collections.IntPairMap;
import sword.collections.IntTraversable;
import sword.collections.IntTraverser;
import sword.collections.MutableIntList;
import sword.collections.MutableIntPairMap;
import sword.collections.Traversable;
import sword.collections.Traverser;

public final class DefinedIntHuffmanTable implements IntHuffmanTable {
    private final int[] _levelIndexes;
    private final int[] _symbols;
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
    public DefinedIntHuffmanTable(int[] levelIndexes, int[] symbols) {
        _levelIndexes = levelIndexes;
        _symbols = symbols;

        assertExhaustiveTable();
    }

    public static DefinedIntHuffmanTable fromTraversable(Traversable<? extends IntTraversable> table) {
        final MutableIntList symbols = MutableIntList.empty();
        final MutableIntList indexes = MutableIntList.empty();

        int bits = 0;
        int index = 0;
        for (Iterable<Integer> iterable : table) {
            if (bits != 0) {
                indexes.append(index);
            }

            for (int element : iterable) {
                symbols.append(element);
                index++;
            }

            bits++;
        }

        final int[] indexesArray = new int[indexes.size()];
        for (int i = 0; i < indexesArray.length; i++) {
            indexesArray[i] = indexes.get(i);
        }

        final int[] symbolsArray = new int[symbols.size()];
        for (int i = 0; i < symbolsArray.length; i++) {
            symbolsArray[i] = symbols.get(i);
        }

        return new DefinedIntHuffmanTable(indexesArray, symbolsArray);
    }

    private final class HuffmanLevelIterator implements IntTraverser {
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
        public Integer next() {
            if (_index >= _last) {
                throw new UnsupportedOperationException();
            }

            return _symbols[_index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class LevelIterable implements IntTraversable {

        private final int _bits;

        private LevelIterable(int bits) {
            _bits = bits;
            //_level = _table[tableIndex];
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
            if (!(other instanceof LevelIterable)) {
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

    private class TableIterator implements Traverser<IntTraversable> {

        private int _bits;

        @Override
        public boolean hasNext() {
            return _bits <= _levelIndexes.length;
        }

        @Override
        public IntTraversable next() {
            return new LevelIterable(_bits++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Traverser<IntTraversable> iterator() {
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
        if (!(other instanceof DefinedIntHuffmanTable)) {
            return false;
        }

        final DefinedIntHuffmanTable that = (DefinedIntHuffmanTable) other;
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
    public int getSymbol(int bits, int index) {
        final int offset = (bits == 0)? 0 : _levelIndexes[bits - 1];
        return _symbols[offset + index];
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

    public static DefinedIntHuffmanTable withFrequencies(IntPairMap frequency) {
        final int length = frequency.size();
        if (length == 0) {
            throw new EmptyCollectionException();
        }
        else if (length == 1) {
            final int[] keys = new int[1];
            keys[0] = frequency.keyAt(0);
            return new DefinedIntHuffmanTable(new int[0], keys);
        }

        final ImmutableIntSet keys = frequency.keySet().toImmutable();
        final Node[] nodes = new Node[length];
        int nodeCount;
        for (nodeCount = 0; nodeCount < length; nodeCount++) {
            final int key = keys.valueAt(nodeCount);
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

        final int[] symbols = new int[length];
        for (int i = 0; i < length; i++) {
            final int bitLength = bitLengths[i];
            symbols[symbolsPerBitLength[bitLength]++] = keys.valueAt(i);
        }

        return new DefinedIntHuffmanTable(tableIndexes, symbols);
    }

    public static DefinedIntHuffmanTable from(IntTraversable symbols) {
        final MutableIntPairMap frequency = MutableIntPairMap.empty();
        for (int symbol : symbols) {
            final int mapValue = frequency.get(symbol, 0);
            frequency.put(symbol, mapValue + 1);
        }

        return withFrequencies(frequency);
    }
}
