package sword.bitstream;

import java.io.IOException;

public interface IntProcedure2WithIOException {
    void apply(int previous, int element) throws IOException;
}
