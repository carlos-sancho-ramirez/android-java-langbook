package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

/**
 * Set of methods to read serialiazed content.
 * <p>
 * This is a complementary class for {@link OutputBitStream}. Thus, this class
 * provides lot of methods to read what the complementary class has written in
 * to the output stream.
 */
public interface InputBitStream extends Closeable {

    /**
     * Read a single boolean from the stream.
     * <p>
     * A byte has 8 bits and a boolean can be represented with a single bits.
     * Thus, this method will only call {@link InputStream#read()} in the
     * wrapped stream once every 8 calls to this method, until reading all
     * bits from the previous read byte.
     * <p>
     * This is a key method and all other more complex methods within the class
     * depends on this.
     *
     * @return true or false depending on next bit value.
     * @throws IOException if it is unable to read from the wrapped stream or
     *                     this stream has been closed.
     */
    boolean readBoolean() throws IOException;
}
