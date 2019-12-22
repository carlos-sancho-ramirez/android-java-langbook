package sword.bitstream;

import java.io.IOException;

/**
 * Callback used to decode the length of any encoded.
 */
public interface CollectionLengthDecoder {

    /**
     * Decode the given length from the stream.
     * @return A positive or 0 value read from the stream.
     * @throws IOException if the stream cannot be read.
     */
    int decodeLength() throws IOException;
}
