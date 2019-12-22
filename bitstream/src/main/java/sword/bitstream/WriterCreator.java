package sword.bitstream;

public interface WriterCreator<T> {
    ProcedureWithIOException<T> create(OutputBitStream stream);
}
