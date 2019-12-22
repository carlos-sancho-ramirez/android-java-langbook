package sword.bitstream.huffman;

import java.util.Iterator;

abstract class AbstractIntegerNumberHuffmanTable<T> implements HuffmanTable<T> {

    private final int _bitAlign;

    AbstractIntegerNumberHuffmanTable(int bitAlign) {
        if (bitAlign < 2) {
            throw new IllegalArgumentException();
        }

        _bitAlign = bitAlign;
    }

    abstract T box(long value);

    /**
     * Return the bit alignment provided in construction time.
     * This value can be used to encode this table, as it is the only relevant number.
     * @return The bit alignment of this Huffman table.
     */
    public int getBitAlign() {
        return _bitAlign;
    }

    private boolean isValidLevel(int level) {
        return level > 0 && ((level % _bitAlign) == 0);
    }

    private int getSymbolsAtLevel(int level) {
        return 1 << ((level / _bitAlign) * (_bitAlign - 1));
    }

    @Override
    public int symbolsWithBits(int bits) {
        return isValidLevel(bits)? getSymbolsAtLevel(bits) : 0;
    }

    private long getBaseFromLevel(int level) {
        long base = 0;
        int exp = ((level - 1) / _bitAlign) * (_bitAlign - 1) - 1;
        while (exp > 0) {
            base += 1 << exp;
            exp -= _bitAlign - 1;
        }

        return base;
    }

    private long getNegativeBaseFromLevel(int level) {
        long base = 0;
        int exp = (level / _bitAlign) * (_bitAlign - 1) - 1;
        while (exp > 0) {
            base -= 1 << exp;
            exp -= _bitAlign - 1;
        }

        return base;
    }

    @Override
    public T getSymbol(int bits, int index) {
        if (!isValidLevel(bits)) {
            throw new IllegalArgumentException();
        }

        int symbolsPerSegment = getSymbolsAtLevel(bits) / 2;
        return box((index < symbolsPerSegment)?
                getBaseFromLevel(bits) + index :
                getNegativeBaseFromLevel(bits) + (index - symbolsPerSegment));
    }

    private class LevelIterator implements Iterator<T> {

        private final int _level;
        private int _index;

        LevelIterator(int level) {
            _level = level;
        }

        @Override
        public boolean hasNext() {
            return _index < symbolsWithBits(_level);
        }

        @Override
        public T next() {
            return getSymbol(_level, _index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class LevelIterable implements Iterable<T> {

        private final int _level;

        LevelIterable(int level) {
            _level = level;
        }

        @Override
        public Iterator<T> iterator() {
            return new LevelIterator(_level);
        }
    }

    private static final Iterator _invalidLevelIterator = new Iterator() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    private static final Iterable _invalidLevelIterable = () -> _invalidLevelIterator;

    private class TableIterator implements Iterator<Iterable<T>> {

        private int _level;

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Iterable<T> next() {
            final int level = _level++;
            return isValidLevel(level)? new LevelIterable(level) : _invalidLevelIterable;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Return an iterator to check symbols one by one in the given order.
     * Note that this iterator will never converge and this table is infinite.
     */
    @Override
    public Iterator<Iterable<T>> iterator() {
        return new TableIterator();
    }
}
