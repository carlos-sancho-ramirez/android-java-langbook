package sword.langbook3.android;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseIntArray;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import sword.bitstream.FunctionWithIOException;
import sword.bitstream.HuffmanTable;
import sword.bitstream.InputBitStream;
import sword.bitstream.NaturalNumberHuffmanTable;
import sword.bitstream.SupplierWithIOException;

class DbManager extends SQLiteOpenHelper {

    private static final String DB_NAME = "Langbook";
    private static final int DB_VERSION = 5;

    private final Context _context;

    DbManager(Context context) {
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

    private void insertConversion(SQLiteDatabase db, int sourceAlphabet, int targetAlphabet, int source, int target) {
        final ConversionsTable table = Tables.conversions;
        db.execSQL("INSERT INTO " + table.getName() + " (" +
                table.getColumnName(table.getSourceAlphabetColumnIndex()) + ", " +
                table.getColumnName(table.getTargetAlphabetColumnIndex()) + ", " +
                table.getColumnName(table.getSourceColumnIndex()) + ", " +
                table.getColumnName(table.getTargetColumnIndex()) + ") VALUES (" +
                sourceAlphabet + ',' + targetAlphabet + ',' + source + ',' + target + ')');
    }

    private int insertCorrelation(SQLiteDatabase db, SparseIntArray correlation) {
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

    private void insertAgent(SQLiteDatabase db, int targetBunch, int sourceBunchSetId,
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
    }

    private void insertBunchAcceptation(SQLiteDatabase db, int bunch, int acceptation) {
        final BunchAcceptationsTable table = Tables.bunchAcceptations;
        db.execSQL("INSERT INTO " + table.getName() + " (" +
                table.getColumnName(table.getBunchColumnIndex()) + ", " +
                table.getColumnName(table.getAcceptationColumnIndex()) + ") VALUES (" +
                bunch + ',' + acceptation + ')');
    }

    private void insertBunchConcept(SQLiteDatabase db, int bunch, int concept) {
        final BunchConceptsTable table = Tables.bunchConcepts;
        db.execSQL("INSERT INTO " + table.getName() + " (" +
                table.getColumnName(table.getBunchColumnIndex()) + ", " +
                table.getColumnName(table.getConceptColumnIndex()) + ") VALUES (" +
                bunch + ',' + concept + ')');
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
        for (int i = 0; i < bunchAcceptationsLength; i++) {
            final int bunch = ibs.readRangedNumber(minValidConcept, maxConcept);
            final Set<Integer> acceptations = ibs.readRangedNumberSet(bunchAcceptationsLengthTable, 0, maxValidAcceptation);
            for (int acceptation : acceptations) {
                insertBunchAcceptation(db, bunch, acceptationsIdMap[acceptation]);
            }
        }
    }

    private void readAgents(
            SQLiteDatabase db, InputBitStream ibs, int maxConcept,
            int minValidAlphabet, int maxValidAlphabet,
            int minSymbolArrayIndex, int maxSymbolArrayIndex) throws IOException {

        final int agentsLength = (int) ibs.readNaturalNumber();
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
                        minValidAlphabet, maxValidAlphabet, minSymbolArrayIndex, maxSymbolArrayIndex);
                final SparseIntArray adder = readCorrelationMap(ibs, matcherSetLengthTable,
                        minValidAlphabet, maxValidAlphabet, minSymbolArrayIndex, maxSymbolArrayIndex);

                final int rule = (adder.size() > 0)?
                        ibs.readRangedNumber(StreamedDatabaseConstants.minValidConcept, maxConcept) :
                        StreamedDatabaseConstants.nullBunchId;

                final boolean fromStart = (matcher.size() > 0 || adder.size() > 0) && ibs.readBoolean();
                final int flags = fromStart? 1 : 0;

                final int sourceBunchSetId = insertBunchSet(db, sourceSet);
                final int diffBunchSetId = 0;

                final int matcherId = insertCorrelation(db, matcher);
                final int adderId = insertCorrelation(db, adder);
                insertAgent(db, targetBunch, sourceBunchSetId, diffBunchSetId, matcherId, adderId, rule, flags);

                lastTarget = targetBunch;
            }
        }
    }

    private static final class StreamedDatabaseConstants {

        /** Reserved for agents for null references */
        static final int nullBunchId = 0;

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

    static final class SymbolArraysTable extends DbTable {

        SymbolArraysTable() {
            super("SymbolArrays", null, new String[] {"str"});
        }

        int getStrColumnIndex() {
            return 1;
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
    }

    static final class BunchAcceptationsTable extends DbTable {

        BunchAcceptationsTable() {
            super("BunchAcceptations", new String[] {"bunch", "acceptation"}, null);
        }

        int getBunchColumnIndex() {
            return 1;
        }

        int getAcceptationColumnIndex() {
            return 2;
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

    static final class Tables {
        static final AcceptationsTable acceptations = new AcceptationsTable();
        static final AgentsTable agents = new AgentsTable();
        static final BunchAcceptationsTable bunchAcceptations = new BunchAcceptationsTable();
        static final BunchConceptsTable bunchConcepts = new BunchConceptsTable();
        static final BunchSetsTable bunchSets = new BunchSetsTable();
        static final ConversionsTable conversions = new ConversionsTable();
        static final CorrelationsTable correlations = new CorrelationsTable();
        static final CorrelationArraysTable correlationArrays = new CorrelationArraysTable();
        static final SymbolArraysTable symbolArrays = new SymbolArraysTable();
    }

    private static final String idColumnName = "id";

    private static final DbTable[] dbTables = new DbTable[9];
    static {
        dbTables[0] = Tables.acceptations;
        dbTables[1] = Tables.agents;
        dbTables[2] = Tables.bunchAcceptations;
        dbTables[3] = Tables.bunchConcepts;
        dbTables[4] = Tables.bunchSets;
        dbTables[5] = Tables.conversions;
        dbTables[6] = Tables.correlations;
        dbTables[7] = Tables.correlationArrays;
        dbTables[8] = Tables.symbolArrays;
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
            int minAlphabet, int maxAlphabet,
            int minSymbolArray, int maxSymbolArray) throws IOException {

        final int mapLength = ibs.readHuffmanSymbol(matcherSetLengthTable);
        final SparseIntArray result = new SparseIntArray();
        for (int i = 0; i < mapLength; i++) {
            final int alphabet = ibs.readRangedNumber(minAlphabet, maxAlphabet);
            final int symbolArrayIndex = ibs.readRangedNumber(minSymbolArray, maxSymbolArray);
            minAlphabet = alphabet + 1;
            result.put(alphabet, symbolArrayIndex);
        }

        return result;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);

        final InputStream is = _context.getResources().openRawResource(R.raw.basic);
        try {
            is.skip(20);
            final InputBitStream ibs = new InputBitStream(is);
            final int[] symbolArraysIdMap = readSymbolArrays(db, ibs);
            final int minSymbolArrayIndex = 0;
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

            final int minValidAcceptation = 0;
            final int maxValidAcceptation = acceptationsIdMap.length - 1;

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
            readBunchConcepts(db, ibs, minValidConcept, maxConcept);

            // Export bunchAcceptations
            readBunchAcceptations(db, ibs, minValidConcept, maxConcept, acceptationsIdMap);

            // Export agents
            readAgents(db, ibs, maxConcept, minValidAlphabet, maxValidAlphabet, minSymbolArrayIndex, maxSymbolArrayIndex);
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
