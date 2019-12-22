package sword.bitstream;

public interface SupplierCreator<T> {
    SupplierWithIOException<T> create(InputBitStream stream);
}
