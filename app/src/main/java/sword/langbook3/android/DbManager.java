package sword.langbook3.android;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import sword.bitstream.FunctionWithIOException;
import sword.bitstream.HuffmanTable;
import sword.bitstream.InputBitStream;
import sword.bitstream.NaturalNumberHuffmanTable;
import sword.bitstream.SupplierWithIOException;

class DbManager extends SQLiteOpenHelper {

    static final String DB_NAME = "Langbook";
    private static final int DB_VERSION = 5;

    private static DbManager _instance;

    private final Context _context;
    private Uri _uri;
    private DatabaseImportProgressListener _progressListener;
    private DatabaseImportTask _databaseImportTask;
    private DatabaseImportProgressListener _externalProgressListener;
    private DatabaseImportProgress _lastProgress;

    interface DatabaseImportProgressListener {

        /**
         * Notify the progress of a task
         * @param progress float value expected to be between 0 and 1.
         * @param message Text message describing the current subTask.
         */
        void setProgress(float progress, String message);
    }

    static DbManager getInstance() {
        return _instance;
    }

    static void createInstance(Context context) {
        _instance = new DbManager(context);
    }

    private DbManager(Context context) {
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

    private static class IntHuffmanSymbolReader implements SupplierWithIOException<Integer> {

        private final InputBitStream _ibs;
        private final HuffmanTable<Long> _table;

        IntHuffmanSymbolReader(InputBitStream ibs, HuffmanTable<Long> table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Integer apply() throws IOException {
            return _ibs.readHuffmanSymbol(_table).intValue();
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

    private String getSymbolArray(SQLiteDatabase db, int id) {
        final String whereClause = idColumnName + " = ?";
        final SymbolArraysTable table = Tables.symbolArrays;
        Cursor cursor = db.query(
                table.getName(),
                new String[] {table.getColumnName(table.getStrColumnIndex())},
                whereClause, new String[] { Integer.toString(id) },
                null, null, null, null);

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
        final SymbolArraysTable table = Tables.symbolArrays;
        final String whereClause = table.getColumnName(table.getStrColumnIndex()) + " = ?";
        Cursor cursor = db.query(table.getName(), new String[] {idColumnName}, whereClause,
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

    private Integer findRuledConcept(SQLiteDatabase db, int rule, int concept) {
        final RuledConceptsTable table = Tables.ruledConcepts;
        final String whereClause = table.getColumnName(table.getRuleColumnIndex()) + "=? AND " +
                table.getColumnName(table.getConceptColumnIndex()) + "=?";
        Cursor cursor = db.query(table.getName(), new String[] {idColumnName}, whereClause,
                new String[] { Integer.toString(rule), Integer.toString(concept) }, null, null, null, null);

        if (cursor != null) {
            try {
                final int count = cursor.getCount();
                if (count > 1) {
                    throw new AssertionError("There should not be repeated ruled concepts");
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
        final AcceptationsTable table = Tables.acceptations;

        final String whereClause = new StringBuilder()
                .append(table.getColumnName(table.getWordColumnIndex()))
                .append("=? AND ")
                .append(table.getColumnName(table.getConceptColumnIndex()))
                .append("=? AND ")
                .append(table.getColumnName(table.getCorrelationArrayColumnIndex()))
                .append("=?")
                .toString();

        Cursor cursor = db.query(table.getName(), new String[] {idColumnName}, whereClause,
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
        final AcceptationsTable table = Tables.acceptations;

        final String whereClause = new StringBuilder()
                .append(table.getColumnName(table.getWordColumnIndex()))
                .append("=? AND ")
                .append(table.getColumnName(table.getConceptColumnIndex()))
                .append("=?")
                .toString();

        Cursor cursor = db.query(
                table.getName(),
                new String[] {idColumnName, table.getColumnName(table.getCorrelationArrayColumnIndex())},
                whereClause, new String[] { Integer.toString(word), Integer.toString(concept) },
                null, null, null, null);

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
        final SymbolArraysTable table = Tables.symbolArrays;
        db.execSQL("INSERT INTO " + table.getName() + " (" + table.getColumnName(table.getStrColumnIndex()) + ") VALUES ('" + str + "')");
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

    private int insertRuledConcept(SQLiteDatabase db, int rule, int concept) {
        final int ruledConcept = getMaxConcept(db) + 1;
        final RuledConceptsTable table = Tables.ruledConcepts;
        db.execSQL("INSERT INTO " + table.getName() + " (" + idColumnName + ',' +
                table.getColumnName(table.getRuleColumnIndex()) + ',' +
                table.getColumnName(table.getConceptColumnIndex()) + ") VALUES (" +
                ruledConcept + ',' + rule + ',' + concept + ')');
        final Integer id = findRuledConcept(db, rule, concept);
        if (id == null) {
            throw new AssertionError("A just introduced register should be found");
        }

        return id;
    }

    private int obtainRuledConcept(SQLiteDatabase db, int rule, int concept) {
        final Integer id = findRuledConcept(db, rule, concept);
        if (id != null) {
            return id;
        }

        return insertRuledConcept(db, rule, concept);
    }

    private void insertAlphabet(SQLiteDatabase db, int id, int language) {
        final AlphabetsTable table = Tables.alphabets;
        db.execSQL("INSERT INTO " + table.getName() + " (" + idColumnName + ',' + table.getColumnName(table.getLanguageColumnIndex()) +
                ") VALUES (" + id + ',' + language + ')');
    }

    private void insertLanguage(SQLiteDatabase db, int id, String code, int mainAlphabet) {
        final LanguagesTable table = Tables.languages;
        db.execSQL("INSERT INTO " + table.getName() + " (" + idColumnName + ',' + table.getColumnName(table.getCodeColumnIndex()) +
                ',' + table.getColumnName(table.getMainAlphabetColumnIndex()) + ") VALUES (" + id + ",'" + code + "'," + mainAlphabet + ')');
    }

    private void insertConversion(SQLiteDatabase db, int sourceAlphabet, int targetAlphabet, int source, int target) {
        final ConversionsTable table = Tables.conversions;
        db.execSQL("INSERT INTO " + table.getName() + " (" +
                table.getColumnName(table.getSourceAlphabetColumnIndex()) + ", " +
                table.getColumnName(table.getTargetAlphabetColumnIndex()) + ", " +
                table.getColumnName(table.getSourceColumnIndex()) + ", " +
                table.getColumnName(table.getTargetColumnIndex()) + ") VALUES (" +
                sourceAlphabet + ',' + targetAlphabet + ',' + source + ',' + target + ')');
    }

    private int getMaxWord(SQLiteDatabase db) {
        AcceptationsTable table = Tables.acceptations;
        Cursor cursor = db.rawQuery("SELECT max(" +
                table.getColumnName(table.getWordColumnIndex()) + ") FROM " +
                table.getName(), null);

        if (cursor != null && cursor.getCount() == 1 && cursor.moveToFirst()) {
            try {
                return cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }

        throw new AssertionError("Unable to retrieve maximum wordId");
    }

    private int getMaxConcept(SQLiteDatabase db) {
        AcceptationsTable table = Tables.acceptations;
        Cursor cursor = db.rawQuery("SELECT max(" +
                table.getColumnName(table.getConceptColumnIndex()) + ") FROM " +
                table.getName(), null);

        if (cursor != null && cursor.getCount() == 1 && cursor.moveToFirst()) {
            try {
                return cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }

        throw new AssertionError("Unable to retrieve maximum conceptId");
    }

    private SparseIntArray getCorrelation(SQLiteDatabase db, int id) {
        CorrelationsTable table = Tables.correlations;
        Cursor cursor = db.rawQuery("SELECT " + table.getColumnName(table.getAlphabetColumnIndex()) +
                ',' + table.getColumnName(table.getSymbolArrayColumnIndex()) +
                " FROM " + table.getName() + " WHERE " + table.getColumnName(table.getCorrelationIdColumnIndex()) +
                "=?", new String[] {Integer.toString(id)});

        SparseIntArray result = new SparseIntArray();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        result.put(cursor.getInt(0), cursor.getInt(1));
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private Integer findCorrelation(SQLiteDatabase db, SparseIntArray correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }

        final int firstAlphabet = correlation.keyAt(0);
        final int firstSymbolArray = correlation.valueAt(0);

        CorrelationsTable table = Tables.correlations;
        Cursor cursor = db.rawQuery("SELECT " + table.getColumnName(table.getCorrelationIdColumnIndex()) +
                " FROM " + table.getName() + " WHERE " + table.getColumnName(table.getAlphabetColumnIndex()) +
                "=? AND " + table.getColumnName(table.getSymbolArrayColumnIndex()) +
                "=?", new String[] {Integer.toString(firstAlphabet), Integer.toString(firstSymbolArray)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        final int correlationId = cursor.getInt(0);
                        final SparseIntArray corr = getCorrelation(db, correlationId);
                        if (EqualUtils.checkEqual(corr, correlation)) {
                            return correlationId;
                        }
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        return null;
    }

    private int insertCorrelation(SQLiteDatabase db, SparseIntArray correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }

        final CorrelationsTable table = Tables.correlations;
        final String correlationIdColumnName = table.getColumnName(table.getCorrelationIdColumnIndex());
        final String alphabetColumnName = table.getColumnName(table.getAlphabetColumnIndex());
        final String symbolArrayColumnName = table.getColumnName(table.getSymbolArrayColumnIndex());

        Cursor cursor = db.rawQuery("SELECT max(" +
                correlationIdColumnName + ") FROM " +
                table.getName(), null);

        if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()) {
            throw new AssertionError("Unable to retrieve maximum correlationId");
        }

        try {
            final int newCorrelationId = cursor.getInt(0) + 1;
            final int mapLength = correlation.size();
            for (int i = 0; i < mapLength; i++) {
                final int alphabet = correlation.keyAt(i);
                final int symbolArray = correlation.valueAt(i);
                db.execSQL("INSERT INTO " + table.getName() + " (" +
                        correlationIdColumnName + ", " +
                        alphabetColumnName + ", " +
                        symbolArrayColumnName + ") VALUES (" +
                        newCorrelationId + ',' + alphabet + ',' + symbolArray + ')');
            }

            return newCorrelationId;
        }
        finally {
            cursor.close();
        }
    }

    private int obtainCorrelation(SQLiteDatabase db, SparseIntArray correlation) {
        final Integer id = findCorrelation(db, correlation);
        if (id == null) {
            return insertCorrelation(db, correlation);
        }

        return id;
    }

    private int insertCorrelationArray(SQLiteDatabase db, int... correlation) {
        final CorrelationArraysTable table = Tables.correlationArrays;
        final String arrayIdColumnName = table.getColumnName(table.getArrayIdColumnIndex());
        Cursor cursor = db.rawQuery("SELECT max(" + arrayIdColumnName + ") FROM " +
                table.getName(), null);

        if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()) {
            throw new AssertionError("Unable to retrieve maximum arrayId");
        }

        try {
            final int newArrayId = cursor.getInt(0) + 1;
            final int arrayLength = correlation.length;
            for (int i = 0; i < arrayLength; i++) {
                final int corr = correlation[i];
                db.execSQL("INSERT INTO " + table.getName() + " (" + arrayIdColumnName + ", " +
                        table.getColumnName(table.getArrayPositionColumnIndex()) + ", " +
                        table.getColumnName(table.getCorrelationColumnIndex()) + ") VALUES (" +
                        newArrayId + ',' + i + ',' + corr + ')');
            }

            return newArrayId;
        }
        finally {
            cursor.close();
        }
    }

    private int insertAcceptation(SQLiteDatabase db, int word, int concept, int correlationArray) {
        final AcceptationsTable table = Tables.acceptations;
        db.execSQL("INSERT INTO " + table.getName() + " (" +
                table.getColumnName(table.getWordColumnIndex()) + ", " +
                table.getColumnName(table.getConceptColumnIndex()) + ", " +
                table.getColumnName(table.getCorrelationArrayColumnIndex()) + ") VALUES (" +
                word + ',' + concept + ',' + correlationArray + ')');

        final Integer id = getAcceptation(db, word, concept, correlationArray);
        if (id == null) {
            throw new AssertionError("A just introduced register should be found");
        }

        return id;
    }

    private Integer getAgentId(SQLiteDatabase db, int targetBunch, int sourceBunchSetId,
                             int diffBunchSetId, int matcherId, int adderId, int rule, int flags) {
        final AgentsTable table = Tables.agents;

        final String whereClause = new StringBuilder()
                .append(table.getColumnName(table.getTargetBunchColumnIndex()))
                .append("=? AND ")
                .append(table.getColumnName(table.getSourceBunchSetColumnIndex()))
                .append("=? AND ")
                .append(table.getColumnName(table.getDiffBunchSetColumnIndex()))
                .append("=? AND ")
                .append(table.getColumnName(table.getMatcherColumnIndex()))
                .append("=? AND ")
                .append(table.getColumnName(table.getAdderColumnIndex()))
                .append("=? AND ")
                .append(table.getColumnName(table.getRuleColumnIndex()))
                .append("=? AND ")
                .append(table.getColumnName(table.getFlagsColumnIndex()))
                .append("=?")
                .toString();

        Cursor cursor = db.query(table.getName(), new String[] {idColumnName}, whereClause, new String[] {
                Integer.toString(targetBunch),
                Integer.toString(sourceBunchSetId),
                Integer.toString(diffBunchSetId),
                Integer.toString(matcherId),
                Integer.toString(adderId),
                Integer.toString(rule),
                Integer.toString(flags)
        }, null, null, null, null);

        if (cursor != null) {
            try {
                final int count = cursor.getCount();
                if (count > 1) {
                    throw new AssertionError("There should not be repeated agents");
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

    private int insertAgent(SQLiteDatabase db, int targetBunch, int sourceBunchSetId,
                             int diffBunchSetId, int matcherId, int adderId, int rule, int flags) {
        final AgentsTable table = Tables.agents;
        db.execSQL("INSERT INTO " + table.getName() + " (" +
                table.getColumnName(table.getTargetBunchColumnIndex()) + ", " +
                table.getColumnName(table.getSourceBunchSetColumnIndex()) + ", " +
                table.getColumnName(table.getDiffBunchSetColumnIndex()) + ", " +
                table.getColumnName(table.getMatcherColumnIndex()) + ", " +
                table.getColumnName(table.getAdderColumnIndex()) + ", " +
                table.getColumnName(table.getRuleColumnIndex()) + ", " +
                table.getColumnName(table.getFlagsColumnIndex()) + ") VALUES (" +
                targetBunch + ',' + sourceBunchSetId + ',' + diffBunchSetId + ',' +
                matcherId + ',' + adderId + ',' + rule + ',' + flags + ')');

        final Integer id = getAgentId(db, targetBunch, sourceBunchSetId, diffBunchSetId, matcherId, adderId, rule, flags);
        if (id == null) {
            throw new AssertionError("A just introduced register should be found");
        }

        return id;
    }

    private void insertStringQuery(
            SQLiteDatabase db, String str, String mainStr, int mainAcceptation,
            int dynAcceptation, int strAlphabet) {
        final StringQueriesTable table = Tables.stringQueries;
        db.execSQL("INSERT INTO " + table.getName() + " (" +
                table.getColumnName(table.getStringColumnIndex()) + ", " +
                table.getColumnName(table.getMainStringColumnIndex()) + ", " +
                table.getColumnName(table.getMainAcceptationColumnIndex()) + ", " +
                table.getColumnName(table.getDynamicAcceptationColumnIndex()) + ", " +
                table.getColumnName(table.getStringAlphabetColumnIndex()) + ") VALUES ('" +
                str + "','" + mainStr + "'," + mainAcceptation + ',' +
                dynAcceptation + ',' + strAlphabet + ')');
    }

    private void insertBunchAcceptation(SQLiteDatabase db, int bunch, int acceptation, int agentSet) {
        final BunchAcceptationsTable table = Tables.bunchAcceptations;
        db.execSQL("INSERT INTO " + table.getName() + " (" +
                table.getColumnName(table.getBunchColumnIndex()) + ", " +
                table.getColumnName(table.getAcceptationColumnIndex()) + ", " +
                table.getColumnName(table.getAgentSetColumnIndex()) + ") VALUES (" +
                bunch + ',' + acceptation + ',' + agentSet + ')');
    }

    private void insertBunchConcept(SQLiteDatabase db, int bunch, int concept) {
        final BunchConceptsTable table = Tables.bunchConcepts;
        db.execSQL("INSERT INTO " + table.getName() + " (" +
                table.getColumnName(table.getBunchColumnIndex()) + ", " +
                table.getColumnName(table.getConceptColumnIndex()) + ") VALUES (" +
                bunch + ',' + concept + ')');
    }

    private int insertAgentSet(SQLiteDatabase db, Set<Integer> agents) {
        final AgentSetsTable table = Tables.agentSets;

        if (agents.isEmpty()) {
            return table.nullReference();
        }
        else {
            final String setIdColumnName = table.getColumnName(table.getSetIdColumnIndex());
            Cursor cursor = db.rawQuery("SELECT max(" + setIdColumnName + ") FROM " +
                    table.getName(), null);

            if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()) {
                throw new AssertionError("Unable to retrieve maximum setId");
            }

            final int setId = cursor.getInt(0) + 1;
            cursor.close();

            for (int agent : agents) {
                db.execSQL("INSERT INTO " + table.getName() + " (" +
                        table.getColumnName(table.getSetIdColumnIndex()) + ", " +
                        table.getColumnName(table.getAgentColumnIndex()) + ") VALUES (" +
                        setId + ',' + agent + ')');
            }

            return setId;
        }
    }

    private int insertBunchSet(SQLiteDatabase db, Set<Integer> bunches) {
        final BunchSetsTable table = Tables.bunchSets;

        if (bunches.isEmpty()) {
            return table.nullReference();
        }
        else {
            final String setIdColumnName = table.getColumnName(table.getSetIdColumnIndex());
            Cursor cursor = db.rawQuery("SELECT max(" + setIdColumnName + ") FROM " +
                    table.getName(), null);

            if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()) {
                throw new AssertionError("Unable to retrieve maximum setId");
            }

            final int setId = cursor.getInt(0) + 1;
            cursor.close();

            for (int bunch : bunches) {
                db.execSQL("INSERT INTO " + table.getName() + " (" +
                        table.getColumnName(table.getSetIdColumnIndex()) + ", " +
                        table.getColumnName(table.getBunchColumnIndex()) + ") VALUES (" +
                        setId + ',' + bunch + ')');
            }

            return setId;
        }
    }

    private void assignAcceptationCorrelationArray(SQLiteDatabase db, int word, int correlationArrayId) {
        final AcceptationsTable table = Tables.acceptations;
        db.execSQL("UPDATE " + table.getName() + " SET " +
                table.getColumnName(table.getCorrelationArrayColumnIndex()) + '=' +
                correlationArrayId + " WHERE " +
                table.getColumnName(table.getWordColumnIndex()) + '=' + word);
    }

    private void assignAcceptationCorrelationArray(SQLiteDatabase db, int word, int concept, int correlationArrayId) {
        final AcceptationsTable table = Tables.acceptations;
        db.execSQL("UPDATE " + table.getName() + " SET " +
                table.getColumnName(table.getCorrelationArrayColumnIndex()) + '=' +
                correlationArrayId + " WHERE " +
                table.getColumnName(table.getWordColumnIndex()) + '=' + word +
                " AND " + table.getColumnName(table.getConceptColumnIndex()) + '=' + concept);
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

    private void readBunchConcepts(SQLiteDatabase db, InputBitStream ibs, int minValidConcept, int maxConcept) throws IOException {
        final int bunchConceptsLength = (int) ibs.readNaturalNumber();
        final HuffmanTable<Integer> bunchConceptsLengthTable = (bunchConceptsLength > 0)?
                ibs.readHuffmanTable(new IntReader(ibs), new IntDiffReader(ibs)) : null;

        for (int i = 0; i < bunchConceptsLength; i++) {
            final int bunch = ibs.readRangedNumber(minValidConcept, maxConcept);
            final Set<Integer> concepts = ibs.readRangedNumberSet(bunchConceptsLengthTable, minValidConcept, maxConcept);
            for (int concept : concepts) {
                insertBunchConcept(db, bunch, concept);
            }
        }
    }

    private void readBunchAcceptations(SQLiteDatabase db, InputBitStream ibs, int minValidConcept, int maxConcept, int[] acceptationsIdMap) throws IOException {
        final int bunchAcceptationsLength = (int) ibs.readNaturalNumber();
        final HuffmanTable<Integer> bunchAcceptationsLengthTable = (bunchAcceptationsLength > 0)?
                ibs.readHuffmanTable(new IntReader(ibs), new IntDiffReader(ibs)) : null;

        final int maxValidAcceptation = acceptationsIdMap.length - 1;
        final int nullAgentSet = Tables.agentSets.nullReference();
        for (int i = 0; i < bunchAcceptationsLength; i++) {
            final int bunch = ibs.readRangedNumber(minValidConcept, maxConcept);
            final Set<Integer> acceptations = ibs.readRangedNumberSet(bunchAcceptationsLengthTable, 0, maxValidAcceptation);
            for (int acceptation : acceptations) {
                insertBunchAcceptation(db, bunch, acceptationsIdMap[acceptation], nullAgentSet);
            }
        }
    }

    private static class Agent {

        private final int _target;
        private final Set<Integer> _sources;
        private final Set<Integer> _diff;
        private final SparseIntArray _matcher;
        private final SparseIntArray _adder;
        private final int _rule;
        private final int _flags;

        Agent(int target, Set<Integer> sources, Set<Integer> diff,
                SparseIntArray matcher, SparseIntArray adder, int rule, int flags) {
            _target = target;
            _sources = sources;
            _diff = diff;
            _matcher = matcher;
            _adder = adder;
            _rule = rule;
            _flags = flags;
        }

        boolean dependsOn(Agent agent) {
            final int target = agent._target;
            return _sources.contains(target) || _diff.contains(target);
        }
    }

    private SparseArray<Agent> readAgents(
            SQLiteDatabase db, InputBitStream ibs, int maxConcept,
            int minValidAlphabet, int maxValidAlphabet, int[] symbolArrayIdMap) throws IOException {

        final int agentsLength = (int) ibs.readNaturalNumber();
        final SparseArray<Agent> result = new SparseArray<>(agentsLength);

        if (agentsLength > 0) {
            final HuffmanTable<Long> nat3Table = new NaturalNumberHuffmanTable(3);
            final IntHuffmanSymbolReader intHuffmanSymbolReader = new IntHuffmanSymbolReader(ibs, nat3Table);
            final HuffmanTable<Integer> sourceSetLengthTable = ibs.readHuffmanTable(intHuffmanSymbolReader, null);
            final HuffmanTable<Integer> matcherSetLengthTable = ibs.readHuffmanTable(intHuffmanSymbolReader, null);

            int lastTarget = StreamedDatabaseConstants.nullBunchId;
            int minSource = StreamedDatabaseConstants.minValidConcept;
            for (int i = 0; i < agentsLength; i++) {
                final int targetBunch = ibs.readRangedNumber(lastTarget, maxConcept);

                if (targetBunch != lastTarget) {
                    minSource = StreamedDatabaseConstants.minValidConcept;
                }

                final Set<Integer> sourceSet = ibs.readRangedNumberSet(sourceSetLengthTable, minSource, maxConcept);

                if (!sourceSet.isEmpty()) {
                    int min = Integer.MAX_VALUE;
                    for (int value : sourceSet) {
                        if (value < min) {
                            min = value;
                        }
                    }
                    minSource = min;
                }

                final SparseIntArray matcher = readCorrelationMap(ibs, matcherSetLengthTable,
                        minValidAlphabet, maxValidAlphabet, symbolArrayIdMap);
                final SparseIntArray adder = readCorrelationMap(ibs, matcherSetLengthTable,
                        minValidAlphabet, maxValidAlphabet, symbolArrayIdMap);

                final int rule = (adder.size() > 0)?
                        ibs.readRangedNumber(StreamedDatabaseConstants.minValidConcept, maxConcept) :
                        StreamedDatabaseConstants.nullRuleId;

                final boolean fromStart = (matcher.size() > 0 || adder.size() > 0) && ibs.readBoolean();
                final int flags = fromStart? 1 : 0;

                final int sourceBunchSetId = insertBunchSet(db, sourceSet);
                final int diffBunchSetId = 0;

                final int matcherId = obtainCorrelation(db, matcher);
                final int adderId = obtainCorrelation(db, adder);
                if (rule != StreamedDatabaseConstants.nullRuleId && matcherId == adderId) {
                    throw new AssertionError("When rule is provided, modification is expected, but matcher and adder are the same");
                }

                final int agentId = insertAgent(db, targetBunch, sourceBunchSetId, diffBunchSetId, matcherId, adderId, rule, flags);

                final Set<Integer> diffSet = Collections.emptySet();
                result.put(agentId, new Agent(targetBunch, sourceSet, diffSet, matcher, adder, rule, flags));

                lastTarget = targetBunch;
            }
        }

        return result;
    }

    private static final class StreamedDatabaseConstants {

        /** Reserved for empty correlations */
        static final int nullCorrelationId = 0;

        /** Reserved for agents for null references */
        static final int nullBunchId = 0;

        /** Reserved for agents for null references */
        static final int nullRuleId = 0;

        /** First alphabet within the database */
        static final int minValidAlphabet = 3;

        /** First concept within the database that is considered to be a valid concept */
        static final int minValidConcept = 1;

        /** First word within the database that is considered to be a valid word */
        static final int minValidWord = 0;
    }

    private static final class Language {

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

        boolean containsAlphabet(int alphabet) {
            return alphabet >= _minAlphabet && alphabet <= _maxAlphabet;
        }

        String getCode() {
            return _code;
        }

        int getMainAlphabet() {
            // For now, it is considered the main alphabet the first of them.
            return _minAlphabet;
        }

        @Override
        public String toString() {
            return "(" + _code + ", " + Integer.toString(_maxAlphabet - _minAlphabet + 1) + ')';
        }
    }

    private static final class Conversion {

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

        int getSourceAlphabet() {
            return _sourceAlphabet;
        }

        int getTargetAlphabet() {
            return _targetAlphabet;
        }

        private String convert(String text, int index, String acc) {
            final int length = _sources.length;
            if (index == text.length()) {
                return acc;
            }

            for (int i = 0; i < length; i++) {
                if (text.startsWith(_sources[i], index)) {
                    return convert(text, index + _sources[i].length(), acc + _targets[i]);
                }
            }

            return null;
        }

        String convert(String text) {
            return (text != null)? convert(text, 0, "") : null;
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

    static class DbTable {

        private final String _name;
        private final String[] _columnNames;
        private final int _intColumnCount; // This already count the identifier

        DbTable(String name, String[] intColumns, String[] textColumns) {
            final int intColumnCount = (intColumns != null)? intColumns.length : 0;
            final int textColumnCount = (textColumns != null)? textColumns.length : 0;

            String[] columnNames = new String[intColumnCount + textColumnCount + 1];
            columnNames[0] = idColumnName;

            if (intColumnCount > 0) {
                System.arraycopy(intColumns, 0, columnNames, 1, intColumnCount);
            }

            if (textColumnCount > 0) {
                System.arraycopy(textColumns, 0, columnNames, intColumnCount + 1, textColumnCount);
            }

            _name = name;
            _columnNames = columnNames;
            _intColumnCount = intColumnCount + 1;
        }

        String getName() {
            return _name;
        }

        int getColumnCount() {
            return _columnNames.length;
        }

        String getColumnName(int index) {
            return _columnNames[index];
        }

        boolean isTextColumn(int index) {
            return index >= _intColumnCount;
        }
    }

    static final class AcceptationsTable extends DbTable {

        AcceptationsTable() {
            super("Acceptations", new String[] {"word", "concept", "correlationArray"}, null);
        }

        int getWordColumnIndex() {
            return 1;
        }

        int getConceptColumnIndex() {
            return 2;
        }

        int getCorrelationArrayColumnIndex() {
            return 3;
        }
    }

    static final class AgentsTable extends DbTable {

        AgentsTable() {
            super("Agents", new String[] {"target", "sourceSet", "diffSet", "matcher", "adder", "rule", "flags"}, null);
        }

        int getTargetBunchColumnIndex() {
            return 1;
        }

        int getSourceBunchSetColumnIndex() {
            return 2;
        }

        int getDiffBunchSetColumnIndex() {
            return 3;
        }

        int getMatcherColumnIndex() {
            return 4;
        }

        int getAdderColumnIndex() {
            return 5;
        }

        int getRuleColumnIndex() {
            return 6;
        }

        int getFlagsColumnIndex() {
            return 7;
        }

        int nullReference() {
            return 0;
        }
    }

    static final class AgentSetsTable extends DbTable {

        AgentSetsTable() {
            super("AgentSets", new String[] {"setId", "agent"}, null);
        }

        int getSetIdColumnIndex() {
            return 1;
        }

        int getAgentColumnIndex() {
            return 2;
        }

        int nullReference() {
            return 0;
        }
    }

    static final class AlphabetsTable extends DbTable {

        AlphabetsTable() {
            super("Alphabets", new String[] {"language"}, null);
        }

        int getLanguageColumnIndex() {
            return 1;
        }
    }

    static final class BunchAcceptationsTable extends DbTable {

        BunchAcceptationsTable() {
            super("BunchAcceptations", new String[] {"bunch", "acceptation", "agentSet"}, null);
        }

        int getBunchColumnIndex() {
            return 1;
        }

        int getAcceptationColumnIndex() {
            return 2;
        }

        int getAgentSetColumnIndex() {
            return 3;
        }
    }

    static final class BunchConceptsTable extends DbTable {

        BunchConceptsTable() {
            super("BunchConcepts", new String[] {"bunch", "concept"}, null);
        }

        int getBunchColumnIndex() {
            return 1;
        }

        int getConceptColumnIndex() {
            return 2;
        }
    }

    static final class BunchSetsTable extends DbTable {

        BunchSetsTable() {
            super("BunchSets", new String[] {"setId", "bunch"}, null);
        }

        int getSetIdColumnIndex() {
            return 1;
        }

        int getBunchColumnIndex() {
            return 2;
        }

        int nullReference() {
            return 0;
        }
    }

    static final class ConversionsTable extends DbTable {

        ConversionsTable() {
            super("Conversions", new String[] {"sourceAlphabet", "targetAlphabet", "source", "target"}, null);
        }

        int getSourceAlphabetColumnIndex() {
            return 1;
        }

        int getTargetAlphabetColumnIndex() {
            return 2;
        }

        int getSourceColumnIndex() {
            return 3;
        }

        int getTargetColumnIndex() {
            return 4;
        }
    }

    static final class CorrelationsTable extends DbTable {

        CorrelationsTable() {
            super("Correlations", new String[] {"correlationId", "alphabet", "symbolArray"}, null);
        }

        int getCorrelationIdColumnIndex() {
            return 1;
        }

        int getAlphabetColumnIndex() {
            return 2;
        }

        int getSymbolArrayColumnIndex() {
            return 3;
        }
    }

    static final class CorrelationArraysTable extends DbTable {

        CorrelationArraysTable() {
            super("CorrelationArrays", new String[] {"arrayId", "arrayPos", "correlation"}, null);
        }

        int getArrayIdColumnIndex() {
            return 1;
        }

        int getArrayPositionColumnIndex() {
            return 2;
        }

        int getCorrelationColumnIndex() {
            return 3;
        }
    }

    static final class LanguagesTable extends DbTable {

        LanguagesTable() {
            super("Languages", new String[] {"mainAlphabet"}, new String[] {"code"});
        }

        int getMainAlphabetColumnIndex() {
            return 1;
        }

        int getCodeColumnIndex() {
            return 2;
        }
    }

    static final class RuledConceptsTable extends DbTable {

        RuledConceptsTable() {
            super("RuledConcepts", new String[] {"rule", "concept"}, null);
        }

        int getRuleColumnIndex() {
            return 1;
        }

        int getConceptColumnIndex() {
            return 2;
        }
    }

    static final class StringQueriesTable extends DbTable {

        StringQueriesTable() {
            super("StringQueryTable", new String[] {"mainAcceptation", "dynamicAcceptation", "strAlphabet"}, new String[] {"str", "mainStr"});
        }

        int getMainAcceptationColumnIndex() {
            return 1;
        }

        int getDynamicAcceptationColumnIndex() {
            return 2;
        }

        int getStringAlphabetColumnIndex() {
            return 3;
        }

        int getStringColumnIndex() {
            return 4;
        }

        int getMainStringColumnIndex() {
            return 5;
        }
    }

    static final class SymbolArraysTable extends DbTable {

        SymbolArraysTable() {
            super("SymbolArrays", null, new String[] {"str"});
        }

        int getStrColumnIndex() {
            return 1;
        }
    }

    static final class Tables {
        static final AcceptationsTable acceptations = new AcceptationsTable();
        static final AgentsTable agents = new AgentsTable();
        static final AgentSetsTable agentSets = new AgentSetsTable();
        static final AlphabetsTable alphabets = new AlphabetsTable();
        static final BunchAcceptationsTable bunchAcceptations = new BunchAcceptationsTable();
        static final BunchConceptsTable bunchConcepts = new BunchConceptsTable();
        static final BunchSetsTable bunchSets = new BunchSetsTable();
        static final ConversionsTable conversions = new ConversionsTable();
        static final CorrelationsTable correlations = new CorrelationsTable();
        static final CorrelationArraysTable correlationArrays = new CorrelationArraysTable();
        static final LanguagesTable languages = new LanguagesTable();
        static final RuledConceptsTable ruledConcepts = new RuledConceptsTable();
        static final StringQueriesTable stringQueries = new StringQueriesTable();
        static final SymbolArraysTable symbolArrays = new SymbolArraysTable();
    }

    static final String idColumnName = "id";

    private static final DbTable[] dbTables = new DbTable[14];
    static {
        dbTables[0] = Tables.acceptations;
        dbTables[1] = Tables.agents;
        dbTables[2] = Tables.agentSets;
        dbTables[3] = Tables.alphabets;
        dbTables[4] = Tables.bunchAcceptations;
        dbTables[5] = Tables.bunchConcepts;
        dbTables[6] = Tables.bunchSets;
        dbTables[7] = Tables.conversions;
        dbTables[8] = Tables.correlations;
        dbTables[9] = Tables.correlationArrays;
        dbTables[10] = Tables.languages;
        dbTables[11] = Tables.ruledConcepts;
        dbTables[12] = Tables.stringQueries;
        dbTables[13] = Tables.symbolArrays;
    }

    private void createTables(SQLiteDatabase db) {
        for (DbTable table : dbTables) {
            StringBuilder builder = new StringBuilder();
            builder.append("CREATE TABLE ")
                    .append(table.getName())
                    .append(" (")
                    .append(idColumnName)
                    .append(" INTEGER PRIMARY KEY AUTOINCREMENT");

            final int columnCount = table.getColumnCount();
            for (int i = 1; i < columnCount; i++) {
                builder.append(", ")
                        .append(table.getColumnName(i))
                        .append(table.isTextColumn(i)? " TEXT" : " INTEGER");
            }
            builder.append(')');

            db.execSQL(builder.toString());
        }
    }

    private SparseIntArray readCorrelationMap(
            InputBitStream ibs, HuffmanTable<Integer> matcherSetLengthTable,
            int minAlphabet, int maxAlphabet, int[] symbolArraysIdMap) throws IOException {

        final int maxSymbolArray = symbolArraysIdMap.length - 1;
        final int mapLength = ibs.readHuffmanSymbol(matcherSetLengthTable);
        final SparseIntArray result = new SparseIntArray();
        for (int i = 0; i < mapLength; i++) {
            final int alphabet = ibs.readRangedNumber(minAlphabet, maxAlphabet);
            final int symbolArrayIndex = ibs.readRangedNumber(0, maxSymbolArray);
            minAlphabet = alphabet + 1;
            result.put(alphabet, symbolArraysIdMap[symbolArrayIndex]);
        }

        return result;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final Uri uri = (_uri != null)? _uri : Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + _context.getPackageName() + '/' + R.raw.basic);

        _uri = null;

        importDatabase(db, uri);
    }

    private void setProgress(float progress, String message) {
        final DatabaseImportProgressListener listener = _progressListener;
        if (listener != null) {
            listener.setProgress(progress, message);
        }
    }

    private boolean importDatabase(SQLiteDatabase db, Uri uri) {
        createTables(db);

        try {
            final InputStream is = _context.getContentResolver().openInputStream(uri);
            if (is != null) {
                try {
                    is.skip(20);

                    setProgress(0, "Reading symbolArrays");
                    final InputBitStream ibs = new InputBitStream(is);
                    final int[] symbolArraysIdMap = readSymbolArrays(db, ibs);
                    final int minSymbolArrayIndex = 0;
                    final int maxSymbolArrayIndex = symbolArraysIdMap.length - 1;

                    // Read languages and its alphabets
                    setProgress(0.03f, "Reading languages and its alphabets");
                    final int languageCount = (int) ibs.readNaturalNumber();
                    final Language[] languages = new Language[languageCount];
                    final int minValidAlphabet = StreamedDatabaseConstants.minValidAlphabet;
                    int nextMinAlphabet = StreamedDatabaseConstants.minValidAlphabet;
                    final HuffmanTable<Long> nat2Table = new NaturalNumberHuffmanTable(2);
                    int kanjiAlphabet = -1;
                    int kanaAlphabet = -1;

                    for (int languageIndex = 0; languageIndex < languageCount; languageIndex++) {
                        final int codeSymbolArrayIndex = ibs.readRangedNumber(minSymbolArrayIndex, maxSymbolArrayIndex);
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

                    final int maxValidAlphabet = nextMinAlphabet - 1;
                    final int minLanguage = nextMinAlphabet;
                    final int maxLanguage = minLanguage + languages.length - 1;

                    for (int i = minValidAlphabet; i <= maxValidAlphabet; i++) {
                        for (int j = 0; j < languageCount; j++) {
                            Language lang = languages[j];
                            if (lang.containsAlphabet(i)) {
                                insertAlphabet(db, i, minLanguage + j);
                                break;
                            }
                        }
                    }

                    for (int i = 0; i < languageCount; i++) {
                        final Language lang = languages[i];
                        insertLanguage(db, minLanguage + i, lang.getCode(), lang.getMainAlphabet());
                    }

                    // Read conversions
                    setProgress(0.06f, "Reading conversions");
                    final Conversion[] conversions = readConversions(db, ibs, minValidAlphabet, maxValidAlphabet, 0,
                            maxSymbolArrayIndex, symbolArraysIdMap);

                    // Export the amount of words and concepts in order to range integers
                    final int maxWord = (int) ibs.readNaturalNumber() - 1;
                    final int maxConcept = (int) ibs.readNaturalNumber() - 1;

                    // Export acceptations
                    setProgress(0.09f, "Reading acceptations");
                    final int minValidWord = StreamedDatabaseConstants.minValidWord;
                    final int minValidConcept = StreamedDatabaseConstants.minValidConcept;
                    final int[] acceptationsIdMap = readAcceptations(db, ibs, minValidWord, maxWord, minValidConcept,
                            maxConcept);

                    final int minValidAcceptation = 0;
                    final int maxValidAcceptation = acceptationsIdMap.length - 1;

                    // Export word representations
                    setProgress(0.12f, "Reading representations");
                    final int wordRepresentationLength = (int) ibs.readNaturalNumber();
                    for (int i = 0; i < wordRepresentationLength; i++) {
                        final int word = ibs.readRangedNumber(minValidWord, maxWord);
                        final int alphabet = ibs.readRangedNumber(minValidAlphabet, maxValidAlphabet);
                        final int symbolArray = ibs.readRangedNumber(0, maxSymbolArrayIndex);

                        final SparseIntArray correlation = new SparseIntArray();
                        correlation.put(alphabet, symbolArraysIdMap[symbolArray]);
                        final int correlationId = obtainCorrelation(db, correlation);
                        final int correlationArrayId = insertCorrelationArray(db, correlationId);
                        assignAcceptationCorrelationArray(db, word, correlationArrayId);
                    }

                    // Export kanji-kana correlations
                    setProgress(0.15f, "Reading kanji-kana correlations");
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
                        kanjiKanaCorrelationIdMap[i] = obtainCorrelation(db, correlation);
                    }

                    // Export jaWordCorrelations
                    setProgress(0.18f, "Reading Japanese word correlations");
                    final int jaWordCorrelationsLength = (int) ibs.readNaturalNumber();
                    if (jaWordCorrelationsLength > 0) {
                        final IntReader intReader = new IntReader(ibs);
                        final IntDiffReader intDiffReader = new IntDiffReader(ibs);

                        final HuffmanTable<Integer> correlationReprCountHuffmanTable = ibs
                                .readHuffmanTable(intReader, intDiffReader);
                        final HuffmanTable<Integer> correlationConceptCountHuffmanTable = ibs
                                .readHuffmanTable(intReader, intDiffReader);
                        final HuffmanTable<Integer> correlationVectorLengthHuffmanTable = ibs
                                .readHuffmanTable(intReader, intDiffReader);

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

                                final int correlationArrayLength = ibs
                                        .readHuffmanSymbol(correlationVectorLengthHuffmanTable);
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

                    // Export bunchConcepts
                    setProgress(0.21f, "Reading bunch concepts");
                    readBunchConcepts(db, ibs, minValidConcept, maxConcept);

                    // Export bunchAcceptations
                    setProgress(0.24f, "Reading bunch acceptations");
                    readBunchAcceptations(db, ibs, minValidConcept, maxConcept, acceptationsIdMap);

                    // Export agents
                    setProgress(0.27f, "Reading agents");
                    SparseArray<Agent> agents = readAgents(db, ibs, maxConcept, minValidAlphabet, maxValidAlphabet,
                            symbolArraysIdMap);

                    setProgress(0.3f, "Indexing strings");
                    fillSearchQueryTable(db);

                    setProgress(0.4f, "Running agents");
                    runAgents(db, agents);

                    setProgress(0.8f, "Applying conversions");
                    applyConversions(db, conversions);
                    return true;
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
        }
        catch (FileNotFoundException e) {
            // Nothing can be done
        }

        return false;
    }

    private int[] sortAgents(SparseArray<Agent> agents) {
        final int agentCount = agents.size();
        int[] ids = new int[agentCount];
        if (agentCount == 0) {
            return ids;
        }

        Agent[] result = new Agent[agentCount];

        for (int i = 0; i < agentCount; i++) {
            ids[i] = agents.keyAt(i);
            result[i] = agents.valueAt(i);
        }

        int index = agentCount;
        do {
            final Agent agent = result[--index];

            int firstDependency = -1;
            for (int i = 0; i < index; i++) {
                if (result[i].dependsOn(agent)) {
                    firstDependency = i;
                }
            }

            if (firstDependency >= 0) {
                int id = ids[firstDependency];
                ids[firstDependency] = ids[index];
                ids[index] = id;

                Agent temp = result[firstDependency];
                result[firstDependency] = result[index];
                result[index++] = temp;
            }
        } while(index > 0);

        return ids;
    }

    private SparseArray<String> readCorrelation(SQLiteDatabase db, int agentId, int agentColumn) {
        AgentsTable agents = Tables.agents;
        CorrelationsTable correlations = Tables.correlations;
        SymbolArraysTable symbolArrays = Tables.symbolArrays;

        Cursor cursor = db.rawQuery("SELECT J1." + correlations.getColumnName(correlations.getAlphabetColumnIndex()) + ",J2." + symbolArrays.getColumnName(symbolArrays.getStrColumnIndex()) +
                " FROM " + agents.getName() + " AS J0" +
                " JOIN " + correlations.getName() + " AS J1 ON J0." + agents.getColumnName(agentColumn) + "=J1." + correlations.getColumnName(correlations.getCorrelationIdColumnIndex()) +
                " JOIN " + symbolArrays.getName() + " AS J2 ON J1." + correlations.getColumnName(correlations.getSymbolArrayColumnIndex()) + "=J2." + idColumnName +
                " WHERE J0." + idColumnName + "=?", new String[] { Integer.toString(agentId)});

        if (cursor == null) {
            throw new AssertionError("Cursor should not be null");
        }

        final SparseArray<String> matcher = new SparseArray<>(cursor.getCount());
        try {
            if (cursor.moveToFirst()) {
                do {
                    final int alphabet = cursor.getInt(0);
                    final String str = cursor.getString(1);
                    matcher.put(alphabet, str);
                } while (cursor.moveToNext());
            }
        }
        finally {
            cursor.close();
        }

        return matcher;
    }

    private boolean agentFromStart(int flags) {
        return (flags & 1) != 0;
    }

    private class AgentSetSupplier {

        private final int _agentId;
        private boolean _created;
        private int _agentSetId;

        AgentSetSupplier(int agentId) {
            _agentId = agentId;
        }

        private int get(SQLiteDatabase db) {
            if (!_created) {
                final Set<Integer> set = new HashSet<>();
                set.add(_agentId);
                _agentSetId = insertAgentSet(db, set);
            }

            return _agentSetId;
        }
    }

    /**
     * @return True if the suggestedNewWordId has been used.
     */
    private boolean applyAgent(SQLiteDatabase db, AgentSetSupplier agentSetSupplier, int accId, int concept, int suggestedNewWordId, int targetBunch, SparseArray<String> matcher, SparseArray<String> adder, int rule, SparseArray<String> corr, int flags) {
        boolean suggestedNewWordUsed = false;
        boolean matching = true;

        final int matcherLength = matcher.size();
        for (int i = 0; i < matcherLength; i++) {
            final int alphabet = matcher.keyAt(i);
            final String corrStr = corr.get(alphabet);
            final String matcherStr = matcher.valueAt(i);
            matching = corrStr != null && (
                    agentFromStart(flags) && corrStr.startsWith(matcherStr) ||
                            !agentFromStart(flags) && corrStr.endsWith(matcherStr)
            );

            if (!matching) {
                break;
            }
        }

        if (matching) {
            int targetAccId = accId;

            // TODO: This condition should be changed with just matcher == adder.
            // But OldDbConverter has to be updated first
            final boolean modifyWords = rule != StreamedDatabaseConstants.nullRuleId &&
                    matcher.size() != 0 && adder.size() != 0 && !EqualUtils.checkEqual(matcher, adder);

            if (modifyWords) {
                SparseArray<String> resultCorr = new SparseArray<>();
                final int corrLength = corr.size();
                for (int i = 0; i < corrLength; i++) {
                    final int alphabet = corr.keyAt(i);
                    final String matcherStr = matcher.get(alphabet);
                    final int removeLength = (matcherStr != null) ? matcherStr.length() : 0;

                    final String corrStr = corr.valueAt(i);
                    final String resultStr = agentFromStart(flags) ?
                            corrStr.substring(removeLength) :
                            corrStr.substring(0, corrStr.length() - removeLength);

                    resultCorr.put(alphabet, resultStr);
                }

                final int adderLength = adder.size();
                for (int i = 0; i < adderLength; i++) {
                    final int alphabet = adder.keyAt(i);
                    final String addition = adder.valueAt(i);
                    final String currentStr = resultCorr.get(alphabet);
                    final String resultStr = (currentStr != null) ? currentStr + addition : addition;
                    resultCorr.put(alphabet, resultStr);
                }

                final int newConcept = obtainRuledConcept(db, rule, concept);
                final int resultCorrLength = resultCorr.size();
                final SparseIntArray resultCorrIds = new SparseIntArray(resultCorrLength);
                for (int i = 0; i < resultCorrLength; i++) {
                    final int alphabet = resultCorr.keyAt(i);
                    resultCorrIds.put(alphabet, insertIfNotExists(db, resultCorr.valueAt(i)));
                }

                final int corrId = obtainCorrelation(db, resultCorrIds);
                final int corrArrayId = insertCorrelationArray(db, corrId);
                suggestedNewWordUsed = true;
                final int dynAccId = insertAcceptation(db, suggestedNewWordId, newConcept, corrArrayId);

                for (int i = 0; i < resultCorrLength; i++) {
                    insertStringQuery(db, resultCorr.valueAt(i), resultCorr.valueAt(0), accId, dynAccId, resultCorr.keyAt(i));
                }

                targetAccId = dynAccId;
            }

            if (targetBunch != StreamedDatabaseConstants.nullBunchId) {
                insertBunchAcceptation(db, targetBunch, targetAccId, agentSetSupplier.get(db));
            }
        }

        return suggestedNewWordUsed;
    }

    private void runAgent(SQLiteDatabase db, int agentId) {
        AcceptationsTable acceptations = Tables.acceptations;
        AgentsTable agents = Tables.agents;
        BunchSetsTable bunchSets = Tables.bunchSets;
        BunchAcceptationsTable bunchAccs = Tables.bunchAcceptations;
        StringQueriesTable strings = Tables.stringQueries;

        int maxWord = getMaxWord(db);
        final SparseArray<String> matcher = readCorrelation(db, agentId, agents.getMatcherColumnIndex());
        final SparseArray<String> adder = readCorrelation(db, agentId, agents.getAdderColumnIndex());

        // TODO: This query does not manage the case where sourceSet is null
        // TODO: This query does not manage the case where diff is different from null
        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J2." + bunchAccs.getColumnName(bunchAccs.getAcceptationColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringColumnIndex()) +
                        ",J0." + agents.getColumnName(agents.getRuleColumnIndex()) +
                        ",J0." + agents.getColumnName(agents.getFlagsColumnIndex()) +
                        ",J4." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        ",J0." + agents.getColumnName(agents.getTargetBunchColumnIndex()) +
                " FROM " + agents.getName() + " AS J0" +
                " JOIN " + bunchSets.getName() + " AS J1 ON J0." + agents.getColumnName(agents.getSourceBunchSetColumnIndex()) + "=J1." + bunchSets.getColumnName(bunchSets.getSetIdColumnIndex()) +
                " JOIN " + bunchAccs.getName() + " AS J2 ON J1." + bunchSets.getColumnName(bunchSets.getBunchColumnIndex()) + "=J2." + bunchAccs.getColumnName(bunchAccs.getBunchColumnIndex()) +
                " JOIN " + strings.getName() + " AS J3 ON J2." + bunchAccs.getColumnName(bunchAccs.getAcceptationColumnIndex()) + "=J3." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                " JOIN " + acceptations.getName() + " AS J4 ON J2." + bunchAccs.getColumnName(bunchAccs.getAcceptationColumnIndex()) + "=J4." + idColumnName +
                " WHERE J0." + idColumnName + "=?" +
                " ORDER BY" +
                        " J2." + bunchAccs.getColumnName(bunchAccs.getAcceptationColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringAlphabetColumnIndex())
                , new String[] { Integer.toString(agentId)});

        if (cursor != null) {
            try {
                final SparseArray<String> corr = new SparseArray<>();
                int accId;
                int rule;
                int flags;
                int concept;
                int targetBunch;

                if (cursor.moveToFirst()) {
                    accId = cursor.getInt(0);
                    corr.put(cursor.getInt(1), cursor.getString(2));
                    rule = cursor.getInt(3);
                    flags = cursor.getInt(4);
                    concept = cursor.getInt(5);
                    targetBunch = cursor.getInt(6);

                    final AgentSetSupplier agentSetSupplier = new AgentSetSupplier(agentId);
                    int newAccId;
                    while (cursor.moveToNext()) {
                        newAccId = cursor.getInt(0);
                        if (newAccId != accId) {
                            if (applyAgent(db, agentSetSupplier, accId, concept, maxWord + 1, targetBunch, matcher, adder, rule, corr, flags)) {
                                ++maxWord;
                            }

                            accId = newAccId;
                            corr.clear();
                            corr.put(cursor.getInt(1), cursor.getString(2));
                            rule = cursor.getInt(3);
                            flags = cursor.getInt(4);
                            concept = cursor.getInt(5);
                            targetBunch = cursor.getInt(6);
                        }
                        else {
                            corr.put(cursor.getInt(1), cursor.getString(2));
                        }
                    }

                    applyAgent(db, agentSetSupplier, accId, concept, maxWord +1, targetBunch, matcher, adder, rule, corr, flags);
                }
            }
            finally {
                cursor.close();
            }
        }
    }

    private void runAgents(SQLiteDatabase db, SparseArray<Agent> agents) {
        final int agentCount = agents.size();
        int index = 0;
        for (int agentId : sortAgents(agents)) {
            setProgress(0.4f + ((0.8f - 0.4f) / agentCount) * index, "Running agent " + (++index) + " out of " + agentCount);
            runAgent(db, agentId);
        }
    }

    private void fillSearchQueryTable(SQLiteDatabase db) {
        final AcceptationsTable acceptations = Tables.acceptations; // J0
        final CorrelationArraysTable correlationArrays = Tables.correlationArrays; // J1
        final CorrelationsTable correlations = Tables.correlations; // J2
        final SymbolArraysTable symbolArrays = Tables.symbolArrays; // J3
        final AlphabetsTable alphabets = Tables.alphabets;
        final LanguagesTable languages = Tables.languages;

        Cursor cursor = db.rawQuery("SELECT accId, alphabetId, group_concat(str1,''), group_concat(str2,'') FROM (" +
                "SELECT" +
                    " J0." + idColumnName + " AS accId," +
                    " J2." + correlations.getColumnName(correlations.getAlphabetColumnIndex()) + " AS alphabetId," +
                    " J7." + symbolArrays.getColumnName(symbolArrays.getStrColumnIndex()) + " AS str2," +
                    " J3." + symbolArrays.getColumnName(symbolArrays.getStrColumnIndex()) + " AS str1" +
                " FROM " + acceptations.getName() + " AS J0" +
                " JOIN " + correlationArrays.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getCorrelationArrayColumnIndex()) + "=J1." + correlationArrays.getColumnName(correlationArrays.getArrayIdColumnIndex()) +
                " JOIN " + correlations.getName() + " AS J2 ON J1." + correlationArrays.getColumnName(correlationArrays.getCorrelationColumnIndex()) + "=J2." + correlations.getColumnName(correlations.getCorrelationIdColumnIndex()) +
                " JOIN " + symbolArrays.getName() + " AS J3 ON J2." + correlations.getColumnName(correlations.getSymbolArrayColumnIndex()) + "=J3." + idColumnName +
                " JOIN " + alphabets.getName() + " AS J4 ON J2." + correlations.getColumnName(correlations.getAlphabetColumnIndex()) + "=J4." + idColumnName +
                " JOIN " + languages.getName() + " AS J5 ON J4." + alphabets.getColumnName(alphabets.getLanguageColumnIndex()) + "=J5." + idColumnName +
                " JOIN " + correlations.getName() + " AS J6 ON J1." + correlationArrays.getColumnName(correlationArrays.getCorrelationColumnIndex()) + "=J6." + correlations.getColumnName(correlations.getCorrelationIdColumnIndex()) +
                " JOIN " + symbolArrays.getName() + " AS J7 ON J6." + correlations.getColumnName(correlations.getSymbolArrayColumnIndex()) + "=J7." + idColumnName +
                " WHERE J5." + languages.getColumnName(languages.getMainAlphabetColumnIndex()) + "=J6." + correlations.getColumnName(correlations.getAlphabetColumnIndex()) +

                " ORDER BY" +
                    " J0." + idColumnName +
                    ",J2." + correlations.getColumnName(correlations.getAlphabetColumnIndex()) +
                    ",J1." + correlationArrays.getColumnName(correlationArrays.getArrayPositionColumnIndex()) +
                ") GROUP BY accId, alphabetId", null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        final int accId = cursor.getInt(0);
                        final int alphabet = cursor.getInt(1);
                        final String str = cursor.getString(2);
                        final String mainStr = cursor.getString(3);

                        // TODO: Change this to point to the dynamic acceptation in Japanese. More JOINS are required whenever agents are applied
                        final int dynAccId = accId;

                        insertStringQuery(db, str, mainStr, accId, dynAccId, alphabet);
                    } while(cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }
    }

    private void applyConversions(SQLiteDatabase db, Conversion[] conversions) {
        for (Conversion conversion : conversions) {
            final int sourceAlphabet = conversion.getSourceAlphabet();

            final DbManager.StringQueriesTable table = DbManager.Tables.stringQueries;
            Cursor cursor = db.rawQuery("SELECT " +
                    table.getColumnName(table.getStringColumnIndex()) + ',' +
                    table.getColumnName(table.getMainStringColumnIndex()) + ',' +
                    table.getColumnName(table.getMainAcceptationColumnIndex()) + ',' +
                    table.getColumnName(table.getDynamicAcceptationColumnIndex()) +
                    " FROM " + table.getName() + " WHERE " + table.getColumnName(table.getStringAlphabetColumnIndex()) + '=' + sourceAlphabet, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            final String str = conversion.convert(cursor.getString(0));
                            if (str != null) {
                                final String mainStr = cursor.getString(1);
                                final int mainAcc = cursor.getInt(2);
                                final int dynAcc = cursor.getInt(3);

                                insertStringQuery(db, str, mainStr, mainAcc, dynAcc, conversion.getTargetAlphabet());
                            }
                        } while (cursor.moveToNext());
                    }
                }
                finally {
                    cursor.close();
                }
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // So far, version 5 is the only one expected. So this method should never be called
    }

    private static class DatabaseImportProgress {

        final float progress;
        final String message;

        DatabaseImportProgress(float progress, String message) {
            this.progress = progress;
            this.message = message;
        }
    }

    private class DatabaseImportTask extends AsyncTask<Uri, DatabaseImportProgress, Boolean> implements DbManager.DatabaseImportProgressListener {

        @Override
        protected Boolean doInBackground(Uri... uris) {
            getWritableDatabase().close();
            _context.deleteDatabase(DB_NAME);
            _uri = uris[0];
            getWritableDatabase().close();
            return true;
        }

        @Override
        public void setProgress(float progress, String message) {
            publishProgress(new DatabaseImportProgress(progress, message));
        }

        @Override
        public void onProgressUpdate(DatabaseImportProgress... progresses) {
            final DatabaseImportProgress progress = progresses[0];
            final DatabaseImportProgressListener listener = _externalProgressListener;
            if (listener != null) {
                listener.setProgress(progress.progress, progress.message);
            }

            _lastProgress = progress;
        }

        @Override
        public void onPostExecute(Boolean result) {
            _progressListener = null;
            _databaseImportTask = null;

            final DatabaseImportProgressListener listener = _externalProgressListener;
            final DatabaseImportProgress lastProgress = new DatabaseImportProgress(1f, "Completed");
            if (listener != null) {
                listener.setProgress(lastProgress.progress, lastProgress.message);
            }
            _lastProgress = lastProgress;
        }
    }

    public void importDatabase(Uri uri) {
        _databaseImportTask = new DatabaseImportTask();
        _progressListener = _databaseImportTask;
        _databaseImportTask.execute(uri);
    }

    public boolean isImportingDatabase() {
        return _databaseImportTask != null;
    }

    public void setProgressListener(DatabaseImportProgressListener listener) {
        _externalProgressListener = listener;
        final DatabaseImportProgress lastProgress = _lastProgress;
        if (listener != null && lastProgress != null) {
            listener.setProgress(lastProgress.progress, lastProgress.message);
        }
    }
}
