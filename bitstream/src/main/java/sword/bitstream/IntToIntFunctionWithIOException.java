package sword.bitstream;

import java.io.IOException;

public interface IntToIntFunctionWithIOException {
    int apply(int param) throws IOException;
}
