package sword.bitstream;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamWrapperWithHuffman implements InputHuffmanStream {
    private final InputStream _is;
    private int _buffer;
    private int _bitsOnBuffer;
    private boolean _closed;

    /**
     * Create a new instance wrapping the given InputStream.
     * @param is InputStream used to read.
     */
    public InputStreamWrapperWithHuffman(InputStream is) {
        if (is == null) {
            throw new IllegalArgumentException();
        }

        _is = is;
    }

    /**
     * Close this stream and the wrapped one.
     * @throws IOException if it is not possible to close it.
     *                     This is usually because it is already closed.
     */
    @Override
    public void close() throws IOException {
        _is.close();
        _closed = true;
    }

    @Override
    public boolean readBoolean() throws IOException {
        if (_closed) {
            throw new IllegalArgumentException("Stream already closed");
        }

        if (_bitsOnBuffer == 0) {
            _buffer = _is.read();
            if (_buffer < 0) {
                throw new IOException("Stream end already reached");
            }

            _bitsOnBuffer = 8;
        }

        boolean value = (_buffer & 1) != 0;
        _buffer >>>= 1;
        --_bitsOnBuffer;

        return value;
    }
}
