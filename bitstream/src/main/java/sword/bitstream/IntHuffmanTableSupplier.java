package sword.bitstream;

import java.io.IOException;
import java.util.ArrayList;

import sword.bitstream.huffman.DefinedIntHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.MutableIntList;
import sword.collections.MutableList;

public final class IntHuffmanTableSupplier implements SupplierWithIOException<DefinedIntHuffmanTable> {
    private final InputBitStream _stream;
    private final IntSupplierCreator _supplierCreator;
    private final DiffIntSupplierCreator _diffSupplierCreator;

    IntHuffmanTableSupplier(IntSupplierCreator supplierCreator, DiffIntSupplierCreator diffSupplierCreator, InputBitStream stream) {
        _supplierCreator = supplierCreator;
        _diffSupplierCreator = diffSupplierCreator;
        _stream = stream;
    }

    @Override
    public DefinedIntHuffmanTable apply() throws IOException {
        final ArrayList<Integer> levelLengths = new ArrayList<>();
        int max = 1;
        while (max > 0) {
            final HuffmanSymbolSupplier<Integer> lengthSymbolSupplier = new HuffmanSymbolSupplier<>(new RangedIntegerHuffmanTable(0, max), _stream);
            final int levelLength = lengthSymbolSupplier.apply();
            levelLengths.add(levelLength);
            max -= levelLength;
            max <<= 1;
        }

        final MutableList<MutableIntList> symbols = MutableList.empty();
        final IntSupplierWithIOException supplier = _supplierCreator.create(_stream);
        final IntToIntFunctionWithIOException diffSupplier = (_diffSupplierCreator != null)? _diffSupplierCreator.create(_stream) : null;
        for (int levelLength : levelLengths) {
            final MutableIntList level = MutableIntList.empty();
            int element = 0;
            if (levelLength > 0) {
                element = supplier.apply();
                level.append(element);
            }

            for (int i = 1; i < levelLength; i++) {
                if (diffSupplier != null) {
                    element = diffSupplier.apply(element);
                }
                else {
                    element = supplier.apply();
                }
                level.append(element);
            }

            symbols.append(level);
        }

        return DefinedIntHuffmanTable.fromTraversable(symbols);
    }
}
