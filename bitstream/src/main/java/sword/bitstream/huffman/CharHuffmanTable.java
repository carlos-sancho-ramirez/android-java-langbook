package sword.bitstream.huffman;

/**
 * Huffman table that allow encoding characters by its unicode.
 */
public final class CharHuffmanTable extends AbstractNaturalNumberHuffmanTable<Character> {

    /**
     * Create a new instance with the given bit alignment.
     * @param bitAlign Number of bits that the most probable symbols will have.
     *                 Check {@link sword.bitstream.huffman.CharHuffmanTable} for more information.
     */
    public CharHuffmanTable(int bitAlign) {
        super(bitAlign);
    }

    @Override
    Character box(long value) {
        if (value > Integer.MAX_VALUE) {
            throw new AssertionError("Symbol exceeds the signed 32-bits bounds. Consider using LongNaturalNumberHuffmanTable instead.");
        }

        return (char) value;
    }
}
