package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

/**
 * Set of methods to write serialized content.
 */
public interface OutputBitStream extends Closeable {

    /**
     * Write a boolean into the stream.
     * <p>
     * This method assumes that a byte has 8 bits and that a boolean can be
     * represented with a single bit. In other words, its possible to include
     * 8 booleans in each byte. With this assumption, this method will only
     * write into the wrapped stream once every 8 calls, once it has the values
     * for each of the booleans composing a byte.
     * <p>
     * Note that calling {@link #close()} in this class is required in order to
     * store all buffered booleans before closing the stream.
     * <p>
     * This is a key method within the class and all other methods depends on it.
     *
     * @param value boolean to be encoded and written into the stream.
     * @throws IOException if it is unable to write into the stream.
     */
    void writeBoolean(boolean value) throws IOException;
}
