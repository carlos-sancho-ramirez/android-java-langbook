package sword.bitstream;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.DefinedIntHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.IntHuffmanTable;

import java.io.IOException;

public interface InputHuffmanStream extends InputBitStream {

    /**
     * Read a symbol from the stream according to the given Huffman table.
     * @param table Huffman table used to decode the symbol.
     * @param <E> Type of the symbol to decode.
     * @return The symbol found in the stream according too the Huffman table.
     * @throws IOException if it is not possible to read from the stream.
     */
    default <E> E readHuffmanSymbol(HuffmanTable<E> table) throws IOException {
        return new HuffmanSymbolSupplier<>(table, this).apply();
    }

    /**
     * Read a Huffman table from the stream.
     * <p>
     * This is the complementary method of {@link OutputHuffmanStream#writeHuffmanTable(DefinedHuffmanTable, ProcedureWithIOException, Procedure2WithIOException)}
     *
     * @param supplier Used to read each of the symbols from the stream.
     *                 This may not be called for all symbols if diffSupplier method is present.
     * @param diffSupplier Optional function used to write a symbol based on the previous one.
     * @param <E> Type of the decoded symbol expected in the Huffman table.
     * @return The HuffmanTable resulting of reading the stream.
     * @throws IOException if it is unable to read from the wrapped stream.
     *
     * @see OutputHuffmanStream#writeHuffmanTable(DefinedHuffmanTable, ProcedureWithIOException, Procedure2WithIOException)
     */
    default <E> DefinedHuffmanTable<E> readHuffmanTable(
            SupplierWithIOException<E> supplier,
            FunctionWithIOException<E, E> diffSupplier) throws IOException {
        final SupplierCreator<E> supplierCreator = stream -> supplier;
        final DiffSupplierCreator<E> diffSupplierCreator = (diffSupplier != null)? stream -> diffSupplier : null;
        return new HuffmanTableSupplier<>(supplierCreator, diffSupplierCreator, this).apply();
    }

    default int readIntHuffmanSymbol(IntHuffmanTable table) throws IOException {
        return new IntHuffmanSymbolSupplier(table, this).apply();
    }

    default DefinedIntHuffmanTable readIntHuffmanTable(
            IntSupplierWithIOException supplier,
            IntToIntFunctionWithIOException diffSupplier) throws IOException {
        final IntSupplierCreator supplierCreator = stream -> supplier;
        final DiffIntSupplierCreator diffSupplierCreator = (diffSupplier != null)? stream -> diffSupplier : null;
        return new IntHuffmanTableSupplier(supplierCreator, diffSupplierCreator, this).apply();
    }
}
