package sword.bitstream.huffman;

import sword.collections.IntPairMap;
import sword.collections.IntTraversable;
import sword.collections.IntTraverser;
import sword.collections.Traverser;

import static sword.bitstream.huffman.IntNumberHuffmanTable._invalidLevelTraversable;

/**
 * Huffman table that allow encoding natural numbers.
 * Negative numbers are not part of the this table.
 * <p>
 * This table assign less bits to the smaller values and more bits to bigger ones.
 * Thus, zero is always the most probable one and then the one that takes less bits.
 * <p>
 * This Huffman table assign always amount of bits that are multiple of the given
 * bit align. Trying to fit inside the lower values and adding more bits for bigger values.
 * <p>
 * E.g. if bitAlign is 4 the resulting table will assign symbols from 0 to 7 to
 * the unique symbols with 4 bits once included, leaving the first bit as a switch
 * to extend the number of bits.
 * <code>
 * <br>&nbsp;&nbsp;0000 &rArr; 0
 * <br>&nbsp;&nbsp;0001 &rArr; 1
 * <br>&nbsp;&nbsp;0010 &rArr; 2
 * <br>&nbsp;&nbsp;0011 &rArr; 3
 * <br>&nbsp;&nbsp;0100 &rArr; 4
 * <br>&nbsp;&nbsp;0101 &rArr; 5
 * <br>&nbsp;&nbsp;0110 &rArr; 6
 * <br>&nbsp;&nbsp;0111 &rArr; 7
 * <br></code>
 * <p>
 * Note that all encoded symbols start with <code>0</code>. In reality the amount of <code>1</code> before
 * this zero reflects the number of bits for this symbol. When the zero is the first
 * one, the amount of bit for the symbol is understood to match the bit align value.
 * When there are one <code>1</code> in front the zero (<code>10</code>) then it will be the bit align
 * value multiplied by 2. Thus <code>110</code> will be <code>bitAlign * 3</code>, <code>1110</code> will be
 * <code>bitAlign * 4</code> and so on.
 * <code>
 * <br>&nbsp;&nbsp;10000000 &rArr; 8
 * <br>&nbsp;&nbsp;10000001 &rArr; 9
 * <br>&nbsp;&nbsp;...
 * <br>&nbsp;&nbsp;10111111 &rArr; 71
 * <br>&nbsp;&nbsp;110000000000 &rArr; 72
 * <br>&nbsp;&nbsp;110000000001 &rArr; 73
 * <br>&nbsp;&nbsp;...
 * <br></code>
 * <p>
 * This table can theoretically include any number, even if it is really big.
 * Technically it is currently limited to the int bounds (32-bit integer).
 * As it can include any number and numbers are infinite, this table is
 * infinite as well and its iterable will not converge.
 */
public final class NatNumberHuffmanTable implements IntHuffmanTable {

    private final int _bitAlign;

    /**
     * Create a new instance with the given bit alignment.
     * @param bitAlign Number of bits that the most probable symbols will have.
     *                 Check {@link NatNumberHuffmanTable} for more information.
     */
    public NatNumberHuffmanTable(int bitAlign) {
        if (bitAlign < 2) {
            throw new IllegalArgumentException();
        }

        _bitAlign = bitAlign;
    }

    /**
     * Return the bit alignment provided in construction time.
     * This value can be used to encode this table, as it is the only relevant number.
     * @return The bit alignment of this Huffman table.
     */
    public int getBitAlign() {
        return _bitAlign;
    }

    private boolean isValidLevel(int level) {
        return level > 0 && (level % _bitAlign) == 0;
    }

    private int getSymbolsAtLevel(int level) {
        return 1 << ((level / _bitAlign) * (_bitAlign - 1));
    }

    @Override
    public int symbolsWithBits(int bits) {
        return isValidLevel(bits)? getSymbolsAtLevel(bits) : 0;
    }

    private int getBaseFromLevel(int level) {
        int base = 0;
        int exp = (level - 1) / _bitAlign;
        while (exp > 0) {
            base += 1 << (exp * (_bitAlign - 1));
            exp--;
        }

        return base;
    }

    @Override
    public int getSymbol(int bits, int index) {
        if (!isValidLevel(bits)) {
            throw new IllegalArgumentException();
        }

        return getBaseFromLevel(bits) + index;
    }

    private class LevelTraverser implements IntTraverser {
        private final int _lastLevelSymbol;
        private int _next;

        LevelTraverser(int base, int lastLevelSymbol) {
            _lastLevelSymbol = lastLevelSymbol;
            _next = base;
        }

        @Override
        public boolean hasNext() {
            return _next <= _lastLevelSymbol;
        }

        @Override
        public Integer next() {
            return _next++;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class LevelTraversable implements IntTraversable {
        private final int _level;

        LevelTraversable(int level) {
            _level = level;
        }

        @Override
        public IntTraverser iterator() {
            final int base = getBaseFromLevel(_level);
            final int lastLevelSymbol = base + getSymbolsAtLevel(_level) - 1;
            return new LevelTraverser(base, lastLevelSymbol);
        }
    }

    class TableTraverser implements Traverser<IntTraversable> {
        private int _level;

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public IntTraversable next() {
            final int level = _level++;
            return isValidLevel(level)? new LevelTraversable(level) : _invalidLevelTraversable;
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
    public Traverser<IntTraversable> iterator() {
        return new TableTraverser();
    }

    /**
     * Build a new instance based on the given map of frequencies.
     * Check {@link DefinedIntHuffmanTable#withFrequencies(IntPairMap)} for more detail.
     *
     * @param frequency Map of frequencies.
     * @return A new instance create.
     * @see DefinedIntHuffmanTable#withFrequencies(IntPairMap)
     */
    public static NatNumberHuffmanTable withFrequencies(IntPairMap frequency) {
        int maxValue = Integer.MIN_VALUE;
        for (int symbol : frequency.keySet()) {
            if (symbol < 0) {
                throw new IllegalArgumentException("Found a negative number");
            }

            if (symbol > maxValue) {
                maxValue = symbol;
            }
        }

        if (maxValue < 0) {
            throw new IllegalArgumentException("map should not be empty");
        }

        int requiredBits = 0;
        int possibilities = 1;
        while (maxValue > possibilities) {
            possibilities <<= 1;
            requiredBits++;
        }

        final int minValidBitAlign = 2;

        // Any maxCheckedBitAlign bigger than requiredBits + 1 will always increase
        // for sure the number of required bits. That's why the limit is set here.
        final int maxCheckedBitAlign = requiredBits + 1;

        int minSize = Integer.MAX_VALUE;
        int bestBitAlign = 0;

        for (int bitAlign = minValidBitAlign; bitAlign <= maxCheckedBitAlign; bitAlign++) {
            int length = 0;
            for (IntPairMap.Entry entry : frequency.entries()) {
                final int symbol = entry.key();
                int packs = 1;
                int nextBase = 1 << (bitAlign - 1);
                while (symbol >= nextBase) {
                    packs++;
                    nextBase += 1 << ((bitAlign - 1) * packs);
                }

                length += bitAlign * packs * entry.value();
                if (length > minSize) {
                    break;
                }
            }

            if (length < minSize) {
                minSize = length;
                bestBitAlign = bitAlign;
            }
        }

        return new NatNumberHuffmanTable(bestBitAlign);
    }
}
