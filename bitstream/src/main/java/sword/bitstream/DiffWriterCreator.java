package sword.bitstream;

public interface DiffWriterCreator<T> {
    Procedure2WithIOException<T> create(OutputBitStream stream);
}
