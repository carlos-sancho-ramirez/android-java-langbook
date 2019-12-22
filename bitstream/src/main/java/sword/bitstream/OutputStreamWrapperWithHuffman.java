package sword.bitstream;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamWrapperWithHuffman implements OutputHuffmanStream {
    private final OutputStream _os;
    private int _buffer;
    private int _bitsOnBuffer;
    private boolean _closed;

    /**
     * Create a new instance wrapping the given {@link OutputStream}
     * @param os {@link OutputStream} to use to write the encoded data.
     */
    public OutputStreamWrapperWithHuffman(OutputStream os) {
        if (os == null) {
            throw new IllegalArgumentException();
        }

        _os = os;
    }

    /**
     * Close this stream and the wrapped one.
     * @throws IOException if it is unable to write into the stream or it is already close.
     */
    @Override
    public void close() throws IOException {
        if (_bitsOnBuffer > 0) {
            _os.write(_buffer);
        }

        _os.close();
        _closed = true;
    }

    private void flushByte() throws IOException {
        while (_bitsOnBuffer >= 8) {
            _os.write(_buffer);
            _buffer >>>= 8;
            _bitsOnBuffer -= 8;
        }
    }

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
    public void writeBoolean(boolean value) throws IOException {
        if (_closed) {
            throw new IllegalArgumentException("Stream already closed");
        }

        if (value) {
            _buffer |= 1 << _bitsOnBuffer;
        }

        _bitsOnBuffer++;
        flushByte();
    }
}
