package sword.langbook3.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import sword.bitstream.FunctionWithIOException;
import sword.bitstream.HuffmanTable;
import sword.bitstream.InputBitStream;
import sword.bitstream.NaturalNumberHuffmanTable;
import sword.bitstream.SupplierWithIOException;

public class DbManager extends SQLiteOpenHelper {

    private static final String DB_NAME = "Langbook";
    private static final int DB_VERSION = 5;

    private final Context _context;

    public DbManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        _context = context;
    }

    private static class CharReader implements SupplierWithIOException<Character> {

        private final InputBitStream _ibs;

        CharReader(InputBitStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Character apply() throws IOException {
            return _ibs.readChar();
        }
    }

    private static class CharDiffReader implements FunctionWithIOException<Character, Character> {

        private final InputBitStream _ibs;
        private final HuffmanTable<Long> _table;

        CharDiffReader(InputBitStream ibs, HuffmanTable<Long> table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Character apply(Character previous) throws IOException {
            long value = _ibs.readHuffmanSymbol(_table);
            return (char) (value + previous + 1);
        }
    }

    private static class IntReader implements SupplierWithIOException<Integer> {

        private final InputBitStream _ibs;

        IntReader(InputBitStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Integer apply() throws IOException {
            return (int) _ibs.readNaturalNumber();
        }
    }

    private static class IntDiffReader implements FunctionWithIOException<Integer, Integer> {

        private final InputBitStream _ibs;
        private final HuffmanTable<Long> _table;

        IntDiffReader(InputBitStream ibs, HuffmanTable<Long> table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Integer apply(Integer previous) throws IOException {
            return _ibs.readHuffmanSymbol(_table).intValue() + previous + 1;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE SymbolArrays (id INTEGER PRIMARY KEY AUTOINCREMENT, str TEXT)");

        final InputStream is = _context.getResources().openRawResource(R.raw.basic);
        try {
            is.skip(20);
            final InputBitStream ibs = new InputBitStream(is);

            final int symbolArraysLength = (int) ibs.readNaturalNumber();
            final HuffmanTable<Long> nat3Table = new NaturalNumberHuffmanTable(3);
            final HuffmanTable<Long> nat4Table = new NaturalNumberHuffmanTable(4);

            final HuffmanTable<Character> charHuffmanTable =
                    ibs.readHuffmanTable(new CharReader(ibs), new CharDiffReader(ibs, nat4Table));

            final HuffmanTable<Integer> symbolArraysLengthTable =
                    ibs.readHuffmanTable(new IntReader(ibs), new IntDiffReader(ibs, nat3Table));

            for (int index = 0; index < symbolArraysLength; index++) {
                final int length = ibs.readHuffmanSymbol(symbolArraysLengthTable);
                final StringBuilder builder = new StringBuilder();
                for (int pos = 0; pos < length; pos++) {
                    builder.append(ibs.readHuffmanSymbol(charHuffmanTable));
                }

                final String str = builder.toString();
                db.execSQL("INSERT INTO SymbolArrays (str) VALUES ('" + str + "')");
            }
        }
        catch (IOException e) {
            Toast.makeText(_context, "Error loading database", Toast.LENGTH_SHORT).show();
        }
        finally {
            try {
                is.close();
            } catch (IOException e) {
                // Nothing can be done
            }
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // So far, version 5 is the only one expected. So this method should never be called
    }
}
