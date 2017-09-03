package sword.langbook3.android;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseIntArray;
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

    private static class CharHuffmanSymbolDiffReader implements FunctionWithIOException<Character, Character> {

        private final InputBitStream _ibs;
        private final HuffmanTable<Long> _table;

        CharHuffmanSymbolDiffReader(InputBitStream ibs, HuffmanTable<Long> table) {
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

        IntDiffReader(InputBitStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Integer apply(Integer previous) throws IOException {
            return (int) _ibs.readNaturalNumber() + previous + 1;
        }
    }

    private static class IntHuffmanSymbolDiffReader implements FunctionWithIOException<Integer, Integer> {

        private final InputBitStream _ibs;
        private final HuffmanTable<Long> _table;

        IntHuffmanSymbolDiffReader(InputBitStream ibs, HuffmanTable<Long> table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Integer apply(Integer previous) throws IOException {
            return _ibs.readHuffmanSymbol(_table).intValue() + previous + 1;
        }
    }

    static final class TableNames {
        static final String acceptations = "Acceptations";
        static final String conversions = "Conversions";
        static final String correlations = "Correlations";
        static final String correlationArrays = "CorrelationArrays";
        static final String symbolArrays = "SymbolArrays";
    }

    private String getSymbolArray(SQLiteDatabase db, int id) {
        final String whereClause = "id = ?";
        Cursor cursor = db.query(TableNames.symbolArrays, new String[] {"str"}, whereClause,
                new String[] { Integer.toString(id) }, null, null, null, null);
        if (cursor != null) {
            try {
                final int count = cursor.getCount();
                if (count > 1) {
                    throw new AssertionError("There should not be repeated identifiers");
                }

                if (count > 0 && cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            }
            finally {
                cursor.close();
            }
        }

        return null;
    }

    private Integer getSymbolArray(SQLiteDatabase db, String str) {
        final String whereClause = "str = ?";
        Cursor cursor = db.query(TableNames.symbolArrays, new String[] {"id"}, whereClause,
                new String[] { str }, null, null, null, null);
        if (cursor != null) {
            try {
                final int count = cursor.getCount();
                if (count > 1) {
                    throw new AssertionError("There should not be repeated symbol arrays");
                }

                if (count > 0 && cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            }
            finally {
                cursor.close();
            }
        }

        return null;
    }

    private Integer getAcceptation(SQLiteDatabase db, int word, int concept, int correlationArray) {
        final String whereClause = "word = ? AND concept = ? AND correlationArray = ?";
        Cursor cursor = db.query(TableNames.acceptations, new String[] {"id"}, whereClause,
                new String[] { Integer.toString(word), Integer.toString(concept), Integer.toString(correlationArray) }, null, null, null, null);
        if (cursor != null) {
            try {
                final int count = cursor.getCount();
                if (count > 1) {
                    throw new AssertionError("There should not be repeated acceptations");
                }

                if (count > 0 && cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            }
            finally {
                cursor.close();
            }
        }

        return null;
    }

    private static final class Pair<A,B> {

        private final A _left;
        private final B _right;

        Pair(A left, B right) {
            _left = left;
            _right = right;
        }

        A getLeft() {
            return _left;
        }

        B getRight() {
            return _right;
        }
    }

    private Pair<Integer, Integer> getAcceptation(SQLiteDatabase db, int word, int concept) {
        final String whereClause = "word = ? AND concept = ?";
        Cursor cursor = db.query(TableNames.acceptations, new String[] {"id", "correlationArray"}, whereClause,
                new String[] { Integer.toString(word), Integer.toString(concept) }, null, null, null, null);
        if (cursor != null) {
            try {
                final int count = cursor.getCount();
                if (count > 1) {
                    throw new AssertionError("There should not be repeated acceptations");
                }

                if (count > 0 && cursor.moveToFirst()) {
                    return new Pair<>(cursor.getInt(0), cursor.getInt(1));
                }
            }
            finally {
                cursor.close();
            }
        }

        return null;
    }

    private int insertSymbolArray(SQLiteDatabase db, String str) {
        db.execSQL("INSERT INTO " + TableNames.symbolArrays + " (str) VALUES ('" + str + "')");
        final Integer id = getSymbolArray(db, str);
        if (id == null) {
            throw new AssertionError("A just introduced register should be found");
        }

        return id;
    }

    private int insertIfNotExists(SQLiteDatabase db, String str) {
        final Integer id = getSymbolArray(db, str);
        if (id != null) {
            return id;
        }

        return insertSymbolArray(db, str);
    }

    private void insertConversion(SQLiteDatabase db, int sourceAlphabet, int targetAlphabet, int source, int target) {
        db.execSQL("INSERT INTO " + TableNames.conversions + " (sourceAlphabet, targetAlphabet, source, target) VALUES (" +
                sourceAlphabet + ',' + targetAlphabet + ',' + source + ',' + target + ')');
    }

    private int insertCorrelation(SQLiteDatabase db, SparseIntArray correlation) {
        Cursor cursor = db.rawQuery("SELECT max(correlationId) FROM " + TableNames.correlations, null);
        if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()) {
            throw new AssertionError("Unable to retrieve maximum correlationId");
        }

        final int newCorrelationId = cursor.getInt(0) + 1;
        final int mapLength = correlation.size();
        for (int i = 0; i < mapLength; i++) {
            final int alphabet = correlation.keyAt(i);
            final int symbolArray = correlation.valueAt(i);
            db.execSQL("INSERT INTO " + TableNames.correlations + " (correlationId, alphabet, symbolArray) VALUES (" +
                    newCorrelationId + ',' + alphabet + ',' + symbolArray + ')');
        }

        return newCorrelationId;
    }

    private int insertCorrelationArray(SQLiteDatabase db, int... correlation) {
        Cursor cursor = db.rawQuery("SELECT max(arrayId) FROM " + TableNames.correlationArrays, null);
        if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()) {
            throw new AssertionError("Unable to retrieve maximum arrayId");
        }

        final int newArrayId = cursor.getInt(0) + 1;
        final int arrayLength = correlation.length;
        for (int i = 0; i < arrayLength; i++) {
            final int corr = correlation[i];
            db.execSQL("INSERT INTO " + TableNames.correlationArrays + " (arrayId, arrayPos, correlation) VALUES (" +
                    newArrayId + ',' + i + ',' + corr + ')');
        }

        return newArrayId;
    }

    private int insertAcceptation(SQLiteDatabase db, int word, int concept, int correlationArray) {
        db.execSQL("INSERT INTO " + TableNames.acceptations + " (word, concept, correlationArray) VALUES (" +
                word + ',' + concept + ',' + correlationArray + ')');
        final Integer id = getAcceptation(db, word, concept, correlationArray);
        if (id == null) {
            throw new AssertionError("A just introduced register should be found");
        }

        return id;
    }

    private void assignAcceptationCorrelationArray(SQLiteDatabase db, int word, int correlationArrayId) {
        db.execSQL("UPDATE " + TableNames.acceptations + " SET correlationArray=" + correlationArrayId + " WHERE word=" + word);
    }

    private void assignAcceptationCorrelationArray(SQLiteDatabase db, int word, int concept, int correlationArrayId) {
        db.execSQL("UPDATE " + TableNames.acceptations + " SET correlationArray=" + correlationArrayId + " WHERE word=" + word + " AND concept=" + concept);
    }

    private int[] readSymbolArrays(SQLiteDatabase db, InputBitStream ibs) throws IOException {
        final int symbolArraysLength = (int) ibs.readNaturalNumber();
        final HuffmanTable<Long> nat3Table = new NaturalNumberHuffmanTable(3);
        final HuffmanTable<Long> nat4Table = new NaturalNumberHuffmanTable(4);

        final HuffmanTable<Character> charHuffmanTable =
                ibs.readHuffmanTable(new CharReader(ibs), new CharHuffmanSymbolDiffReader(ibs, nat4Table));

        final HuffmanTable<Integer> symbolArraysLengthTable =
                ibs.readHuffmanTable(new IntReader(ibs), new IntHuffmanSymbolDiffReader(ibs, nat3Table));

        final int[] idMap = new int[symbolArraysLength];
        for (int index = 0; index < symbolArraysLength; index++) {
            final int length = ibs.readHuffmanSymbol(symbolArraysLengthTable);
            final StringBuilder builder = new StringBuilder();
            for (int pos = 0; pos < length; pos++) {
                builder.append(ibs.readHuffmanSymbol(charHuffmanTable));
            }

            idMap[index] = insertIfNotExists(db, builder.toString());
        }

        return idMap;
    }

    private Conversion[] readConversions(SQLiteDatabase db, InputBitStream ibs, int minValidAlphabet, int maxValidAlphabet, int minSymbolArrayIndex, int maxSymbolArrayIndex, int[] symbolArraysIdMap) throws IOException {
        final int conversionsLength = (int) ibs.readNaturalNumber();
        final Conversion[] conversions = new Conversion[conversionsLength];

        int minSourceAlphabet = minValidAlphabet;
        int minTargetAlphabet = minValidAlphabet;
        for (int i = 0; i < conversionsLength; i++) {
            final int sourceAlphabet = ibs.readRangedNumber(minSourceAlphabet, maxValidAlphabet);

            if (minSourceAlphabet != sourceAlphabet) {
                minTargetAlphabet = minValidAlphabet;
                minSourceAlphabet = sourceAlphabet;
            }

            final int targetAlphabet = ibs.readRangedNumber(minTargetAlphabet, maxValidAlphabet);
            minTargetAlphabet = targetAlphabet + 1;

            final int pairCount = (int) ibs.readNaturalNumber();
            final String[] sources = new String[pairCount];
            final String[] targets = new String[pairCount];
            for (int j = 0; j < pairCount; j++) {
                final int source = symbolArraysIdMap[ibs.readRangedNumber(minSymbolArrayIndex, maxSymbolArrayIndex)];
                final int target = symbolArraysIdMap[ibs.readRangedNumber(minSymbolArrayIndex, maxSymbolArrayIndex)];
                insertConversion(db, sourceAlphabet, targetAlphabet, source, target);

                sources[j] = getSymbolArray(db, source);
                targets[j] = getSymbolArray(db, target);
            }

            conversions[i] = new Conversion(sourceAlphabet, targetAlphabet, sources, targets);
        }

        return conversions;
    }

    private static final int nullCorrelationArray = 0;

    private int[] readAcceptations(SQLiteDatabase db, InputBitStream ibs, int minWord, int maxWord, int minConcept, int maxConcept) throws IOException {
        final int acceptationsLength = (int) ibs.readNaturalNumber();
        final int[] acceptationsIdMap = new int[acceptationsLength];

        for (int i = 0; i < acceptationsLength; i++) {
            final int word = ibs.readRangedNumber(minWord, maxWord);
            final int concept = ibs.readRangedNumber(minConcept, maxConcept);
            acceptationsIdMap[i] = insertAcceptation(db, word, concept, nullCorrelationArray);
        }

        return acceptationsIdMap;
    }

    private static final class StreamedDatabaseConstants {

        /** First alphabet within the database */
        static final int minValidAlphabet = 3;

        /** First concept within the database that is considered to be a valid concept */
        static final int minValidConcept = 1;

        /** First word within the database that is considered to be a valid word */
        static final int minValidWord = 0;
    }

    static final class Language {

        private final String _code;
        private final int _minAlphabet;
        private final int _maxAlphabet;

        Language(String code, int minAlphabet, int alphabetCount) {
            if (code.length() != 2) {
                throw new IllegalArgumentException("Invalid language code");
            }

            if (alphabetCount <= 0) {
                throw new IllegalArgumentException("Alphabet count must be positive");
            }

            _code = code;
            _minAlphabet = minAlphabet;
            _maxAlphabet = minAlphabet + alphabetCount - 1;
        }

        @Override
        public String toString() {
            return "(" + _code + ", " + Integer.toString(_maxAlphabet - _minAlphabet + 1) + ')';
        }
    }

    public static final class Conversion {

        private final int _sourceAlphabet;
        private final int _targetAlphabet;
        private final String[] _sources;
        private final String[] _targets;

        Conversion(int sourceAlphabet, int targetAlphabet, String[] sources, String[] targets) {
            _sourceAlphabet = sourceAlphabet;
            _targetAlphabet = targetAlphabet;
            _sources = sources;
            _targets = targets;
        }
    }

    private static final class JaWordRepr {

        private final int[] _concepts;
        private final int[] _correlationArray;

        JaWordRepr(int[] concepts, int[] correlationArray) {
            _concepts = concepts;
            _correlationArray = correlationArray;
        }

        int[] getConcepts() {
            return _concepts;
        }

        int[] getCorrelationArray() {
            return _correlationArray;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TableNames.acceptations + " (id INTEGER PRIMARY KEY AUTOINCREMENT, word INTEGER, concept INTEGER, correlationArray INTEGER)");
        db.execSQL("CREATE TABLE " + TableNames.conversions + " (id INTEGER PRIMARY KEY AUTOINCREMENT, sourceAlphabet INTEGER, targetAlphabet INTEGER, source INTEGER, target INTEGER)");
        db.execSQL("CREATE TABLE " + TableNames.correlations + " (id INTEGER PRIMARY KEY AUTOINCREMENT, correlationId INTEGER, alphabet INTEGER, symbolArray INTEGER)");
        db.execSQL("CREATE TABLE " + TableNames.correlationArrays + " (id INTEGER PRIMARY KEY AUTOINCREMENT, arrayId INTEGER, arrayPos INTEGER, correlation INTEGER)");
        db.execSQL("CREATE TABLE " + TableNames.symbolArrays + " (id INTEGER PRIMARY KEY AUTOINCREMENT, str TEXT)");

        final InputStream is = _context.getResources().openRawResource(R.raw.basic);
        try {
            is.skip(20);
            final InputBitStream ibs = new InputBitStream(is);
            final int[] symbolArraysIdMap = readSymbolArrays(db, ibs);
            final int maxSymbolArrayIndex = symbolArraysIdMap.length - 1;

            // Read languages and its alphabets
            final int languageCount = (int) ibs.readNaturalNumber();
            final Language[] languages = new Language[languageCount];
            final int minValidAlphabet = StreamedDatabaseConstants.minValidAlphabet;
            int nextMinAlphabet = StreamedDatabaseConstants.minValidAlphabet;
            final HuffmanTable<Long> nat2Table = new NaturalNumberHuffmanTable(2);
            int kanjiAlphabet = -1;
            int kanaAlphabet = -1;

            for (int languageIndex = 0; languageIndex < languageCount; languageIndex++) {
                final int codeSymbolArrayIndex = ibs.readRangedNumber(0, maxSymbolArrayIndex);
                final int alphabetCount = ibs.readHuffmanSymbol(nat2Table).intValue();
                final String code = getSymbolArray(db, symbolArraysIdMap[codeSymbolArrayIndex]);
                languages[languageIndex] = new Language(code, nextMinAlphabet, alphabetCount);

                // In order to inflate kanjiKanaCorrelations we assume that the Japanese language
                // is present and that its first alphabet is the kanji and the second the kana.
                if ("ja".equals(code)) {
                    kanjiAlphabet = nextMinAlphabet;
                    kanaAlphabet = nextMinAlphabet + 1;
                }

                nextMinAlphabet += alphabetCount;
            }

            // Read conversions
            final int maxValidAlphabet = nextMinAlphabet - 1;
            final Conversion[] conversions = readConversions(db, ibs, minValidAlphabet, maxValidAlphabet, 0, maxSymbolArrayIndex, symbolArraysIdMap);

            // Export the amount of words and concepts in order to range integers
            final int maxWord = (int) ibs.readNaturalNumber() - 1;
            final int maxConcept = (int) ibs.readNaturalNumber() - 1;

            // Export acceptations
            final int minValidWord = StreamedDatabaseConstants.minValidWord;
            final int minValidConcept = StreamedDatabaseConstants.minValidConcept;
            final int[] acceptationsIdMap = readAcceptations(db, ibs, minValidWord, maxWord, minValidConcept, maxConcept);

            // Export word representations
            final int wordRepresentationLength = (int) ibs.readNaturalNumber();
            for (int i = 0; i < wordRepresentationLength; i++) {
                final int word = ibs.readRangedNumber(minValidWord, maxWord);
                final int alphabet = ibs.readRangedNumber(minValidAlphabet, maxValidAlphabet);
                final int symbolArray = ibs.readRangedNumber(0, maxSymbolArrayIndex);

                final SparseIntArray correlation = new SparseIntArray();
                correlation.put(alphabet, symbolArraysIdMap[symbolArray]);
                final int correlationId = insertCorrelation(db, correlation);
                final int correlationArrayId = insertCorrelationArray(db, correlationId);
                assignAcceptationCorrelationArray(db, word, correlationArrayId);
            }

            // Export kanji-kana correlations
            final int kanjiKanaCorrelationsLength = (int) ibs.readNaturalNumber();
            if (kanjiKanaCorrelationsLength > 0 && (
                    kanjiAlphabet < minValidAlphabet || kanjiAlphabet > maxValidAlphabet ||
                    kanaAlphabet < minValidAlphabet || kanaAlphabet > maxValidAlphabet)) {
                throw new AssertionError("KanjiAlphabet or KanaAlphabet not set properly");
            }

            final int[] kanjiKanaCorrelationIdMap = new int[kanjiKanaCorrelationsLength];
            for (int i = 0; i < kanjiKanaCorrelationsLength; i++) {
                final SparseIntArray correlation = new SparseIntArray();
                correlation.put(kanjiAlphabet, symbolArraysIdMap[ibs.readRangedNumber(0, maxSymbolArrayIndex)]);
                correlation.put(kanaAlphabet, symbolArraysIdMap[ibs.readRangedNumber(0, maxSymbolArrayIndex)]);
                kanjiKanaCorrelationIdMap[i] = insertCorrelation(db, correlation);
            }

            // Export jaWordCorrelations
            final int jaWordCorrelationsLength = (int) ibs.readNaturalNumber();
            if (jaWordCorrelationsLength > 0) {
                final IntReader intReader = new IntReader(ibs);
                final IntDiffReader intDiffReader = new IntDiffReader(ibs);

                final HuffmanTable<Integer> correlationReprCountHuffmanTable = ibs.readHuffmanTable(intReader, intDiffReader);
                final HuffmanTable<Integer> correlationConceptCountHuffmanTable = ibs.readHuffmanTable(intReader, intDiffReader);
                final HuffmanTable<Integer> correlationVectorLengthHuffmanTable = ibs.readHuffmanTable(intReader, intDiffReader);

                for (int i = 0; i < jaWordCorrelationsLength; i++) {
                    final int wordId = ibs.readRangedNumber(minValidWord, maxWord);
                    final int reprCount = ibs.readHuffmanSymbol(correlationReprCountHuffmanTable);
                    final JaWordRepr[] jaWordReprs = new JaWordRepr[reprCount];

                    for (int j = 0; j < reprCount; j++) {
                        final int conceptSetLength = ibs.readHuffmanSymbol(correlationConceptCountHuffmanTable);
                        int[] concepts = new int[conceptSetLength];
                        for (int k = 0; k < conceptSetLength; k++) {
                            concepts[k] = ibs.readRangedNumber(minValidConcept, maxConcept);
                        }

                        final int correlationArrayLength = ibs.readHuffmanSymbol(correlationVectorLengthHuffmanTable);
                        int[] correlationArray = new int[correlationArrayLength];
                        for (int k = 0; k < correlationArrayLength; k++) {
                            correlationArray[k] = ibs.readRangedNumber(0, kanjiKanaCorrelationsLength - 1);
                        }

                        jaWordReprs[j] = new JaWordRepr(concepts, correlationArray);
                    }

                    for (int j = 0; j < reprCount; j++) {
                        final JaWordRepr jaWordRepr = jaWordReprs[j];
                        final int[] concepts = jaWordRepr.getConcepts();
                        final int conceptCount = concepts.length;
                        for (int k = 0; k < conceptCount; k++) {
                            final int concept = concepts[k];

                            final Pair<Integer, Integer> foundAcc = getAcceptation(db, wordId, concept);
                            if (foundAcc == null) {
                                throw new AssertionError("Acceptation should be already registered");
                            }

                            final int[] correlationArray = jaWordRepr.getCorrelationArray();
                            final int correlationArrayLength = correlationArray.length;

                            /*
                            if (foundAcc.getRight() == nullCorrelationArray) {
                                final int[] dbCorrelations = new int[correlationArrayLength];
                                for (int corrIndex = 0; corrIndex < correlationArrayLength; corrIndex++) {
                                    dbCorrelations[corrIndex] = kanjiKanaCorrelationIdMap[correlationArray[corrIndex]];
                                }

                                final int dbCorrelationArray = insertCorrelationArray(db, dbCorrelations);
                                insertAcceptation(db, wordId, concept, dbCorrelationArray);
                            }
                            else if (correlationArrayLength == 1) {
                                // If reaching this code -> There is a wordRepresentation for this word as well
                                // Only acceptable if the correlationArray has length 1
                                final int dbCorrelationArray = foundAcc.getRight();
                                final int[] dbCorrelations = getCorrelationArray(db, dbCorrelationArray);
                                if (dbCorrelations.length != 1) {
                                    throw new AssertionError("Unmergeable correlation array found");
                                }

                                SparseIntArray dbCorrelation = getCorrelation(db, dbCorrelations[0]);
                                if (dbCorrelation.size() != 1 && dbCorrelation.keyAt(0) != kanaAlphabet) {
                                    throw new AssertionError("Previous correlation includes symbolArrays for unexpected alphabets");
                                }

                                SparseIntArray kanjiKanaCorrelation = getCorrelation(db, kanjiKanaCorrelationIdMap[correlationArray[0]]);
                                if (dbCorrelation.valueAt(0) != kanjiKanaCorrelation.get(kanaAlphabet)) {
                                    throw new AssertionError("Kana representation does not match");
                                }

                                dbCorrelation.put(kanjiAlphabet, correlationArray[0])
                            }
                            else {
                                throw new AssertionError("Acceptation found with unmergeable correlation array");
                            }
                            */

                            // Straight forward, not checking inconsistencies in the database
                            final int[] dbCorrelations = new int[correlationArrayLength];
                            for (int corrIndex = 0; corrIndex < correlationArrayLength; corrIndex++) {
                                dbCorrelations[corrIndex] = kanjiKanaCorrelationIdMap[correlationArray[corrIndex]];
                            }

                            final int dbCorrelationArray = insertCorrelationArray(db, dbCorrelations);
                            assignAcceptationCorrelationArray(db, wordId, concept, dbCorrelationArray);
                        }
                    }
                }
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
