package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.DefinedIntHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.IntTraversable;
import sword.collections.IntTraverser;

public final class IntHuffmanTableWriter implements ProcedureWithIOException<DefinedIntHuffmanTable> {
    private final OutputBitStream _stream;
    private final IntWriterCreator _writerCreator;
    private final DiffIntWriterCreator _diffWriterCreator;

    public IntHuffmanTableWriter(IntWriterCreator writerCreator, DiffIntWriterCreator diffWriterCreator, OutputHuffmanStream stream) {
        _writerCreator = writerCreator;
        _diffWriterCreator = diffWriterCreator;
        _stream = stream;
    }

    @Override
    public void apply(DefinedIntHuffmanTable table) throws IOException {
        int bits = 0;
        int max = 1;
        while (max > 0) {
            final int levelLength = table.symbolsWithBits(bits++);
            final HuffmanSymbolWriter<Integer> writer = new HuffmanSymbolWriter<>(new RangedIntegerHuffmanTable(0, max), _stream);
            writer.apply(levelLength);
            max -= levelLength;
            max <<= 1;
        }

        final IntProcedureWithIOException proc = _writerCreator.create(_stream);
        final IntProcedure2WithIOException diffProc = (_diffWriterCreator != null)? _diffWriterCreator.create(_stream) : null;
        for (IntTraversable level : table) {
            IntTraverser it = level.iterator();
            int previous = 0;
            if (it.hasNext()) {
                previous = it.next();
                proc.apply(previous);
            }

            while (it.hasNext()) {
                int element = it.next();
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
