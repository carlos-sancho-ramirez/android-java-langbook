package sword.bitstream.huffman;

import sword.collections.IntTraversable;
import sword.collections.IntTraverser;
import sword.collections.Traverser;

public final class RangedIntHuffmanTable implements IntHuffmanTable {
    private final int _min;
    private final int _max;

    private final int _maxBits;
    private final int _limit;

    public RangedIntHuffmanTable(int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("Invalid range");
        }

        _min = min;
        _max = max;

        final int possibilities = max - min + 1;
        int maxBits = 0;
        while (possibilities > (1 << maxBits)) {
            maxBits++;
        }

        _maxBits = maxBits;
        _limit = (1 << maxBits) - possibilities;
    }

    @Override
    public int symbolsWithBits(int bits) {
        if (bits == _maxBits) {
            return _max - _min + 1 - _limit;
        }
        else if (bits == _maxBits - 1) {
            return _limit;
        }

        return 0;
    }

    @Override
    public int getSymbol(int bits, int index) {
        if (bits == _maxBits) {
            return index + _limit + _min;
        }
        else if (bits == _maxBits - 1) {
            return index + _min;
        }

        throw new IllegalArgumentException("Invalid number of bits");
    }

    private static final IntTraverser _emptyTraverser = new IntTraverser() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Integer next() {
            throw new UnsupportedOperationException("Unable to retrieve elements from an empty iterator");
        }
    };

    private static final IntTraversable _emptyTraversable = () -> _emptyTraverser;

    private final class BitLevelIterator implements IntTraverser {

        private final int _bits;
        private final int _symbolCount;
        private int _index;

        BitLevelIterator(int bits) {
            _bits = bits;
            _symbolCount = symbolsWithBits(bits);
        }

        @Override
        public boolean hasNext() {
            return _index < _symbolCount;
        }

        @Override
        public Integer next() {
            return getSymbol(_bits, _index++);
        }
    }

    private final class BitLevelIterable implements IntTraversable {

        private final int _bits;

        BitLevelIterable(int bits) {
            _bits = bits;
        }

        @Override
        public IntTraverser iterator() {
            return new BitLevelIterator(_bits);
        }
    }

    private final class TableIterator implements Traverser<IntTraversable> {

        private int _bits;
        private int _remaining = 1;

        @Override
        public boolean hasNext() {
            return _remaining > 0;
        }

        @Override
        public IntTraversable next() {
            final int symbolCount = symbolsWithBits(_bits);
            _remaining = (_remaining - symbolCount) * 2;

            if (symbolCount == 0) {
                ++_bits;
                return _emptyTraversable;
            }

            return new BitLevelIterable(_bits++);
        }
    }

    @Override
    public Traverser<IntTraversable> iterator() {
        return new TableIterator();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + _min + ',' + _max + ')';
    }
}
