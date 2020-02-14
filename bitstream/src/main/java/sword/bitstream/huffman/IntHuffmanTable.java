package sword.bitstream.huffman;

import sword.collections.IntTraversable;
import sword.collections.Traversable;

public interface IntHuffmanTable extends Traversable<IntTraversable> {
    int symbolsWithBits(int bits);
    int getSymbol(int bits, int index);
}
