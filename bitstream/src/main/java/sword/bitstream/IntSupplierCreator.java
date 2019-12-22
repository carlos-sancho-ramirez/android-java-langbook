package sword.bitstream;

public interface IntSupplierCreator {
    IntSupplierWithIOException create(InputBitStream stream);
}
