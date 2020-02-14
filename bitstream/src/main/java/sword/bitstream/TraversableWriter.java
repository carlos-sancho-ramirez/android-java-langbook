package sword.bitstream;

import java.io.IOException;

import sword.collections.Traversable;

public final class TraversableWriter<T> implements ProcedureWithIOException<Traversable<T>> {

    private final CollectionLengthEncoder _lengthEncoder;
    private final ProcedureWithIOException<T> _symbolWriter;

    TraversableWriter(CollectionLengthEncoder lengthEncoder, ProcedureWithIOException<T> symbolWriter) {
        _lengthEncoder = lengthEncoder;
        _symbolWriter = symbolWriter;
    }

    @Override
    public void apply(Traversable<T> traversable) throws IOException {
        final int length = traversable.size();
        _lengthEncoder.encodeLength(length);

        for (T symbol : traversable) {
            _symbolWriter.apply(symbol);
        }
    }
}
