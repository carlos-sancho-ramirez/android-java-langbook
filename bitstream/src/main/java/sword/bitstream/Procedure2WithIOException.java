package sword.bitstream;

import java.io.IOException;

public interface Procedure2WithIOException<E> {
    void apply(E previous, E element) throws IOException;
}
