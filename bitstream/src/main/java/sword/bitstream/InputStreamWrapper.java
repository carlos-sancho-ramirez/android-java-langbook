package sword.bitstream;

import java.io.InputStream;

/**
 * Wrapper for a Java InputStream that adds functionality to read serialiazed content.
 * <p>
 * This is a complementary class for {@link OutputStreamWrapper}. Thus, this class
 * provides lot of methods to read what the complementary class has written in
 * to the output stream.
 */
public class InputStreamWrapper extends InputStreamWrapperWithHuffman implements InputCollectionStream {

    /**
     * Create a new instance wrapping the given InputStream.
     * @param is InputStream used to read.
     */
    public InputStreamWrapper(InputStream is) {
        super(is);
    }
}
