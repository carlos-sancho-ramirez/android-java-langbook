package sword.bitstream;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

import java.io.IOException;
import java.util.Iterator;

public final class HuffmanTableWriter<T> implements ProcedureWithIOException<DefinedHuffmanTable<T>> {

    private final OutputBitStream _stream;
    private final WriterCreator<T> _writerCreator;
    private final DiffWriterCreator<T> _diffWriterCreator;

    public HuffmanTableWriter(WriterCreator<T> writerCreator, DiffWriterCreator<T> diffWriterCreator, OutputHuffmanStream stream) {
        _writerCreator = writerCreator;
        _diffWriterCreator = diffWriterCreator;
        _stream = stream;
    }

    @Override
    public void apply(DefinedHuffmanTable<T> table) throws IOException {
        int bits = 0;
        int max = 1;
        while (max > 0) {
            final int levelLength = table.symbolsWithBits(bits++);
            final HuffmanSymbolWriter<Integer> writer = new HuffmanSymbolWriter<>(new RangedIntegerHuffmanTable(0, max), _stream);
            writer.apply(levelLength);
            max -= levelLength;
            max <<= 1;
        }

        final ProcedureWithIOException<T> proc = _writerCreator.create(_stream);
        final Procedure2WithIOException<T> diffProc = (_diffWriterCreator != null)? _diffWriterCreator.create(_stream) : null;
        for (Iterable<T> level : table) {
            Iterator<T> it = level.iterator();
            T previous = null;
            if (it.hasNext()) {
                previous = it.next();
                proc.apply(previous);
            }

            while (it.hasNext()) {
                T element = it.next();
                if (diffProc != null) {
                    diffProc.apply(previous, element);
                    previous = element;
                }
                else {
                    proc.apply(element);
                }
            }
        }
    }
}
