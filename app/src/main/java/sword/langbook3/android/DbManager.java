package sword.langbook3.android;

import android.content.ContentResolver;
import android.content.ContentValues;
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
import sword.langbook3.android.LangbookDbSchema.AcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.AgentSetsTable;
import sword.langbook3.android.LangbookDbSchema.AgentsTable;
import sword.langbook3.android.LangbookDbSchema.AlphabetsTable;
import sword.langbook3.android.LangbookDbSchema.BunchAcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.BunchConceptsTable;
import sword.langbook3.android.LangbookDbSchema.BunchSetsTable;
import sword.langbook3.android.LangbookDbSchema.ConversionsTable;
import sword.langbook3.android.LangbookDbSchema.CorrelationArraysTable;
import sword.langbook3.android.LangbookDbSchema.CorrelationsTable;
import sword.langbook3.android.LangbookDbSchema.LanguagesTable;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldSets;
import sword.langbook3.android.LangbookDbSchema.QuizDefinitionsTable;
import sword.langbook3.android.LangbookDbSchema.RuledAcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.RuledConceptsTable;
import sword.langbook3.android.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.LangbookDbSchema.SymbolArraysTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.db.DbColumn;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbIntValue;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbSchema;
import sword.langbook3.android.db.DbStringValue;
import sword.langbook3.android.db.DbTable;
import sword.langbook3.android.db.DbValue;

import static sword.langbook3.android.db.DbIdColumn.idColumnName;

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

    private Integer findSymbolArray(SQLiteDatabase db, String str) {
        final SymbolArraysTable table = Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStrColumnIndex(), str)
                .select(table.getIdColumnIndex());
        final SQLiteDbResult result = select(db, query);
        try {
            final Integer value = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError();
            }

            return value;
        }
        finally {
            result.close();
        }
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

    private static SQLiteDbResult select(SQLiteDatabase db, DbQuery query) {
        return new SQLiteDbResult(query.getSelectedColumns(), db.rawQuery(new SQLiteDbQuery(query).toSql(), null));
    }

    private static Integer insert(SQLiteDatabase db, DbInsertQuery query) {
        final int count = query.getColumnCount();
        ContentValues cv = new ContentValues();
        for (int i = 0; i < count; i++) {
            final String name = query.getColumn(i).getName();
            final DbValue value = query.getValue(i);
            if (value.isText()) {
                cv.put(name, value.toText());
            }
            else {
                cv.put(name, value.toInt());
            }
        }

        final long returnId = db.insert(query.getTable().getName(), null, cv);
        return (returnId >= 0)? (int) returnId : null;
    }

    public Integer insert(DbInsertQuery query) {
        return insert(getWritableDatabase(), query);
    }

    private Integer insertSymbolArray(SQLiteDatabase db, String str) {
        final SymbolArraysTable table = Tables.symbolArrays;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getStrColumnIndex(), str)
                .build();
        return insert(db, query);
    }

    private int obtainSymbolArray(SQLiteDatabase db, String str) {
        Integer id = insertSymbolArray(db, str);
        if (id != null) {
            return id;
        }

        id = findSymbolArray(db, str);
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
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getLanguageColumnIndex(), language)
                .build();
        insert(db, query);
    }

    private void insertLanguage(SQLiteDatabase db, int id, String code, int mainAlphabet) {
        final LanguagesTable table = Tables.languages;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getCodeColumnIndex(), code)
                .put(table.getMainAlphabetColumnIndex(), mainAlphabet)
                .build();
        insert(db, query);
    }

    private void insertConversion(SQLiteDatabase db, int sourceAlphabet, int targetAlphabet, int source, int target) {
        final ConversionsTable table = Tables.conversions;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getSourceAlphabetColumnIndex(), sourceAlphabet)
                .put(table.getTargetAlphabetColumnIndex(), targetAlphabet)
                .put(table.getSourceColumnIndex(), source)
                .put(table.getTargetColumnIndex(), target)
                .build();
        insert(db, query);
    }

    private static int getColumnMax(SQLiteDatabase db, DbTable table, int columnIndex) {
        final DbQuery query = new DbQuery.Builder(table)
                .select(DbQuery.max(columnIndex));
        final SQLiteDbResult result = select(db, query);
        try {
            return result.next().get(0).toInt();
        }
        finally {
            result.close();
        }
    }

    private static int getMaxWord(SQLiteDatabase db) {
        AcceptationsTable table = Tables.acceptations;
        return getColumnMax(db, table, table.getWordColumnIndex());
    }

    private static int getMaxConcept(SQLiteDatabase db) {
        AcceptationsTable table = Tables.acceptations;
        return getColumnMax(db, table, table.getConceptColumnIndex());
    }

    private SparseIntArray getCorrelation(SQLiteDatabase db, int id) {
        CorrelationsTable table = Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getCorrelationIdColumnIndex(), id)
                .select(table.getAlphabetColumnIndex(), table.getSymbolArrayColumnIndex());

        SparseIntArray correlation = new SparseIntArray();
        final DbResult result = select(db, query);
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                correlation.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }
        finally {
            result.close();
        }

        return correlation;
    }

    private Integer findCorrelation(SQLiteDatabase db, SparseIntArray correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }

        final CorrelationsTable table = Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAlphabetColumnIndex(), correlation.keyAt(0))
                .where(table.getSymbolArrayColumnIndex(), correlation.valueAt(0))
                .select(table.getCorrelationIdColumnIndex());
        final SQLiteDbResult result = select(db, query);
        try {
            final Integer value = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError();
            }
            return value;
        }
        finally {
            result.close();
        }
    }

    private static int getMaxCorrelationId(SQLiteDatabase db) {
        final CorrelationsTable table = Tables.correlations;
        return getColumnMax(db, table, table.getCorrelationIdColumnIndex());
    }

    private int insertCorrelation(SQLiteDatabase db, SparseIntArray correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }

        final int newCorrelationId = getMaxCorrelationId(db) + 1;
        final int mapLength = correlation.size();

        final CorrelationsTable table = Tables.correlations;
        for (int i = 0; i < mapLength; i++) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getCorrelationIdColumnIndex(), newCorrelationId)
                    .put(table.getAlphabetColumnIndex(), correlation.keyAt(i))
                    .put(table.getSymbolArrayColumnIndex(), correlation.valueAt(i))
                    .build();
            insert(db, query);
        }

        return newCorrelationId;
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
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getArrayIdColumnIndex(), id)
                .select(table.getArrayPositionColumnIndex(), table.getCorrelationColumnIndex());
        final DbResult dbResult = select(db, query);
        final int[] result = new int[dbResult.getRemainingRows()];
        final BitSet set = new BitSet();
        try {
            while (dbResult.hasNext()) {
                final DbResult.Row row = dbResult.next();
                final int pos = row.get(0).toInt();
                final int corr = row.get(1).toInt();
                if (set.get(pos)) {
                    throw new AssertionError("Malformed correlation array with id " + id);
                }

                set.set(pos);
                result[pos] = corr;
            }
        }
        finally {
            dbResult.close();
        }

        return result;
    }

    private Integer findCorrelationArray(SQLiteDatabase db, int... correlations) {
        if (correlations.length == 0) {
            return StreamedDatabaseConstants.nullCorrelationArrayId;
        }

        CorrelationArraysTable table = Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), correlations[0])
                .select(table.getArrayIdColumnIndex());

        final DbResult result = select(db, query);
        try {
            while (result.hasNext()) {
                final int arrayId = result.next().get(0).toInt();
                final int[] array = getCorrelationArray(db, arrayId);
                if (Arrays.equals(correlations, array)) {
                    return arrayId;
                }
            }
        }
        finally {
            result.close();
        }

        return null;
    }

    private int getMaxCorrelationArrayId(SQLiteDatabase db) {
        final CorrelationArraysTable table = Tables.correlationArrays;
        return getColumnMax(db, table, table.getArrayIdColumnIndex());
    }

    private int insertCorrelationArray(SQLiteDatabase db, int... correlation) {
        final CorrelationArraysTable table = Tables.correlationArrays;
        final int maxArrayId = getMaxCorrelationArrayId(db);
        final int newArrayId = maxArrayId + ((maxArrayId + 1 != StreamedDatabaseConstants.nullCorrelationArrayId)? 1 : 2);
        final int arrayLength = correlation.length;
        for (int i = 0; i < arrayLength; i++) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getArrayIdColumnIndex(), newArrayId)
                    .put(table.getArrayPositionColumnIndex(), i)
                    .put(table.getCorrelationColumnIndex(), correlation[i])
                    .build();
            insert(db, query);
        }

        return newArrayId;
    }

    private int obtainCorrelationArray(SQLiteDatabase db, int... array) {
        final Integer id = findCorrelationArray(db, array);
        return (id == null)? insertCorrelationArray(db, array) : id;
    }

    private int insertAcceptation(SQLiteDatabase db, int word, int concept, int correlationArray) {
        final AcceptationsTable table = Tables.acceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getWordColumnIndex(), word)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getCorrelationArrayColumnIndex(), correlationArray)
                .build();
        return insert(db, query);
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

    private int getMaxAgentSetId(SQLiteDatabase db) {
        final AgentSetsTable table = Tables.agentSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    private int insertAgentSet(SQLiteDatabase db, Set<Integer> agents) {
        final AgentSetsTable table = Tables.agentSets;

        if (agents.isEmpty()) {
            return table.nullReference();
        }
        else {
            final int setId = getMaxAgentSetId(db) + 1;
            for (int agent : agents) {
                db.execSQL("INSERT INTO " + table.getName() + " (" +
                        table.getColumnName(table.getSetIdColumnIndex()) + ", " +
                        table.getColumnName(table.getAgentColumnIndex()) + ") VALUES (" +
                        setId + ',' + agent + ')');
            }

            return setId;
        }
    }

    private static int getMaxBunchSetId(SQLiteDatabase db) {
        final BunchSetsTable table = Tables.bunchSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
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

        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getBunchColumnIndex(), bunches.iterator().next())
                .select(table.getSetIdColumnIndex(), table.getColumnCount() + table.getBunchColumnIndex());
        final DbResult result = select(db, query);
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                final HashSet<Integer> set = new HashSet<>();
                int setId = row.get(0).toInt();
                set.add(row.get(1).toInt());

                while (result.hasNext()) {
                    row = result.next();
                    if (row.get(0).toInt() != setId) {
                        if (set.equals(bunches)) {
                            return setId;
                        }

                        setId = row.get(0).toInt();
                        set.clear();
                        set.add(row.get(1).toInt());
                    }
                }

                if (set.equals(bunches)) {
                    return setId;
                }
            }
        }
        finally {
            result.close();
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
            return flags & QuestionFieldFlags.TYPE_MASK;
        }

        int getTypeStringResId() {
            switch (getType()) {
                case QuestionFieldFlags.TYPE_SAME_ACC:
                    return R.string.questionTypeSameAcceptation;

                case QuestionFieldFlags.TYPE_SAME_CONCEPT:
                    return R.string.questionTypeSameConcept;

                case QuestionFieldFlags.TYPE_APPLY_RULE:
                    return R.string.questionTypeAppliedRule;
            }

            return 0;
        }

        boolean isAnswer() {
            return (flags & QuestionFieldFlags.IS_ANSWER) != 0;
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
        final QuestionFieldSets fieldSets = Tables.questionFieldSets;
        final int columnCount = fieldSets.getColumnCount();
        final DbQuery query = new DbQuery.Builder(fieldSets)
                .join(fieldSets, fieldSets.getSetIdColumnIndex(), fieldSets.getSetIdColumnIndex())
                .where(fieldSets.getAlphabetColumnIndex(), firstField.alphabet)
                .where(fieldSets.getRuleColumnIndex(), firstField.rule)
                .where(fieldSets.getFlagsColumnIndex(), firstField.flags)
                .select(
                        fieldSets.getSetIdColumnIndex(),
                        columnCount + fieldSets.getAlphabetColumnIndex(),
                        columnCount + fieldSets.getRuleColumnIndex(),
                        columnCount + fieldSets.getFlagsColumnIndex());

        final DbResult result = select(db, query);

        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int setId = row.get(0).toInt();
                final Set<QuestionField> foundSet = new HashSet<>();
                foundSet.add(new QuestionField(row.get(1).toInt(), row.get(2).toInt(), row.get(3).toInt()));

                while (result.hasNext()) {
                    row = result.next();
                    if (setId != row.get(0).toInt()) {
                        if (foundSet.equals(set)) {
                            return setId;
                        }
                        else {
                            foundSet.clear();
                            setId = row.get(0).toInt();
                        }
                    }

                    foundSet.add(new QuestionField(row.get(1).toInt(), row.get(2).toInt(), row.get(3).toInt()));
                }

                if (foundSet.equals(set)) {
                    return setId;
                }
            }
        }
        finally {
            result.close();
        }

        return null;
    }

    private static int getMaxQuestionFieldSetId(SQLiteDatabase db) {
        QuestionFieldSets table = Tables.questionFieldSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    static int insertQuestionFieldSet(SQLiteDatabase db, List<QuestionField> fields) {
        final QuestionFieldSets table = Tables.questionFieldSets;
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
        final QuizDefinitionsTable table = Tables.quizDefinitions;
        final DbResult result = getInstance().attach(new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getQuestionFieldsColumnIndex(), setId)
                .select(table.getIdColumnIndex())).iterator();
        final Integer value = result.hasNext()? result.next().get(0).toInt() : null;
        if (result.hasNext()) {
            throw new AssertionError("Only one quiz definition expected");
        }

        return value;
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
            int desiredSetId = getMaxBunchSetId(db) + 1;
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

    private static void createTables(SQLiteDatabase db, DbSchema schema) {
        for (DbTable table : schema.tables()) {
            StringBuilder builder = new StringBuilder();
            builder.append("CREATE TABLE ")
                    .append(table.getName())
                    .append(" (");

            final int columnCount = table.getColumnCount();
            for (int j = 0; j < columnCount; j++) {
                final String columnName = table.getColumnName(j);
                if (j != 0) {
                    builder.append(", ");
                }

                builder.append(columnName).append(' ')
                        .append(table.getColumnType(j));
            }
            builder.append(')');

            db.execSQL(builder.toString());
        }
    }

    private static void createIndexes(SQLiteDatabase db, DbSchema schema) {
        int i = 0;
        for (DbSchema.DbIndex index : schema.indexes()) {
            db.execSQL("CREATE INDEX I" + (i++) + " ON " + index.table.getName() + " (" + index.table.getColumnName(index.column) + ')');
        }
    }

    final class DbAttachedQuery implements Iterable<DbResult.Row> {

        private final DbQuery _query;

        private DbAttachedQuery(DbQuery query) {
            _query = query;
        }

        @Override
        public SQLiteDbResult iterator() {
            return select(getReadableDatabase(), _query);
        }
    }

    /**
     * Attach the given query to this manager instance.
     * This targets a databses where to execute the query.
     */
    public DbAttachedQuery attach(DbQuery query) {
        return new DbAttachedQuery(query);
    }

    static class SQLiteDbResult implements DbResult {
        private final DbColumn[] _columns;
        private final Cursor _cursor;
        private final int _rowCount;
        private int _nextRowIndex;

        SQLiteDbResult(DbColumn[] columns, Cursor cursor) {
            _columns = columns;
            _cursor = cursor;
            _rowCount = cursor.getCount();

            if (!cursor.moveToFirst()) {
                close();
            }
        }

        @Override
        public void close() {
            _nextRowIndex = _rowCount;
            _cursor.close();
        }

        @Override
        public int getRemainingRows() {
            return _rowCount - _nextRowIndex;
        }

        @Override
        public boolean hasNext() {
            return getRemainingRows() > 0;
        }

        @Override
        public Row next() {
            if (!hasNext()) {
                throw new UnsupportedOperationException("End already reached");
            }

            final DbValue[] values = new DbValue[_columns.length];
            for (int i = 0; i < _columns.length; i++) {
                values[i] = _columns[i].isText()? new DbStringValue(_cursor.getString(i)) : new DbIntValue(_cursor.getInt(i));
            }
            Row row = new Row(values);

            if (!_cursor.moveToNext()) {
                close();
            }
            _nextRowIndex++;

            return row;
        }

        static class Row implements DbResult.Row {

            private final DbValue[] _values;

            Row(DbValue[] values) {
                _values = values;
            }

            @Override
            public DbValue get(int index) {
                return _values[index];
            }
        }
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
        final LangbookDbSchema schema = LangbookDbSchema.getInstance();
        createTables(db, schema);
        createIndexes(db, schema);

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

            final StringQueriesTable table = Tables.stringQueries;
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

    public String getDatabasePath() {
        return _context.getDatabasePath(DB_NAME).toString();
    }
}
