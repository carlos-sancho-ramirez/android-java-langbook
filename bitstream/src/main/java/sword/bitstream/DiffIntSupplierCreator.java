package sword.bitstream;

public interface DiffIntSupplierCreator {
    IntToIntFunctionWithIOException create(InputBitStream stream);
}
