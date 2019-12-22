package sword.bitstream;

import java.io.IOException;

public interface ProcedureWithIOException<E> {
    void apply(E element) throws IOException;
}
