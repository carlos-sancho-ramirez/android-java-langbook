package sword.bitstream.huffman;

import sword.collections.IntTraversable;
import sword.collections.Traversable;
import sword.collections.Traverser;

public interface IntHuffmanTable extends Traversable<IntTraversable> {
    int symbolsWithBits(int bits);
    int getSymbol(int bits, int index);
}
