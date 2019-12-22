package sword.bitstream;

import java.io.OutputStream;

/**
 * Wrapper for an {@link OutputStream} that provides optimal serialization
 * to compact and encode data into the stream.
 */
public final class OutputStreamWrapper extends OutputStreamWrapperWithHuffman implements OutputCollectionStream {

    public OutputStreamWrapper(OutputStream os) {
        super(os);
    }
}
