package sword.bitstream;

import java.io.IOException;

/**
 * Callback used to encode the length of any collection.
 */
public interface CollectionLengthEncoder {

    /**
     * Encode the given length within the stream.
     * @param length Positive or 0 value to be encoded.
     * @throws IOException if the stream cannot be written.
     */
    void encodeLength(int length) throws IOException;
}
