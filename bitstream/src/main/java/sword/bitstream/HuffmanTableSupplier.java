package sword.bitstream;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

import java.io.IOException;
import java.util.ArrayList;

public final class HuffmanTableSupplier<T> implements SupplierWithIOException<DefinedHuffmanTable<T>> {

    private final InputBitStream _stream;
    private final SupplierCreator<T> _supplierCreator;
    private final DiffSupplierCreator<T> _diffSupplierCreator;

    public HuffmanTableSupplier(SupplierCreator<T> supplierCreator, DiffSupplierCreator<T> diffSupplierCreator, InputBitStream stream) {
        _supplierCreator = supplierCreator;
        _diffSupplierCreator = diffSupplierCreator;
        _stream = stream;
    }

    @Override
    public DefinedHuffmanTable<T> apply() throws IOException {
        final ArrayList<Integer> levelLengths = new ArrayList<>();
        int max = 1;
        while (max > 0) {
            final HuffmanSymbolSupplier<Integer> lengthSymbolSupplier = new HuffmanSymbolSupplier<>(new RangedIntegerHuffmanTable(0, max), _stream);
            final int levelLength = lengthSymbolSupplier.apply();
            levelLengths.add(levelLength);
            max -= levelLength;
            max <<= 1;
        }

        final ArrayList<Iterable<T>> symbols = new ArrayList<>(levelLengths.size());
        final SupplierWithIOException<T> supplier = _supplierCreator.create(_stream);
        final FunctionWithIOException<T, T> diffSupplier = (_diffSupplierCreator != null)? _diffSupplierCreator.create(_stream) : null;
        for (int levelLength : levelLengths) {
            final ArrayList<T> level = new ArrayList<>();
            T element = null;
            if (levelLength > 0) {
                element = supplier.apply();
                level.add(element);
            }

            for (int i = 1; i < levelLength; i++) {
                if (diffSupplier != null) {
                    element = diffSupplier.apply(element);
                }
                else {
                    element = supplier.apply();
                }
                level.add(element);
            }

            symbols.add(level);
        }

        return DefinedHuffmanTable.fromIterable(symbols);
    }
}
