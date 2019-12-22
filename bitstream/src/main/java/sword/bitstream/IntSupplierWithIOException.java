package sword.bitstream;

import java.io.IOException;

public interface IntSupplierWithIOException {
    int apply() throws IOException;
}
