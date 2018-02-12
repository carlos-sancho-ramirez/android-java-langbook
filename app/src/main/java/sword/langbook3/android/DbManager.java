package sword.langbook3.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sword.bitstream.FunctionWithIOException;
import sword.bitstream.InputBitStream;
import sword.bitstream.IntegerDecoder;
import sword.bitstream.RangedIntegerSetDecoder;
import sword.bitstream.SupplierWithIOException;
import sword.bitstream.huffman.CharHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

class DbManager extends SQLiteOpenHelper {

    static final String DB_NAME = "Langbook";
    private static final int DB_VERSION = 5;

    private static DbManager _instance;

    private final NaturalNumberHuffmanTable _naturalNumberTable = new NaturalNumberHuffmanTable(8);
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

        private final CharHuffmanTable _table = new CharHuffmanTable(8);
        private final InputBitStream _ibs;

        CharReader(InputBitStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Character apply() throws IOException {
            return _ibs.readHuffmanSymbol(_table);
        }
    }

    private static class CharHuffmanSymbolDiffReader implements FunctionWithIOException<Character, Character> {

        private final InputBitStream _ibs;
        private final NaturalNumberHuffmanTable _table;

        CharHuffmanSymbolDiffReader(InputBitStream ibs, NaturalNumberHuffmanTable table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Character apply(Character previous) throws IOException {
            int value = _ibs.readHuffmanSymbol(_table);
            return (char) (value + previous + 1);
        }
    }

    private class IntReader implements SupplierWithIOException<Integer> {

        private final InputBitStream _ibs;

        IntReader(InputBitStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Integer apply() throws IOException {
            return _ibs.readHuffmanSymbol(_naturalNumberTable);
        }
    }

    private static class IntHuffmanSymbolReader implements SupplierWithIOException<Integer> {

        private final InputBitStream _ibs;
        private final NaturalNumberHuffmanTable _table;

        IntHuffmanSymbolReader(InputBitStream ibs, NaturalNumberHuffmanTable table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Integer apply() throws IOException {
            return _ibs.readHuffmanSymbol(_table);
        }
    }

    private class IntDiffReader implements FunctionWithIOException<Integer, Integer> {

        private final InputBitStream _ibs;

        IntDiffReader(InputBitStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Integer apply(Integer previous) throws IOException {
            return _ibs.readHuffmanSymbol(_naturalNumberTable) + previous + 1;
        }
    }

    private static class IntHuffmanSymbolDiffReader implements FunctionWithIOException<Integer, Integer> {

        private final InputBitStream _ibs;
        private final NaturalNumberHuffmanTable _table;

        IntHuffmanSymbolDiffReader(InputBitStream ibs, NaturalNumberHuffmanTable table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Integer apply(Integer previous) throws IOException {
            return _ibs.readHuffmanSymbol(_table) + previous + 1;
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

    private Integer findRuledConcept(SQLiteDatabase db, int agent, int concept) {
        final RuledConceptsTable table = Tables.ruledConcepts;
        final String whereClause = table.getColumnName(table.getAgentColumnIndex()) + "=? AND " +
                table.getColumnName(table.getConceptColumnIndex()) + "=?";
        Cursor cursor = db.query(table.getName(), new String[] {idColumnName}, whereClause,
                new String[] { Integer.toString(agent), Integer.toString(concept) }, null, null, null, null);

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

    private Integer insertSymbolArray(SQLiteDatabase db, String str) {
        final SymbolArraysTable table = Tables.symbolArrays;
        ContentValues cv = new ContentValues();
        cv.put(table.getColumnName(table.getStrColumnIndex()), str);
        final long returnId = db.insert(table.getName(), null, cv);
        return (returnId >= 0)? (int) returnId : null;
    }

    private int obtainSymbolArray(SQLiteDatabase db, String str) {
        Integer id = insertSymbolArray(db, str);
        if (id != null) {
            return id;
        }

        id = getSymbolArray(db, str);
        if (id == null) {
            throw new AssertionError("Unable to insert, and not present");
        }

        return id;
    }

    private void insertRuledAcceptation(SQLiteDatabase db, int ruledAcceptation, int agent, int acceptation) {
        final RuledAcceptationsTable table = Tables.ruledAcceptations;
        db.execSQL("INSERT INTO " + table.getName() + " (" + idColumnName + ',' +
                table.getColumnName(table.getAgentColumnIndex()) + ',' +
                table.getColumnName(table.getAcceptationColumnIndex()) + ") VALUES (" +
                ruledAcceptation + ',' + agent + ',' + acceptation + ')');
    }

    private int insertRuledConcept(SQLiteDatabase db, int agent, int concept) {
        final int ruledConcept = getMaxConcept(db) + 1;
        final RuledConceptsTable table = Tables.ruledConcepts;
        db.execSQL("INSERT INTO " + table.getName() + " (" + idColumnName + ',' +
                table.getColumnName(table.getAgentColumnIndex()) + ',' +
                table.getColumnName(table.getConceptColumnIndex()) + ") VALUES (" +
                ruledConcept + ',' + agent + ',' + concept + ')');
        final Integer id = findRuledConcept(db, agent, concept);
        if (id == null) {
            throw new AssertionError("A just introduced register should be found");
        }

        return id;
    }

    private int obtainRuledConcept(SQLiteDatabase db, int agent, int concept) {
        final Integer id = findRuledConcept(db, agent, concept);
        if (id != null) {
            return id;
        }

        return insertRuledConcept(db, agent, concept);
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

            SQLiteStatement statement = db.compileStatement("INSERT INTO " + table.getName() + " (" +
                    correlationIdColumnName + ", " +
                    alphabetColumnName + ", " +
                    symbolArrayColumnName + ") VALUES (" +
                    newCorrelationId + ",?,?)");

            for (int i = 0; i < mapLength; i++) {
                statement.bindLong(1, correlation.keyAt(i));
                statement.bindLong(2, correlation.valueAt(i));
                statement.executeInsert();
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

    private int[] getCorrelationArray(SQLiteDatabase db, int id) {
        if (id == StreamedDatabaseConstants.nullCorrelationArrayId) {
            return new int[0];
        }

        CorrelationArraysTable table = Tables.correlationArrays;
        Cursor cursor = db.rawQuery("SELECT " + table.getColumnName(table.getArrayPositionColumnIndex()) +
                ',' + table.getColumnName(table.getCorrelationColumnIndex()) +
                " FROM " + table.getName() + " WHERE " + table.getColumnName(table.getArrayIdColumnIndex()) +
                "=?", new String[] {Integer.toString(id)});

        int[] result = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    result = new int[cursor.getCount()];
                    final BitSet set = new BitSet();
                    do {
                        final int pos = cursor.getInt(0);
                        final int corr = cursor.getInt(1);
                        if (set.get(pos)) {
                            throw new AssertionError("Malformed correlation array with id " + id);
                        }

                        set.set(pos);
                        result[pos] = corr;
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private Integer findCorrelationArray(SQLiteDatabase db, int... correlations) {
        if (correlations.length == 0) {
            return StreamedDatabaseConstants.nullCorrelationArrayId;
        }

        CorrelationArraysTable table = Tables.correlationArrays;
        Cursor cursor = db.rawQuery("SELECT " + table.getColumnName(table.getArrayIdColumnIndex()) +
                " FROM " + table.getName() + " WHERE " + table.getColumnName(table.getArrayPositionColumnIndex()) +
                "=0 AND " + table.getColumnName(table.getCorrelationColumnIndex()) +
                "=?", new String[] {Integer.toString(correlations[0])});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        final int arrayId = cursor.getInt(0);
                        final int[] array = getCorrelationArray(db, arrayId);
                        if (Arrays.equals(correlations, array)) {
                            return arrayId;
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

    private int insertCorrelationArray(SQLiteDatabase db, int... correlation) {
        final CorrelationArraysTable table = Tables.correlationArrays;
        final String arrayIdColumnName = table.getColumnName(table.getArrayIdColumnIndex());
        Cursor cursor = db.rawQuery("SELECT max(" + arrayIdColumnName + ") FROM " +
                table.getName(), null);

        if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()) {
            throw new AssertionError("Unable to retrieve maximum arrayId");
        }

        try {
            final int maxArrayId = cursor.getInt(0);
            final int newArrayId = maxArrayId + ((maxArrayId + 1 != StreamedDatabaseConstants.nullCorrelationArrayId)? 1 : 2);
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

    private int obtainCorrelationArray(SQLiteDatabase db, int... array) {
        final Integer id = findCorrelationArray(db, array);
        return (id == null)? insertCorrelationArray(db, array) : id;
    }

    private int insertAcceptation(SQLiteDatabase db, int word, int concept, int correlationArray) {
        final AcceptationsTable table = Tables.acceptations;
        ContentValues cv = new ContentValues();
        cv.put(table.getColumnName(table.getWordColumnIndex()), word);
        cv.put(table.getColumnName(table.getConceptColumnIndex()), concept);
        cv.put(table.getColumnName(table.getCorrelationArrayColumnIndex()), correlationArray);
        final long returnId = db.insert(table.getName(), null, cv);
        if (returnId < 0) {
            throw new AssertionError("insert returned a negative id");
        }
        return (int) returnId;
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

    private int findLastBunchSet(SQLiteDatabase db) {
        final BunchSetsTable table = Tables.bunchSets;
        final String setIdColumnName = table.getColumnName(table.getSetIdColumnIndex());
        final Cursor cursor = db.rawQuery("SELECT max(" + setIdColumnName + ") FROM " +
                table.getName(), null);

        if (cursor != null) {
            try {
                if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
                else if (cursor.getCount() == 0) {
                    return table.nullReference();
                }
            }
            finally {
                cursor.close();
            }
        }

        throw new AssertionError("Unable to retrieve maximum setId");
    }

    private int insertBunchSet(SQLiteDatabase db, int setId, Set<Integer> bunches) {
        final BunchSetsTable table = Tables.bunchSets;

        if (bunches.isEmpty()) {
            return table.nullReference();
        }
        else {
            for (int bunch : bunches) {
                db.execSQL("INSERT INTO " + table.getName() + " (" +
                        table.getColumnName(table.getSetIdColumnIndex()) + ", " +
                        table.getColumnName(table.getBunchColumnIndex()) + ") VALUES (" +
                        setId + ',' + bunch + ')');
            }

            return setId;
        }
    }

    private Integer findBunchSet(SQLiteDatabase db, Set<Integer> bunches) {
        final BunchSetsTable table = Tables.bunchSets;
        if (bunches.isEmpty()) {
            return table.nullReference();
        }

        final String setIdField = table.getColumnName(table.getSetIdColumnIndex());
        Cursor cursor = db.rawQuery("SELECT " +
                setIdField + ',' + table.getColumnName(table.getBunchColumnIndex()) +
                " FROM " + table.getName() + " ORDER BY " + setIdField, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int setId = cursor.getInt(0);
                    final Set<Integer> set = new HashSet<>();
                    set.add(cursor.getInt(1));

                    while (cursor.moveToNext()) {
                        if (setId == cursor.getInt(0)) {
                            set.add(cursor.getInt(1));
                        }
                        else {
                            if (set.equals(bunches)) {
                                return setId;
                            }

                            setId = cursor.getInt(0);
                            set.clear();
                            set.add(cursor.getInt(1));
                        }
                    }

                    if (set.equals(bunches)) {
                        return setId;
                    }
                }
            }
            finally {
                cursor.close();
            }
        }

        return null;
    }

    private int obtainBunchSet(SQLiteDatabase db, int setId, Set<Integer> bunches) {
        final Integer foundId = findBunchSet(db, bunches);
        return (foundId != null)? foundId : insertBunchSet(db, setId, bunches);
    }

    static final class QuestionField {

        final int alphabet;
        final int rule;
        final int flags;

        QuestionField(int alphabet, int rule, int flags) {
            this.alphabet = alphabet;
            this.rule = rule;
            this.flags = flags;
        }

        int getType() {
            return flags & DbManager.QuestionFieldFlags.TYPE_MASK;
        }

        boolean isAnswer() {
            return (flags & DbManager.QuestionFieldFlags.IS_ANSWER) != 0;
        }

        @Override
        public int hashCode() {
            return (alphabet * 31 + rule) * 31 + flags;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof QuestionField)) {
                return false;
            }

            QuestionField that = (QuestionField) other;
            return alphabet == that.alphabet && rule == that.rule && flags == that.flags;
        }
    }

    static Integer findQuestionFieldSet(SQLiteDatabase db, Collection<QuestionField> collection) {
        if (collection == null || collection.size() == 0) {
            return null;
        }

        final Set<QuestionField> set = new HashSet<>(collection);

        final QuestionField firstField = set.iterator().next();
        final DbManager.QuestionFieldSets fieldSets = DbManager.Tables.questionFieldSets;
        final Cursor cursor = db.rawQuery("SELECT" +
                        " J0." + fieldSets.getColumnName(fieldSets.getSetIdColumnIndex()) +
                        ",J1." + fieldSets.getColumnName(fieldSets.getAlphabetColumnIndex()) +
                        ",J1." + fieldSets.getColumnName(fieldSets.getRuleColumnIndex()) +
                        ",J1." + fieldSets.getColumnName(fieldSets.getFlagsColumnIndex()) +
                        " FROM " + fieldSets.getName() + " AS J0" +
                        " JOIN " + fieldSets.getName() + " AS J1 ON J0." + fieldSets.getColumnName(fieldSets.getSetIdColumnIndex()) + "=J1." + fieldSets.getColumnName(fieldSets.getSetIdColumnIndex()) +
                        " WHERE J0." + fieldSets.getColumnName(fieldSets.getAlphabetColumnIndex()) + "=?" +
                        " AND J0." + fieldSets.getColumnName(fieldSets.getRuleColumnIndex()) + "=?" +
                        " AND J0." + fieldSets.getColumnName(fieldSets.getFlagsColumnIndex()) + "=?",
                new String[] {Integer.toString(firstField.alphabet), Integer.toString(firstField.rule), Integer.toString(firstField.flags)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int setId = cursor.getInt(0);
                    final Set<QuestionField> foundSet = new HashSet<>();
                    foundSet.add(new QuestionField(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3)));

                    while (cursor.moveToNext()) {
                        if (setId != cursor.getInt(0)) {
                            if (foundSet.equals(set)) {
                                return setId;
                            }
                            else {
                                foundSet.clear();
                                setId = cursor.getInt(0);
                            }
                        }

                        foundSet.add(new QuestionField(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3)));
                    }

                    if (foundSet.equals(set)) {
                        return setId;
                    }
                }
            }
            finally {
                cursor.close();
            }
        }

        return null;
    }

    private static int getMaxQuestionFieldSetId(SQLiteDatabase db) {
        DbManager.QuestionFieldSets table = DbManager.Tables.questionFieldSets;
        Cursor cursor = db.rawQuery("SELECT max(" +
                table.getColumnName(table.getSetIdColumnIndex()) + ") FROM " +
                table.getName(), null);

        if (cursor != null && cursor.getCount() == 1 && cursor.moveToFirst()) {
            try {
                return cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }
        else {
            return 0;
        }
    }

    static int insertQuestionFieldSet(SQLiteDatabase db, List<QuestionField> fields) {
        final DbManager.QuestionFieldSets table = DbManager.Tables.questionFieldSets;
        ContentValues fieldsCv = new ContentValues();
        final int setId = getMaxQuestionFieldSetId(db) + 1;

        for (QuestionField field : fields) {
            fieldsCv.put(table.getColumnName(table.getSetIdColumnIndex()), setId);
            fieldsCv.put(table.getColumnName(table.getAlphabetColumnIndex()), field.alphabet);
            fieldsCv.put(table.getColumnName(table.getRuleColumnIndex()), field.rule);
            fieldsCv.put(table.getColumnName(table.getFlagsColumnIndex()), field.flags);
            db.insert(table.getName(), null, fieldsCv);
        }

        return setId;
    }

    static int obtainQuestionFieldSet(SQLiteDatabase db, List<QuestionField> fields) {
        final Integer setId = findQuestionFieldSet(db, fields);
        return (setId != null)? setId : insertQuestionFieldSet(db, fields);
    }

    static Integer findQuizDefinition(SQLiteDatabase db, int bunch, int setId) {
        final DbManager.QuizDefinitionsTable quizzes = Tables.quizDefinitions;
        final Cursor cursor = db.rawQuery("SELECT " + idColumnName +
                " FROM " + quizzes.getName() +
                " WHERE " + quizzes.getColumnName(quizzes.getBunchColumnIndex()) + "=?" +
                " AND " + quizzes.getColumnName(quizzes.getQuestionFieldsColumnIndex()) + "=?",
                new String[] {Integer.toString(bunch), Integer.toString(setId)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            }
            finally {
                cursor.close();
            }
        }

        return null;
    }

    static int insertQuizDefinition(SQLiteDatabase db, int bunch, int setId) {
        final QuizDefinitionsTable table = Tables.quizDefinitions;
        ContentValues cv = new ContentValues();
        cv.put(table.getColumnName(table.getBunchColumnIndex()), bunch);
        cv.put(table.getColumnName(table.getQuestionFieldsColumnIndex()), setId);
        final long returnId = db.insert(table.getName(), null, cv);
        if (returnId < 0) {
            throw new AssertionError("insert returned a negative id");
        }
        return (int) returnId;
    }

    static int obtainQuizDefinition(SQLiteDatabase db, int bunch, int setId) {
        final Integer id = findQuizDefinition(db, bunch, setId);
        return (id != null)? id : insertQuizDefinition(db, bunch, setId);
    }

    private int[] readSymbolArrays(SQLiteDatabase db, InputBitStream ibs) throws IOException {
        final int symbolArraysLength = ibs.readHuffmanSymbol(_naturalNumberTable);
        final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
        final NaturalNumberHuffmanTable nat4Table = new NaturalNumberHuffmanTable(4);

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

            idMap[index] = obtainSymbolArray(db, builder.toString());
        }

        return idMap;
    }

    private Conversion[] readConversions(SQLiteDatabase db, InputBitStream ibs, int minValidAlphabet, int maxValidAlphabet, int minSymbolArrayIndex, int maxSymbolArrayIndex, int[] symbolArraysIdMap) throws IOException {
        final int conversionsLength = ibs.readHuffmanSymbol(_naturalNumberTable);
        final Conversion[] conversions = new Conversion[conversionsLength];
        final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(minSymbolArrayIndex, maxSymbolArrayIndex);

        int minSourceAlphabet = minValidAlphabet;
        int minTargetAlphabet = minValidAlphabet;
        for (int i = 0; i < conversionsLength; i++) {
            final RangedIntegerHuffmanTable sourceAlphabetTable = new RangedIntegerHuffmanTable(minSourceAlphabet, maxValidAlphabet);
            final int sourceAlphabet = ibs.readHuffmanSymbol(sourceAlphabetTable);

            if (minSourceAlphabet != sourceAlphabet) {
                minTargetAlphabet = minValidAlphabet;
                minSourceAlphabet = sourceAlphabet;
            }

            final RangedIntegerHuffmanTable targetAlphabetTable = new RangedIntegerHuffmanTable(minTargetAlphabet, maxValidAlphabet);
            final int targetAlphabet = ibs.readHuffmanSymbol(targetAlphabetTable);
            minTargetAlphabet = targetAlphabet + 1;

            final int pairCount = ibs.readHuffmanSymbol(_naturalNumberTable);
            final String[] sources = new String[pairCount];
            final String[] targets = new String[pairCount];
            for (int j = 0; j < pairCount; j++) {
                final int source = symbolArraysIdMap[ibs.readHuffmanSymbol(symbolArrayTable)];
                final int target = symbolArraysIdMap[ibs.readHuffmanSymbol(symbolArrayTable)];
                insertConversion(db, sourceAlphabet, targetAlphabet, source, target);

                sources[j] = getSymbolArray(db, source);
                targets[j] = getSymbolArray(db, target);
            }

            conversions[i] = new Conversion(sourceAlphabet, targetAlphabet, sources, targets);
        }

        return conversions;
    }

    private static class ValueDecoder<E> implements SupplierWithIOException<E> {

        private final InputBitStream _ibs;
        private final HuffmanTable<E> _table;

        ValueDecoder(InputBitStream ibs, HuffmanTable<E> table) {
            if (ibs == null || table == null) {
                throw new IllegalArgumentException();
            }

            _ibs = ibs;
            _table = table;
        }

        @Override
        public E apply() throws IOException {
            return _ibs.readHuffmanSymbol(_table);
        }
    }

    private int[] readCorrelations(SQLiteDatabase db, InputBitStream ibs, int minAlphabet, int maxAlphabet, int[] symbolArraysIdMap) throws IOException {
        final int correlationsLength = ibs.readHuffmanSymbol(_naturalNumberTable);
        final int[] result = new int[correlationsLength];
        if (correlationsLength > 0) {
            final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(0, symbolArraysIdMap.length - 1);
            final IntegerDecoder intDecoder = new IntegerDecoder(ibs);
            final HuffmanTable<Integer> lengthTable = ibs.readHuffmanTable(intDecoder, intDecoder);
            final RangedIntegerSetDecoder keyDecoder = new RangedIntegerSetDecoder(ibs, lengthTable, minAlphabet, maxAlphabet);
            final ValueDecoder<Integer> valueDecoder = new ValueDecoder<>(ibs, symbolArrayTable);

            for (int i = 0; i < correlationsLength; i++) {
                Map<Integer, Integer> corrMap = ibs.readMap(keyDecoder, keyDecoder, keyDecoder, valueDecoder);
                SparseIntArray corr = new SparseIntArray();
                for (Map.Entry<Integer, Integer> entry : corrMap.entrySet()) {
                    corr.put(entry.getKey(), symbolArraysIdMap[entry.getValue()]);
                }
                result[i] = obtainCorrelation(db, corr);
            }
        }

        return result;
    }

    private int[] readCorrelationArrays(SQLiteDatabase db, InputBitStream ibs, int[] correlationIdMap) throws IOException {
        final int arraysLength = ibs.readHuffmanSymbol(_naturalNumberTable);
        final int[] result = new int[arraysLength];
        if (arraysLength > 0) {
            final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.length - 1);
            final IntegerDecoder intDecoder = new IntegerDecoder(ibs);
            final HuffmanTable<Integer> lengthTable = ibs.readHuffmanTable(intDecoder, intDecoder);

            for (int i = 0; i < arraysLength; i++) {
                final int arrayLength = ibs.readHuffmanSymbol(lengthTable);

                int[] corrArray = new int[arrayLength];
                for (int j = 0; j < arrayLength; j++) {
                    corrArray[j] = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                }
                result[i] = obtainCorrelationArray(db, corrArray);
            }
        }

        return result;
    }

    private int[] readAcceptations(SQLiteDatabase db, InputBitStream ibs, int[] wordIdMap, int[] conceptIdMap, int[] correlationArrayIdMap) throws IOException {
        final int acceptationsLength = ibs.readHuffmanSymbol(_naturalNumberTable);

        final int[] acceptationsIdMap = new int[acceptationsLength];
        if (acceptationsLength >= 0) {
            final IntegerDecoder intDecoder = new IntegerDecoder(ibs);
            final HuffmanTable<Integer> corrArraySetLengthTable = ibs.readHuffmanTable(intDecoder, intDecoder);
            final RangedIntegerHuffmanTable wordTable = new RangedIntegerHuffmanTable(StreamedDatabaseConstants.minValidWord, wordIdMap.length - 1);
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(StreamedDatabaseConstants.minValidConcept, conceptIdMap.length - 1);
            for (int i = 0; i < acceptationsLength; i++) {
                final int word = wordIdMap[ibs.readHuffmanSymbol(wordTable)];
                final int concept = conceptIdMap[ibs.readHuffmanSymbol(conceptTable)];
                final Set<Integer> corrArraySet = readRangedNumberSet(ibs, corrArraySetLengthTable, 0, correlationArrayIdMap.length - 1);
                for (int corrArray : corrArraySet) {
                    // TODO: Separate acceptations and correlations in 2 tables to avoid overlapping if there is more than one correlation array
                    acceptationsIdMap[i] = insertAcceptation(db, word, concept, correlationArrayIdMap[corrArray]);
                }
            }
        }

        return acceptationsIdMap;
    }

    private Set<Integer> readRangedNumberSet(InputBitStream ibs, HuffmanTable<Integer> lengthTable, int min, int max) throws IOException {
        final RangedIntegerSetDecoder decoder = new RangedIntegerSetDecoder(ibs, lengthTable, min, max);
        return ibs.readSet(decoder, decoder, decoder);
    }

    private void readBunchConcepts(SQLiteDatabase db, InputBitStream ibs, int minValidConcept, int maxConcept) throws IOException {
        final int bunchConceptsLength = ibs.readHuffmanSymbol(_naturalNumberTable);
        final HuffmanTable<Integer> bunchConceptsLengthTable = (bunchConceptsLength > 0)?
                ibs.readHuffmanTable(new IntReader(ibs), new IntDiffReader(ibs)) : null;

        final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(minValidConcept, maxConcept);
        for (int i = 0; i < bunchConceptsLength; i++) {
            final int bunch = ibs.readHuffmanSymbol(conceptTable);
            final Set<Integer> concepts = readRangedNumberSet(ibs, bunchConceptsLengthTable, minValidConcept, maxConcept);
            for (int concept : concepts) {
                insertBunchConcept(db, bunch, concept);
            }
        }
    }

    private void readBunchAcceptations(SQLiteDatabase db, InputBitStream ibs, int minValidConcept, int[] conceptIdMap, int[] acceptationsIdMap) throws IOException {
        final int bunchAcceptationsLength = ibs.readHuffmanSymbol(_naturalNumberTable);
        final HuffmanTable<Integer> bunchAcceptationsLengthTable = (bunchAcceptationsLength > 0)?
                ibs.readHuffmanTable(new IntReader(ibs), new IntDiffReader(ibs)) : null;

        final int maxValidAcceptation = acceptationsIdMap.length - 1;
        final int nullAgentSet = Tables.agentSets.nullReference();
        final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(minValidConcept, conceptIdMap.length - 1);

        for (int i = 0; i < bunchAcceptationsLength; i++) {
            final int bunch = conceptIdMap[ibs.readHuffmanSymbol(conceptTable)];
            final Set<Integer> acceptations = readRangedNumberSet(ibs, bunchAcceptationsLengthTable, 0, maxValidAcceptation);
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
            SQLiteDatabase db, InputBitStream ibs, int maxConcept, int[] correlationIdMap) throws IOException {

        final int agentsLength = ibs.readHuffmanSymbol(_naturalNumberTable);
        final SparseArray<Agent> result = new SparseArray<>(agentsLength);

        if (agentsLength > 0) {
            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final IntHuffmanSymbolReader intHuffmanSymbolReader = new IntHuffmanSymbolReader(ibs, nat3Table);
            final HuffmanTable<Integer> sourceSetLengthTable = ibs.readHuffmanTable(intHuffmanSymbolReader, null);

            int lastTarget = StreamedDatabaseConstants.nullBunchId;
            int minSource = StreamedDatabaseConstants.minValidConcept;
            int desiredSetId = findLastBunchSet(db) + 1;
            final Map<Set<Integer>, Integer> insertedBunchSets = new HashMap<>();
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(StreamedDatabaseConstants.minValidConcept, maxConcept);
            final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.length - 1);

            for (int i = 0; i < agentsLength; i++) {
                final RangedIntegerHuffmanTable thisConceptTable = new RangedIntegerHuffmanTable(lastTarget, maxConcept);
                final int targetBunch = ibs.readHuffmanSymbol(thisConceptTable);

                if (targetBunch != lastTarget) {
                    minSource = StreamedDatabaseConstants.minValidConcept;
                }

                final Set<Integer> sourceSet = readRangedNumberSet(ibs, sourceSetLengthTable, minSource, maxConcept);

                if (!sourceSet.isEmpty()) {
                    int min = Integer.MAX_VALUE;
                    for (int value : sourceSet) {
                        if (value < min) {
                            min = value;
                        }
                    }
                    minSource = min;
                }

                final int matcherId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                final int adderId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                final SparseIntArray matcher = getCorrelation(db, matcherId);
                final SparseIntArray adder = getCorrelation(db, adderId);

                final boolean adderNonEmpty = adder.size() > 0;
                final int rule = adderNonEmpty?
                        ibs.readHuffmanSymbol(conceptTable) :
                        StreamedDatabaseConstants.nullRuleId;

                final boolean fromStart = (adderNonEmpty || getCorrelation(db, matcherId).size() > 0) && ibs.readBoolean();
                final int flags = fromStart? 1 : 0;

                final Integer reusedBunchSetId = insertedBunchSets.get(sourceSet);
                final int sourceBunchSetId;
                if (reusedBunchSetId != null) {
                    sourceBunchSetId = reusedBunchSetId;
                }
                else {
                    sourceBunchSetId = obtainBunchSet(db, desiredSetId, sourceSet);
                    if (sourceBunchSetId == desiredSetId) {
                        ++desiredSetId;
                    }
                    insertedBunchSets.put(sourceSet, sourceBunchSetId);
                }

                final int diffBunchSetId = 0;

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

        /** Reserved for empty correlations */
        static final int nullCorrelationArrayId = 0;

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

    static abstract class DbColumn {

        private final String _name;

        DbColumn(String name) {
            _name = name;
        }

        final String getName() {
            return _name;
        }

        abstract String getType();
    }

    static final class DbIdColumn extends DbColumn {

        DbIdColumn() {
            super(idColumnName);
        }

        @Override
        String getType() {
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        }
    }

    static final class DbIntColumn extends DbColumn {

        DbIntColumn(String name) {
            super(name);
        }

        @Override
        String getType() {
            return "INTEGER";
        }
    }

    static final class DbTextColumn extends DbColumn {

        DbTextColumn(String name) {
            super(name);
        }

        @Override
        String getType() {
            return "TEXT";
        }
    }

    static final class DbUniqueTextColumn extends DbColumn {

        DbUniqueTextColumn(String name) {
            super(name);
        }

        @Override
        String getType() {
            return "TEXT UNIQUE ON CONFLICT IGNORE";
        }
    }

    static class DbTable {

        private final String _name;
        private final DbColumn[] _columns;

        DbTable(String name, DbColumn... columns) {
            _name = name;
            _columns = new DbColumn[columns.length + 1];

            System.arraycopy(columns, 0, _columns, 1, columns.length);
            _columns[0] = new DbIdColumn();
        }

        String getName() {
            return _name;
        }

        int getColumnCount() {
            return _columns.length;
        }

        String getColumnName(int index) {
            return _columns[index].getName();
        }

        String getColumnType(int index) {
            return _columns[index].getType();
        }
    }

    static final class AcceptationsTable extends DbTable {

        AcceptationsTable() {
            super("Acceptations", new DbIntColumn("word"), new DbIntColumn("concept"), new DbIntColumn("correlationArray"));
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
            super("Agents", new DbIntColumn("target"), new DbIntColumn("sourceSet"), new DbIntColumn("diffSet"),
                    new DbIntColumn("matcher"), new DbIntColumn("adder"), new DbIntColumn("rule"), new DbIntColumn("flags"));
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
            super("AgentSets", new DbIntColumn("setId"), new DbIntColumn("agent"));
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
            super("Alphabets", new DbIntColumn("language"));
        }

        int getLanguageColumnIndex() {
            return 1;
        }
    }

    static final class BunchAcceptationsTable extends DbTable {

        BunchAcceptationsTable() {
            super("BunchAcceptations", new DbIntColumn("bunch"), new DbIntColumn("acceptation"), new DbIntColumn("agentSet"));
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
            super("BunchConcepts", new DbIntColumn("bunch"), new DbIntColumn("concept"));
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
            super("BunchSets", new DbIntColumn("setId"), new DbIntColumn("bunch"));
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
            super("Conversions", new DbIntColumn("sourceAlphabet"), new DbIntColumn("targetAlphabet"), new DbIntColumn("source"), new DbIntColumn("target"));
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
            super("Correlations", new DbIntColumn("correlationId"), new DbIntColumn("alphabet"), new DbIntColumn("symbolArray"));
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
            super("CorrelationArrays", new DbIntColumn("arrayId"), new DbIntColumn("arrayPos"), new DbIntColumn("correlation"));
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

    static final class KnowledgeTable extends DbTable {

        KnowledgeTable() {
            super("Knowledge", new DbIntColumn("quizDefinition"), new DbIntColumn("acceptation"), new DbIntColumn("score"));
        }

        int getQuizDefinitionColumnIndex() {
            return 1;
        }

        int getAcceptationColumnIndex() {
            return 2;
        }

        int getScoreColumnIndex() {
            return 3;
        }
    }

    static final class LanguagesTable extends DbTable {

        LanguagesTable() {
            super("Languages", new DbIntColumn("mainAlphabet"), new DbUniqueTextColumn("code"));
        }

        int getMainAlphabetColumnIndex() {
            return 1;
        }

        int getCodeColumnIndex() {
            return 2;
        }
    }

    interface QuestionFieldFlags {

        /**
         * Once we have an acceptation, there are 3 kind of questions ways of retrieving the information for the question field.
         * <li>Same acceptation: We just get the acceptation form the Database.</li>
         * <li>Same concept: Other acceptation matching the origina concept must be found. Depending on the alphabet, they will be synonymous or translations.</li>
         * <li>Apply rule: The given acceptation is the dictionary form, then the ruled acceptation with the given rule should be found.</li>
         */
        int TYPE_MASK = 3;
        int TYPE_SAME_ACC = 0;
        int TYPE_SAME_CONCEPT = 1;
        int TYPE_APPLY_RULE = 2;

        /**
         * If set, question mask has to be displayed when performing the question.
         */
        int IS_ANSWER = 4;
    }

    static final class QuestionFieldSets extends DbTable {

        QuestionFieldSets() {
            super("QuestionFieldSets", new DbIntColumn("setId"), new DbIntColumn("alphabet"), new DbIntColumn("flags"), new DbIntColumn("rule"));
        }

        int getSetIdColumnIndex() {
            return 1;
        }

        int getAlphabetColumnIndex() {
            return 2;
        }

        /**
         * @see QuestionFieldFlags
         */
        int getFlagsColumnIndex() {
            return 3;
        }

        /**
         * Only relevant if question type if 'apply rule'. Ignored in other cases.
         */
        int getRuleColumnIndex() {
            return 4;
        }
    }

    static final class QuizDefinitionsTable extends DbTable {

        QuizDefinitionsTable() {
            super("QuizDefinitions", new DbIntColumn("bunch"), new DbIntColumn("questionFields"));
        }

        int getBunchColumnIndex() {
            return 1;
        }

        int getQuestionFieldsColumnIndex() {
            return 2;
        }
    }

    static final class RuledAcceptationsTable extends DbTable {

        RuledAcceptationsTable() {
            super("RuledAcceptations", new DbIntColumn("agent"), new DbIntColumn("acceptation"));
        }

        int getAgentColumnIndex() {
            return 1;
        }

        int getAcceptationColumnIndex() {
            return 2;
        }
    }

    static final class RuledConceptsTable extends DbTable {

        RuledConceptsTable() {
            super("RuledConcepts", new DbIntColumn("agent"), new DbIntColumn("concept"));
        }

        int getAgentColumnIndex() {
            return 1;
        }

        int getConceptColumnIndex() {
            return 2;
        }
    }

    static final class StringQueriesTable extends DbTable {

        StringQueriesTable() {
            super("StringQueryTable", new DbIntColumn("mainAcceptation"), new DbIntColumn("dynamicAcceptation"),
                    new DbIntColumn("strAlphabet"), new DbTextColumn("str"), new DbTextColumn("mainStr"));
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
            super("SymbolArrays", new DbUniqueTextColumn("str"));
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
        static final KnowledgeTable knowledge = new KnowledgeTable();
        static final LanguagesTable languages = new LanguagesTable();
        static final QuestionFieldSets questionFieldSets = new QuestionFieldSets();
        static final QuizDefinitionsTable quizDefinitions = new QuizDefinitionsTable();
        static final RuledAcceptationsTable ruledAcceptations = new RuledAcceptationsTable();
        static final RuledConceptsTable ruledConcepts = new RuledConceptsTable();
        static final StringQueriesTable stringQueries = new StringQueriesTable();
        static final SymbolArraysTable symbolArrays = new SymbolArraysTable();
    }

    static final String idColumnName = "id";

    private static final DbTable[] dbTables = new DbTable[18];
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
        dbTables[10] = Tables.knowledge;
        dbTables[11] = Tables.languages;
        dbTables[12] = Tables.questionFieldSets;
        dbTables[13] = Tables.quizDefinitions;
        dbTables[14] = Tables.ruledAcceptations;
        dbTables[15] = Tables.ruledConcepts;
        dbTables[16] = Tables.stringQueries;
        dbTables[17] = Tables.symbolArrays;
    }

    private void createTables(SQLiteDatabase db) {
        for (DbTable table : dbTables) {
            StringBuilder builder = new StringBuilder();
            builder.append("CREATE TABLE ")
                    .append(table.getName())
                    .append(" (");

            final int columnCount = table.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                final String columnName = table.getColumnName(i);
                if (i != 0) {
                    builder.append(", ");
                }

                builder.append(columnName).append(' ')
                        .append(table.getColumnType(i));
            }
            builder.append(')');

            db.execSQL(builder.toString());
        }

        // Temporal solution to speed up queries in the database
        final StringQueriesTable table = Tables.stringQueries;
        db.execSQL("CREATE INDEX StrDynAcc ON " + table.getName() + " (" + table.getColumnName(table.getDynamicAcceptationColumnIndex()) + ")");

        final AcceptationsTable accTable = Tables.acceptations;
        db.execSQL("CREATE INDEX AccConcept ON " + accTable.getName() + " (" + accTable.getColumnName(accTable.getConceptColumnIndex()) + ")");
    }

    private SparseIntArray readCorrelationMap(
            InputBitStream ibs, HuffmanTable<Integer> matcherSetLengthTable,
            int minAlphabet, int maxAlphabet, int[] symbolArraysIdMap) throws IOException {

        final int maxSymbolArray = symbolArraysIdMap.length - 1;
        final int mapLength = ibs.readHuffmanSymbol(matcherSetLengthTable);
        final SparseIntArray result = new SparseIntArray();
        final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(0, maxSymbolArray);
        for (int i = 0; i < mapLength; i++) {
            final RangedIntegerHuffmanTable alphabetTable = new RangedIntegerHuffmanTable(minAlphabet, maxAlphabet);
            final int alphabet = ibs.readHuffmanSymbol(alphabetTable);
            final int symbolArrayIndex = ibs.readHuffmanSymbol(symbolArrayTable);
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
                    final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(minSymbolArrayIndex, maxSymbolArrayIndex);

                    // Read languages and its alphabets
                    setProgress(0.03f, "Reading languages and its alphabets");
                    final int languageCount = ibs.readHuffmanSymbol(_naturalNumberTable);
                    final Language[] languages = new Language[languageCount];
                    final int minValidAlphabet = StreamedDatabaseConstants.minValidAlphabet;
                    int nextMinAlphabet = StreamedDatabaseConstants.minValidAlphabet;
                    final NaturalNumberHuffmanTable nat2Table = new NaturalNumberHuffmanTable(2);
                    int kanjiAlphabet = -1;
                    int kanaAlphabet = -1;

                    for (int languageIndex = 0; languageIndex < languageCount; languageIndex++) {
                        final int codeSymbolArrayIndex = ibs.readHuffmanSymbol(symbolArrayTable);
                        final int alphabetCount = ibs.readHuffmanSymbol(nat2Table);
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
                    final int minValidWord = StreamedDatabaseConstants.minValidWord;
                    final int minValidConcept = StreamedDatabaseConstants.minValidConcept;
                    final int maxWord = ibs.readHuffmanSymbol(_naturalNumberTable) - 1;
                    final int maxConcept = ibs.readHuffmanSymbol(_naturalNumberTable) - 1;

                    int[] wordIdMap = new int[maxWord + 1];
                    for (int i = 0; i <= maxWord; i++) {
                        wordIdMap[i] = i;
                    }

                    int[] conceptIdMap = new int[maxConcept + 1];
                    for (int i = 0; i <= maxConcept; i++) {
                        conceptIdMap[i] = i;
                    }

                    // Import correlations
                    setProgress(0.09f, "Reading correlations");
                    int[] correlationIdMap = readCorrelations(db, ibs, StreamedDatabaseConstants.minValidAlphabet, maxValidAlphabet, symbolArraysIdMap);

                    // Import correlation arrays
                    setProgress(0.13f, "Reading correlation arrays");
                    int[] correlationArrayIdMap = readCorrelationArrays(db, ibs, correlationIdMap);

                    // Import correlation arrays
                    setProgress(0.17f, "Reading acceptations");
                    int[] acceptationIdMap = readAcceptations(db, ibs, wordIdMap, conceptIdMap, correlationArrayIdMap);

                    // Export bunchConcepts
                    setProgress(0.21f, "Reading bunch concepts");
                    readBunchConcepts(db, ibs, minValidConcept, maxConcept);

                    // Export bunchAcceptations
                    setProgress(0.24f, "Reading bunch acceptations");
                    readBunchAcceptations(db, ibs, minValidConcept, conceptIdMap, acceptationIdMap);

                    // Export agents
                    setProgress(0.27f, "Reading agents");
                    SparseArray<Agent> agents = readAgents(db, ibs, maxConcept, correlationIdMap);

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
    private boolean applyAgent(SQLiteDatabase db, int agentId, AgentSetSupplier agentSetSupplier, int accId, int concept, int suggestedNewWordId, int targetBunch, SparseArray<String> matcher, SparseArray<String> adder, int rule, SparseArray<String> corr, int flags) {
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

                final int newConcept = obtainRuledConcept(db, agentId, concept);
                final int resultCorrLength = resultCorr.size();
                final SparseIntArray resultCorrIds = new SparseIntArray(resultCorrLength);
                for (int i = 0; i < resultCorrLength; i++) {
                    final int alphabet = resultCorr.keyAt(i);
                    resultCorrIds.put(alphabet, obtainSymbolArray(db, resultCorr.valueAt(i)));
                }

                final int corrId = obtainCorrelation(db, resultCorrIds);
                final int corrArrayId = insertCorrelationArray(db, corrId);
                suggestedNewWordUsed = true;
                final int dynAccId = insertAcceptation(db, suggestedNewWordId, newConcept, corrArrayId);
                insertRuledAcceptation(db, dynAccId, agentId, accId);

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
                            if (applyAgent(db, agentId, agentSetSupplier, accId, concept, maxWord + 1, targetBunch, matcher, adder, rule, corr, flags)) {
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

                    applyAgent(db, agentId, agentSetSupplier, accId, concept, maxWord +1, targetBunch, matcher, adder, rule, corr, flags);
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
