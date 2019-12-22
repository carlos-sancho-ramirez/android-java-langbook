package sword.bitstream;

public interface DiffSupplierCreator<T> {
    FunctionWithIOException<T, T> create(InputBitStream stream);
}
