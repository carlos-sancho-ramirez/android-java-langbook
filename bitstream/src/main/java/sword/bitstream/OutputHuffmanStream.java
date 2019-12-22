package sword.bitstream;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.DefinedIntHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.IntHuffmanTable;

import java.io.IOException;

public interface OutputHuffmanStream extends OutputBitStream {

    /**
     * Write a symbol into the stream using the given Huffman table.
     * @param table Huffman table that specifies how to encode the symbol
     * @param symbol Symbol to encode. It must be present in the Huffman table.
     * @param <E> Type for the symbol to encode.
     * @throws IOException if it is unable to write into the stream.
     */
    default <E> void writeHuffmanSymbol(HuffmanTable<E> table, E symbol) throws IOException {
        new HuffmanSymbolWriter<>(table, this).apply(symbol);
    }

    /**
     * Write a Huffman table into the stream.
     * <p>
     * As the symbol has a generic type, it is required that the caller of this
     * function provide the proper procedures to write each symbol.
     *
     * @param table Huffman table to encode.
     * @param proc Procedure to write a single symbol. This may not be called
     *             for all symbols if diffProc is different from null in order
     *             to reduce the amount of data to write.
     * @param diffProc Optional procedure to write a symbol based in a previous one.
     *                 This may compress in a better degree the table if their symbols are sortered.
     *                 In case this is null, the function given in proc will be called instead.
     * @param <E> Type of the symbol to encode.
     * @throws IOException if it is unable to write into the stream.
     */
    default <E> void writeHuffmanTable(DefinedHuffmanTable<E> table,
            ProcedureWithIOException<E> proc, Procedure2WithIOException<E> diffProc) throws IOException {
        final WriterCreator<E> writerCreator = stream -> proc;
        final DiffWriterCreator<E> diffWriterCreator = (diffProc != null)? stream -> diffProc : null;
        new HuffmanTableWriter<>(writerCreator, diffWriterCreator, this).apply(table);
    }

    default void writeIntHuffmanSymbol(IntHuffmanTable table, int symbol) throws IOException {
        new IntHuffmanSymbolWriter(table, this).apply(symbol);
    }

    default void writeIntHuffmanTable(DefinedIntHuffmanTable table,
                                      IntProcedureWithIOException proc, IntProcedure2WithIOException diffProc) throws IOException {
        final IntWriterCreator writerCreator = stream -> proc;
        final DiffIntWriterCreator diffWriterCreator = (diffProc != null)? stream -> diffProc : null;
        new IntHuffmanTableWriter(writerCreator, diffWriterCreator, this).apply(table);
    }
}
