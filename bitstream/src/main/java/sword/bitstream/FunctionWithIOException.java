package sword.bitstream;

import java.io.IOException;

public interface FunctionWithIOException<P, R> {
    R apply(P param) throws IOException;
}
