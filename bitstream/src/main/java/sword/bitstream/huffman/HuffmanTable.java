package sword.bitstream.huffman;

/**
 * Data structure to encode and decode symbols using the Huffman algorithm.
 * @param <E> Type of symbol to encode or decode
 */
public interface HuffmanTable<E> extends Iterable<Iterable<E>> {

    /**
     * Return the number of symbols that are encoded with the given amount of bits.
     * @param bits Number of bits used to encode a symbol.
     * @return the number of symbols that are encoded with the given amount of bits.
     */
    int symbolsWithBits(int bits);

    /**
     * Return the symbol for the given amount of bits in the given position of the array.
     * @param bits Number of bits used to encode the symbol.
     * @param index Index within the array of symbols assigned to that amount of bits.
     *              Index 0 is the first position of the array. Negative numbers
     *              and indexes equals or bigger than the value returned in
     *              {@link #symbolsWithBits(int)} should not be provided.
     * @return The symbol within the table for the given position.
     */
    E getSymbol(int bits, int index);
}
