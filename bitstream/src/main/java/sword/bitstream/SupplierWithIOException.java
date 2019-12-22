package sword.bitstream;

import java.io.IOException;

public interface SupplierWithIOException<E> {
    E apply() throws IOException;
}
