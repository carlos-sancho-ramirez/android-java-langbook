package sword.bitstream.huffman;

import java.util.Iterator;

/**
 * Huffman table to encode a range of integers with uniform probability.
 */
public final class RangedIntegerHuffmanTable implements HuffmanTable<Integer> {

    private final int _min;
    private final int _max;

    private final int _maxBits;
    private final int _limit;

    /**
     * Create a new instance for the given range of integers.
     *
     * @param min mininum expected value (inclusive)
     * @param max maximum expected value (inclusive)
     */
    public RangedIntegerHuffmanTable(int min, int max) {
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
    public Integer getSymbol(int bits, int index) {
        if (bits == _maxBits) {
            return index + _limit + _min;
        }
        else if (bits == _maxBits - 1) {
            return index + _min;
        }

        throw new IllegalArgumentException("Invalid number of bits");
    }

    private static final Iterator<Integer> _emptyIterator = new Iterator<Integer>() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Integer next() {
            throw new UnsupportedOperationException("Unable to retrieve elements from an empty iterator");
        }
    };

    private static final Iterable<Integer> _emptyIterable = () -> _emptyIterator;

    private final class BitLevelIterator implements Iterator<Integer> {

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

    private final class BitLevelIterable implements Iterable<Integer> {

        private final int _bits;

        BitLevelIterable(int bits) {
            _bits = bits;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new BitLevelIterator(_bits);
        }
    }

    private final class TableIterator implements Iterator<Iterable<Integer>> {

        private int _bits;
        private int _remaining = 1;

        @Override
        public boolean hasNext() {
            return _remaining > 0;
        }

        @Override
        public Iterable<Integer> next() {
            final int symbolCount = symbolsWithBits(_bits);
            _remaining = (_remaining - symbolCount) * 2;

            if (symbolCount == 0) {
                ++_bits;
                return _emptyIterable;
            }

            return new BitLevelIterable(_bits++);
        }
    }

    @Override
    public Iterator<Iterable<Integer>> iterator() {
        return new TableIterator();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + _min + ',' + _max + ')';
    }
}
