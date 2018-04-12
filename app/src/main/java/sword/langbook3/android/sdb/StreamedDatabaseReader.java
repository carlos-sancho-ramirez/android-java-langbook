package sword.langbook3.android.sdb;

import android.util.SparseArray;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.MutableIntPairMap;
import sword.langbook3.android.EqualUtils;
import sword.langbook3.android.LangbookDbSchema;
import sword.langbook3.android.db.DbInitializer.Database;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbTable;

import static sword.langbook3.android.db.DbQuery.concat;

public final class StreamedDatabaseReader {

    static final NaturalNumberHuffmanTable naturalNumberTable = new NaturalNumberHuffmanTable(8);
    private final Database _db;
    private final InputStream _is;
    private final ProgressListener _listener;

    public StreamedDatabaseReader(Database db, InputStream is, ProgressListener listener) {
        _db = db;
        _is = is;
        _listener = listener;
    }

    private void setProgress(float progress, String message) {
        if (_listener != null) {
            _listener.setProgress(progress, message);
        }
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
            return _ibs.readHuffmanSymbol(naturalNumberTable);
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
            return _ibs.readHuffmanSymbol(naturalNumberTable) + previous + 1;
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

    private Set<Integer> readRangedNumberSet(InputBitStream ibs, HuffmanTable<Integer> lengthTable, int min, int max) throws IOException {
        final RangedIntegerSetDecoder decoder = new RangedIntegerSetDecoder(ibs, lengthTable, min, max);
        return ibs.readSet(decoder, decoder, decoder);
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

    private static class AgentBunches {

        private final int _target;
        private final Set<Integer> _sources;
        private final Set<Integer> _diff;

        AgentBunches(int target, Set<Integer> sources, Set<Integer> diff) {
            _target = target;
            _sources = sources;
            _diff = diff;
        }

        boolean dependsOn(AgentBunches agent) {
            final int target = agent._target;
            return _sources.contains(target) || _diff.contains(target);
        }
    }

    private int getMaxAgentSetId() {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
        return getColumnMax(table, table.getSetIdColumnIndex());
    }

    private int insertAgentSet(Set<Integer> agents) {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;

        if (agents.isEmpty()) {
            return table.nullReference();
        }
        else {
            final int setId = getMaxAgentSetId() + 1;
            for (int agent : agents) {
                final DbInsertQuery query = new DbInsertQuery.Builder(table)
                        .put(table.getSetIdColumnIndex(), setId)
                        .put(table.getAgentColumnIndex(), agent)
                        .build();
                _db.insert(query);
            }

            return setId;
        }
    }

    private class AgentSetSupplier {

        private final int _agentId;
        private boolean _created;
        private int _agentSetId;

        AgentSetSupplier(int agentId) {
            _agentId = agentId;
        }

        private int get() {
            if (!_created) {
                final Set<Integer> set = new HashSet<>();
                set.add(_agentId);
                _agentSetId = insertAgentSet(set);
                _created = true;
            }

            return _agentSetId;
        }
    }

    private int getColumnMax(DbTable table, int columnIndex) {
        final DbQuery query = new DbQuery.Builder(table)
                .select(DbQuery.max(columnIndex));
        final DbResult result = _db.select(query);
        try {
            return result.hasNext()? result.next().get(0).toInt() : 0;
        }
        finally {
            result.close();
        }
    }

    private String getSymbolArray(int id) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .select(table.getStrColumnIndex());
        final DbResult result = _db.select(query);
        try {
            final String str = result.hasNext()? result.next().get(0).toText() : null;
            if (result.hasNext()) {
                throw new AssertionError("There should not be repeated identifiers");
            }
            return str;
        }
        finally {
            result.close();
        }
    }

    private Integer findSymbolArray(String str) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStrColumnIndex(), str)
                .select(table.getIdColumnIndex());
        final DbResult result = _db.select(query);
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

    private Integer insertSymbolArray(String str) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getStrColumnIndex(), str)
                .build();
        return _db.insert(query);
    }

    private int obtainSymbolArray(String str) {
        Integer id = insertSymbolArray(str);
        if (id != null) {
            return id;
        }

        id = findSymbolArray(str);
        if (id == null) {
            throw new AssertionError("Unable to insert, and not present");
        }

        return id;
    }

    private void insertAlphabet(int id, int language) {
        final LangbookDbSchema.AlphabetsTable table = LangbookDbSchema.Tables.alphabets;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getLanguageColumnIndex(), language)
                .build();
        _db.insert(query);
    }

    private void insertLanguage(int id, String code, int mainAlphabet) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getCodeColumnIndex(), code)
                .put(table.getMainAlphabetColumnIndex(), mainAlphabet)
                .build();
        _db.insert(query);
    }

    private void insertConversion(int sourceAlphabet, int targetAlphabet, int source, int target) {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getSourceAlphabetColumnIndex(), sourceAlphabet)
                .put(table.getTargetAlphabetColumnIndex(), targetAlphabet)
                .put(table.getSourceColumnIndex(), source)
                .put(table.getTargetColumnIndex(), target)
                .build();
        _db.insert(query);
    }

    private ImmutableIntPairMap getCorrelation(int id) {
        LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getCorrelationIdColumnIndex(), id)
                .select(table.getAlphabetColumnIndex(), table.getSymbolArrayColumnIndex());

        ImmutableIntPairMap.Builder corrBuilder = new ImmutableIntPairMap.Builder();
        final DbResult result = _db.select(query);
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                corrBuilder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }
        finally {
            result.close();
        }

        return corrBuilder.build();
    }

    private Integer findCorrelation(IntPairMap correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAlphabetColumnIndex(), correlation.keyAt(0))
                .where(table.getSymbolArrayColumnIndex(), correlation.valueAt(0))
                .select(table.getCorrelationIdColumnIndex());
        final DbResult result = _db.select(query);
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

    private int getMaxCorrelationId() {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        return getColumnMax(table, table.getCorrelationIdColumnIndex());
    }

    private int insertCorrelation(IntPairMap correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }

        final int newCorrelationId = getMaxCorrelationId() + 1;
        final int mapLength = correlation.size();

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        for (int i = 0; i < mapLength; i++) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getCorrelationIdColumnIndex(), newCorrelationId)
                    .put(table.getAlphabetColumnIndex(), correlation.keyAt(i))
                    .put(table.getSymbolArrayColumnIndex(), correlation.valueAt(i))
                    .build();
            _db.insert(query);
        }

        return newCorrelationId;
    }

    private int obtainCorrelation(IntPairMap correlation) {
        final Integer id = findCorrelation(correlation);
        if (id == null) {
            return insertCorrelation(correlation);
        }

        return id;
    }

    private int[] getCorrelationArray(int id) {
        if (id == StreamedDatabaseConstants.nullCorrelationArrayId) {
            return new int[0];
        }

        LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getArrayIdColumnIndex(), id)
                .select(table.getArrayPositionColumnIndex(), table.getCorrelationColumnIndex());
        final DbResult dbResult = _db.select(query);
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

    private Integer findCorrelationArray(int... correlations) {
        if (correlations.length == 0) {
            return StreamedDatabaseConstants.nullCorrelationArrayId;
        }

        LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), correlations[0])
                .select(table.getArrayIdColumnIndex());

        final DbResult result = _db.select(query);
        try {
            while (result.hasNext()) {
                final int arrayId = result.next().get(0).toInt();
                final int[] array = getCorrelationArray(arrayId);
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

    private int getMaxCorrelationArrayId() {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        return getColumnMax(table, table.getArrayIdColumnIndex());
    }

    private int insertCorrelationArray(int... correlation) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final int maxArrayId = getMaxCorrelationArrayId();
        final int newArrayId = maxArrayId + ((maxArrayId + 1 != StreamedDatabaseConstants.nullCorrelationArrayId)? 1 : 2);
        final int arrayLength = correlation.length;
        for (int i = 0; i < arrayLength; i++) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getArrayIdColumnIndex(), newArrayId)
                    .put(table.getArrayPositionColumnIndex(), i)
                    .put(table.getCorrelationColumnIndex(), correlation[i])
                    .build();
            _db.insert(query);
        }

        return newArrayId;
    }

    private int obtainCorrelationArray(int... array) {
        final Integer id = findCorrelationArray(array);
        return (id == null)? insertCorrelationArray(array) : id;
    }

    private int getMaxWord() {
        LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        return getColumnMax(table, table.getWordColumnIndex());
    }

    private int getMaxConcept() {
        LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        return getColumnMax(table, table.getConceptColumnIndex());
    }

    private int insertAcceptation(int word, int concept, int correlationArray) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getWordColumnIndex(), word)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getCorrelationArrayColumnIndex(), correlationArray)
                .build();
        return _db.insert(query);
    }

    private void insertBunchConcept(int bunch, int concept) {
        final LangbookDbSchema.BunchConceptsTable table = LangbookDbSchema.Tables.bunchConcepts;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getConceptColumnIndex(), concept)
                .build();
        _db.insert(query);
    }

    private void insertBunchAcceptation(int bunch, int acceptation, int agentSet) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getAcceptationColumnIndex(), acceptation)
                .put(table.getAgentSetColumnIndex(), agentSet)
                .build();
        _db.insert(query);
    }

    private int getMaxBunchSetId() {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        return getColumnMax(table, table.getSetIdColumnIndex());
    }

    private Integer findBunchSet(Set<Integer> bunches) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        if (bunches.isEmpty()) {
            return table.nullReference();
        }

        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getBunchColumnIndex(), bunches.iterator().next())
                .select(table.getSetIdColumnIndex(), table.columns().size() + table.getBunchColumnIndex());
        final DbResult result = _db.select(query);
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

    private int insertBunchSet(int setId, Set<Integer> bunches) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;

        if (bunches.isEmpty()) {
            return table.nullReference();
        }
        else {
            for (int bunch : bunches) {
                final DbInsertQuery query = new DbInsertQuery.Builder(table)
                        .put(table.getSetIdColumnIndex(), setId)
                        .put(table.getBunchColumnIndex(), bunch)
                        .build();
                _db.insert(query);
            }

            return setId;
        }
    }

    private int obtainBunchSet(int setId, Set<Integer> bunches) {
        final Integer foundId = findBunchSet(bunches);
        return (foundId != null)? foundId : insertBunchSet(setId, bunches);
    }

    private int insertAgent(int targetBunch, int sourceBunchSetId,
            int diffBunchSetId, int matcherId, int adderId, int rule, int flags) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getTargetBunchColumnIndex(), targetBunch)
                .put(table.getSourceBunchSetColumnIndex(), sourceBunchSetId)
                .put(table.getDiffBunchSetColumnIndex(), diffBunchSetId)
                .put(table.getMatcherColumnIndex(), matcherId)
                .put(table.getAdderColumnIndex(), adderId)
                .put(table.getRuleColumnIndex(), rule)
                .put(table.getFlagsColumnIndex(), flags)
                .build();
        return _db.insert(query);
    }

    private void insertStringQuery(
            String str, String mainStr, int mainAcceptation,
            int dynAcceptation, int strAlphabet) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getStringColumnIndex(), str)
                .put(table.getMainStringColumnIndex(), mainStr)
                .put(table.getMainAcceptationColumnIndex(), mainAcceptation)
                .put(table.getDynamicAcceptationColumnIndex(), dynAcceptation)
                .put(table.getStringAlphabetColumnIndex(), strAlphabet)
                .build();
        _db.insert(query);
    }

    private void insertRuledAcceptation(int ruledAcceptation, int agent, int acceptation) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), ruledAcceptation)
                .put(table.getAgentColumnIndex(), agent)
                .put(table.getAcceptationColumnIndex(), acceptation)
                .build();
        _db.insert(query);
    }

    private Integer findRuledConcept(int agent, int concept) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agent)
                .where(table.getConceptColumnIndex(), concept)
                .select(table.getIdColumnIndex());
        final DbResult result = _db.select(query);
        try {
            final Integer id = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError("There should not be repeated ruled concepts");
            }
            return id;
        }
        finally {
            result.close();
        }
    }

    private int insertRuledConcept(int agent, int concept) {
        final int ruledConcept = getMaxConcept() + 1;
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), ruledConcept)
                .put(table.getAgentColumnIndex(), agent)
                .put(table.getConceptColumnIndex(), concept)
                .build();
        return _db.insert(query);
    }

    private int obtainRuledConcept(int agent, int concept) {
        final Integer id = findRuledConcept(agent, concept);
        if (id != null) {
            return id;
        }

        return insertRuledConcept(agent, concept);
    }

    private int[] readSymbolArrays(InputBitStream ibs) throws IOException {
        final int symbolArraysLength = ibs.readHuffmanSymbol(naturalNumberTable);
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

            idMap[index] = obtainSymbolArray(builder.toString());
        }

        return idMap;
    }

    private Conversion[] readConversions(InputBitStream ibs, int minValidAlphabet, int maxValidAlphabet, int minSymbolArrayIndex, int maxSymbolArrayIndex, int[] symbolArraysIdMap) throws IOException {
        final int conversionsLength = ibs.readHuffmanSymbol(naturalNumberTable);
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

            final int pairCount = ibs.readHuffmanSymbol(naturalNumberTable);
            final String[] sources = new String[pairCount];
            final String[] targets = new String[pairCount];
            for (int j = 0; j < pairCount; j++) {
                final int source = symbolArraysIdMap[ibs.readHuffmanSymbol(symbolArrayTable)];
                final int target = symbolArraysIdMap[ibs.readHuffmanSymbol(symbolArrayTable)];
                insertConversion(sourceAlphabet, targetAlphabet, source, target);

                sources[j] = getSymbolArray(source);
                targets[j] = getSymbolArray(target);
            }

            conversions[i] = new Conversion(sourceAlphabet, targetAlphabet, sources, targets);
        }

        return conversions;
    }

    private int[] readCorrelations(InputBitStream ibs, int minAlphabet, int maxAlphabet, int[] symbolArraysIdMap) throws IOException {
        final int correlationsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final int[] result = new int[correlationsLength];
        if (correlationsLength > 0) {
            final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(0, symbolArraysIdMap.length - 1);
            final IntegerDecoder intDecoder = new IntegerDecoder(ibs);
            final HuffmanTable<Integer> lengthTable = ibs.readHuffmanTable(intDecoder, intDecoder);
            final RangedIntegerSetDecoder keyDecoder = new RangedIntegerSetDecoder(ibs, lengthTable, minAlphabet, maxAlphabet);
            final ValueDecoder<Integer> valueDecoder = new ValueDecoder<>(ibs, symbolArrayTable);

            for (int i = 0; i < correlationsLength; i++) {
                Map<Integer, Integer> corrMap = ibs.readMap(keyDecoder, keyDecoder, keyDecoder, valueDecoder);
                MutableIntPairMap corr = MutableIntPairMap.empty();
                for (Map.Entry<Integer, Integer> entry : corrMap.entrySet()) {
                    corr.put(entry.getKey(), symbolArraysIdMap[entry.getValue()]);
                }
                result[i] = obtainCorrelation(corr);
            }
        }

        return result;
    }

    private int[] readCorrelationArrays(InputBitStream ibs, int[] correlationIdMap) throws IOException {
        final int arraysLength = ibs.readHuffmanSymbol(naturalNumberTable);
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
                result[i] = obtainCorrelationArray(corrArray);
            }
        }

        return result;
    }

    private int[] readAcceptations(InputBitStream ibs, int[] wordIdMap, int[] conceptIdMap, int[] correlationArrayIdMap) throws IOException {
        final int acceptationsLength = ibs.readHuffmanSymbol(naturalNumberTable);

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
                    acceptationsIdMap[i] = insertAcceptation(word, concept, correlationArrayIdMap[corrArray]);
                }
            }
        }

        return acceptationsIdMap;
    }

    private void readBunchConcepts(InputBitStream ibs, ImmutableIntRange validConcepts) throws IOException {
        final int bunchConceptsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final HuffmanTable<Integer> bunchConceptsLengthTable = (bunchConceptsLength > 0)?
                ibs.readHuffmanTable(new IntReader(ibs), new IntDiffReader(ibs)) : null;

        int remainingBunches = bunchConceptsLength;
        int minBunchConcept = validConcepts.min();
        for (int i = 0; i < bunchConceptsLength; i++) {
            final RangedIntegerHuffmanTable bunchTable = new RangedIntegerHuffmanTable(minBunchConcept, validConcepts.max() - remainingBunches + 1);
            final int bunch = ibs.readHuffmanSymbol(bunchTable);
            minBunchConcept = bunch + 1;
            --remainingBunches;

            final Set<Integer> concepts = readRangedNumberSet(ibs, bunchConceptsLengthTable, validConcepts.min(), validConcepts.max());
            for (int concept : concepts) {
                insertBunchConcept(bunch, concept);
            }
        }
    }

    private void readBunchAcceptations(InputBitStream ibs, ImmutableIntRange validConcepts, int[] acceptationsIdMap) throws IOException {
        final int bunchAcceptationsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final HuffmanTable<Integer> bunchAcceptationsLengthTable = (bunchAcceptationsLength > 0)?
                ibs.readHuffmanTable(new IntReader(ibs), new IntDiffReader(ibs)) : null;

        final int maxValidAcceptation = acceptationsIdMap.length - 1;
        final int nullAgentSet = LangbookDbSchema.Tables.agentSets.nullReference();

        int minValidBunch = validConcepts.min();
        int maxValidBunch = validConcepts.max() - bunchAcceptationsLength + 1;
        for (int i = 0; i < bunchAcceptationsLength; i++) {
            final RangedIntegerHuffmanTable table = new RangedIntegerHuffmanTable(minValidBunch, maxValidBunch);
            final int bunch = ibs.readHuffmanSymbol(table);
            minValidBunch = bunch + 1;
            ++maxValidBunch;

            final Set<Integer> acceptations = readRangedNumberSet(ibs, bunchAcceptationsLengthTable, 0, maxValidAcceptation);
            for (int acceptation : acceptations) {
                insertBunchAcceptation(bunch, acceptationsIdMap[acceptation], nullAgentSet);
            }
        }
    }

    private ImmutableIntKeyMap<AgentBunches> readAgents(
            InputBitStream ibs, int maxConcept, int[] correlationIdMap) throws IOException {

        final int agentsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final ImmutableIntKeyMap.Builder<AgentBunches> builder = new ImmutableIntKeyMap.Builder<>();

        if (agentsLength > 0) {
            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final IntHuffmanSymbolReader intHuffmanSymbolReader = new IntHuffmanSymbolReader(ibs, nat3Table);
            final HuffmanTable<Integer> sourceSetLengthTable = ibs.readHuffmanTable(intHuffmanSymbolReader, null);

            int lastTarget = StreamedDatabaseConstants.nullBunchId;
            int minSource = StreamedDatabaseConstants.minValidConcept;
            int desiredSetId = getMaxBunchSetId() + 1;
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
                final ImmutableIntPairMap adder = getCorrelation(adderId);

                final boolean adderNonEmpty = adder.size() > 0;
                final int rule = adderNonEmpty?
                        ibs.readHuffmanSymbol(conceptTable) :
                        StreamedDatabaseConstants.nullRuleId;

                final boolean fromStart = (adderNonEmpty || getCorrelation(matcherId).size() > 0) && ibs.readBoolean();
                final int flags = fromStart? 1 : 0;

                final Integer reusedBunchSetId = insertedBunchSets.get(sourceSet);
                final int sourceBunchSetId;
                if (reusedBunchSetId != null) {
                    sourceBunchSetId = reusedBunchSetId;
                }
                else {
                    sourceBunchSetId = obtainBunchSet(desiredSetId, sourceSet);
                    if (sourceBunchSetId == desiredSetId) {
                        ++desiredSetId;
                    }
                    insertedBunchSets.put(sourceSet, sourceBunchSetId);
                }

                final int diffBunchSetId = 0;

                if (rule != StreamedDatabaseConstants.nullRuleId && matcherId == adderId) {
                    throw new AssertionError("When rule is provided, modification is expected, but matcher and adder are the same");
                }

                final int agentId = insertAgent(targetBunch, sourceBunchSetId, diffBunchSetId, matcherId, adderId, rule, flags);

                final Set<Integer> diffSet = Collections.emptySet();
                builder.put(agentId, new AgentBunches(targetBunch, sourceSet, diffSet));

                lastTarget = targetBunch;
            }
        }

        return builder.build();
    }

    private void fillSearchQueryTable() {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations; // J0
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = LangbookDbSchema.Tables.correlationArrays; // J1
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations; // J2
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays; // J3
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.LanguagesTable languages = LangbookDbSchema.Tables.languages;

        final int corrArrayOffset = acceptations.columns().size();
        final int corrOffset = corrArrayOffset + correlationArrays.columns().size();
        final int symbolArrayOffset = corrOffset + correlations.columns().size();
        final int alphabetsOffset = symbolArrayOffset + symbolArrays.columns().size();
        final int langOffset = alphabetsOffset + alphabets.columns().size();
        final int corrOffset2 = langOffset + languages.columns().size();
        final int symbolArrayOffset2 = corrOffset2 + correlations.columns().size();

        final DbQuery innerQuery = new DbQuery.Builder(acceptations)
                .join(correlationArrays, acceptations.getCorrelationArrayColumnIndex(), correlationArrays.getArrayIdColumnIndex())
                .join(correlations, corrArrayOffset + correlationArrays.getCorrelationColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbolArrays, corrOffset + correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .join(alphabets, corrOffset + correlations.getAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .join(languages, alphabetsOffset + alphabets.getLanguageColumnIndex(), languages.getIdColumnIndex())
                .join(correlations, corrArrayOffset + correlationArrays.getCorrelationColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbolArrays, corrOffset2 + correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .whereColumnValueMatch(langOffset + languages.getMainAlphabetColumnIndex(), corrOffset2 + correlations.getAlphabetColumnIndex())
                .orderBy(
                        acceptations.getIdColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex(),
                        corrArrayOffset + correlationArrays.getArrayPositionColumnIndex())
                .select(
                        acceptations.getIdColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex(),
                        symbolArrayOffset2 + symbolArrays.getStrColumnIndex(),
                        symbolArrayOffset + symbolArrays.getStrColumnIndex());

        final DbQuery query = new DbQuery.Builder(innerQuery)
                .groupBy(0, 1)
                .select(0, 1, concat(3), concat(2));

        final DbResult result = _db.select(query);
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int accId = row.get(0).toInt();
                final int alphabet = row.get(1).toInt();
                final String str = row.get(2).toText();
                final String mainStr = row.get(3).toText();

                // TODO: Change this to point to the dynamic acceptation in Japanese. More JOINS are required whenever agents are applied
                final int dynAccId = accId;

                insertStringQuery(str, mainStr, accId, dynAccId, alphabet);
            }
        }
        finally {
            result.close();
        }
    }

    private int[] sortAgents(IntKeyMap<AgentBunches> agents) {
        final int agentCount = agents.size();
        int[] ids = new int[agentCount];
        if (agentCount == 0) {
            return ids;
        }

        AgentBunches[] result = new AgentBunches[agentCount];

        for (int i = 0; i < agentCount; i++) {
            ids[i] = agents.keyAt(i);
            result[i] = agents.valueAt(i);
        }

        int index = agentCount;
        do {
            final AgentBunches agent = result[--index];

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

                AgentBunches temp = result[firstDependency];
                result[firstDependency] = result[index];
                result[index++] = temp;
            }
        } while(index > 0);

        return ids;
    }

    private SparseArray<String> readCorrelation(int agentId, int agentColumn) {
        LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;

        final int corrTableOffset = agents.columns().size();
        final int symArrayTableOffset = corrTableOffset + correlations.columns().size();
        final DbQuery query = new DbQuery.Builder(agents)
                .join(correlations, agentColumn, correlations.getCorrelationIdColumnIndex())
                .join(symbolArrays, corrTableOffset + correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(agents.getIdColumnIndex(), agentId)
                .select(corrTableOffset + correlations.getAlphabetColumnIndex(), symArrayTableOffset + symbolArrays.getStrColumnIndex());

        final DbResult result = _db.select(query);
        final SparseArray<String> matcher = new SparseArray<>(result.getRemainingRows());
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int alphabet = row.get(0).toInt();
                final String str = row.get(1).toText();
                matcher.put(alphabet, str);
            }
        }
        finally {
            result.close();
        }

        return matcher;
    }

    private boolean agentFromStart(int flags) {
        return (flags & 1) != 0;
    }

    /**
     * @return True if the suggestedNewWordId has been used.
     */
    private boolean applyAgent(int agentId, AgentSetSupplier agentSetSupplier,
            int accId, int concept, int suggestedNewWordId, int targetBunch,
            SparseArray<String> matcher, SparseArray<String> adder, int rule,
            SparseArray<String> corr, int flags) {
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

                final int newConcept = obtainRuledConcept(agentId, concept);
                final int resultCorrLength = resultCorr.size();
                final MutableIntPairMap resultCorrIds = MutableIntPairMap.empty();
                for (int i = 0; i < resultCorrLength; i++) {
                    final int alphabet = resultCorr.keyAt(i);
                    resultCorrIds.put(alphabet, obtainSymbolArray(resultCorr.valueAt(i)));
                }

                final int corrId = obtainCorrelation(resultCorrIds);
                final int corrArrayId = insertCorrelationArray(corrId);
                suggestedNewWordUsed = true;
                final int dynAccId = insertAcceptation(suggestedNewWordId, newConcept, corrArrayId);
                insertRuledAcceptation(dynAccId, agentId, accId);

                for (int i = 0; i < resultCorrLength; i++) {
                    insertStringQuery(resultCorr.valueAt(i), resultCorr.valueAt(0), accId, dynAccId, resultCorr.keyAt(i));
                }

                targetAccId = dynAccId;
            }

            if (targetBunch != StreamedDatabaseConstants.nullBunchId) {
                insertBunchAcceptation(targetBunch, targetAccId, agentSetSupplier.get());
            }
        }

        return suggestedNewWordUsed;
    }

    private void runAgent(int agentId) {
        LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        LangbookDbSchema.BunchAcceptationsTable bunchAccs = LangbookDbSchema.Tables.bunchAcceptations;
        LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        int maxWord = getMaxWord();
        final SparseArray<String> matcher = readCorrelation(agentId, agents.getMatcherColumnIndex());
        final SparseArray<String> adder = readCorrelation(agentId, agents.getAdderColumnIndex());

        final int bunchSetsOffset = agents.columns().size();
        final int bunchAccsOffset = bunchSetsOffset + bunchSets.columns().size();
        final int stringsOffset = bunchAccsOffset + bunchAccs.columns().size();
        final int acceptationsOffset = stringsOffset + strings.columns().size();

        // TODO: This query does not manage the case where sourceSet is null
        // TODO: This query does not manage the case where diff is different from null
        final DbQuery query = new DbQuery.Builder(agents)
                .join(bunchSets, agents.getSourceBunchSetColumnIndex(), bunchSets.getSetIdColumnIndex())
                .join(bunchAccs, bunchSetsOffset + bunchSets.getBunchColumnIndex(), bunchAccs.getBunchColumnIndex())
                .join(strings, bunchAccsOffset + bunchAccs.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(acceptations, bunchAccsOffset + bunchAccs.getAcceptationColumnIndex(), acceptations.getIdColumnIndex())
                .where(agents.getIdColumnIndex(), agentId)
                .orderBy(bunchAccsOffset + bunchAccs.getAcceptationColumnIndex(), stringsOffset + strings.getStringAlphabetColumnIndex())
                .select(
                        bunchAccsOffset + bunchAccs.getAcceptationColumnIndex(),
                        stringsOffset + strings.getStringAlphabetColumnIndex(),
                        stringsOffset + strings.getStringColumnIndex(),
                        agents.getRuleColumnIndex(),
                        agents.getFlagsColumnIndex(),
                        acceptationsOffset + acceptations.getConceptColumnIndex(),
                        agents.getTargetBunchColumnIndex());

        final DbResult result = _db.select(query);
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                final SparseArray<String> corr = new SparseArray<>();
                int accId = row.get(0).toInt();
                corr.put(row.get(1).toInt(), row.get(2).toText());
                int rule = row.get(3).toInt();
                int flags = row.get(4).toInt();
                int concept = row.get(5).toInt();
                int targetBunch = row.get(6).toInt();

                final AgentSetSupplier agentSetSupplier = new AgentSetSupplier(agentId);
                int newAccId;
                while (result.hasNext()) {
                    row = result.next();
                    newAccId = row.get(0).toInt();
                    if (newAccId != accId) {
                        if (applyAgent(agentId, agentSetSupplier, accId, concept, maxWord + 1, targetBunch,
                                matcher, adder, rule, corr, flags)) {
                            ++maxWord;
                        }

                        accId = newAccId;
                        corr.clear();
                        corr.put(row.get(1).toInt(), row.get(2).toText());
                        rule = row.get(3).toInt();
                        flags = row.get(4).toInt();
                        concept = row.get(5).toInt();
                        targetBunch = row.get(6).toInt();
                    }
                    else {
                        corr.put(row.get(1).toInt(), row.get(2).toText());
                    }
                }

                applyAgent(agentId, agentSetSupplier, accId, concept, maxWord + 1, targetBunch, matcher, adder,
                        rule, corr, flags);
            }
        }
        finally {
            result.close();
        }
    }

    private void runAgents(IntKeyMap<AgentBunches> agents) {
        final int agentCount = agents.size();
        int index = 0;
        for (int agentId : sortAgents(agents)) {
            setProgress(0.4f + ((0.8f - 0.4f) / agentCount) * index, "Running agent " + (++index) + " out of " + agentCount);
            runAgent(agentId);
        }
    }

    private void applyConversions(Conversion[] conversions) {
        for (Conversion conversion : conversions) {
            final int sourceAlphabet = conversion.getSourceAlphabet();

            final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
            final DbQuery query = new DbQuery.Builder(table)
                    .where(table.getStringAlphabetColumnIndex(), sourceAlphabet)
                    .select(
                            table.getStringColumnIndex(),
                            table.getMainStringColumnIndex(),
                            table.getMainAcceptationColumnIndex(),
                            table.getDynamicAcceptationColumnIndex());

            final DbResult result = _db.select(query);
            try {
                while (result.hasNext()) {
                    final DbResult.Row row = result.next();
                    final String str = conversion.convert(row.get(0).toText());
                    if (str != null) {
                        final String mainStr = row.get(1).toText();
                        final int mainAcc = row.get(2).toInt();
                        final int dynAcc = row.get(3).toInt();

                        insertStringQuery(str, mainStr, mainAcc, dynAcc, conversion.getTargetAlphabet());
                    }
                }
            }
            finally {
                result.close();
            }
        }
    }

    public void read() throws IOException {
        try {
            setProgress(0, "Reading symbolArrays");
            final InputBitStream ibs = new InputBitStream(_is);
            final int[] symbolArraysIdMap = readSymbolArrays(ibs);
            final int minSymbolArrayIndex = 0;
            final int maxSymbolArrayIndex = symbolArraysIdMap.length - 1;
            final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(minSymbolArrayIndex,
                    maxSymbolArrayIndex);

            // Read languages and its alphabets
            setProgress(0.03f, "Reading languages and its alphabets");
            final int languageCount = ibs.readHuffmanSymbol(naturalNumberTable);
            final Language[] languages = new Language[languageCount];
            final int minValidAlphabet = StreamedDatabaseConstants.minValidAlphabet;
            int nextMinAlphabet = StreamedDatabaseConstants.minValidAlphabet;
            final NaturalNumberHuffmanTable nat2Table = new NaturalNumberHuffmanTable(2);

            for (int languageIndex = 0; languageIndex < languageCount; languageIndex++) {
                final int codeSymbolArrayIndex = ibs.readHuffmanSymbol(symbolArrayTable);
                final int alphabetCount = ibs.readHuffmanSymbol(nat2Table);
                final String code = getSymbolArray(symbolArraysIdMap[codeSymbolArrayIndex]);
                languages[languageIndex] = new Language(code, nextMinAlphabet, alphabetCount);

                nextMinAlphabet += alphabetCount;
            }

            final int maxValidAlphabet = nextMinAlphabet - 1;
            final int minLanguage = nextMinAlphabet;

            for (int i = minValidAlphabet; i <= maxValidAlphabet; i++) {
                for (int j = 0; j < languageCount; j++) {
                    Language lang = languages[j];
                    if (lang.containsAlphabet(i)) {
                        insertAlphabet(i, minLanguage + j);
                        break;
                    }
                }
            }

            for (int i = 0; i < languageCount; i++) {
                final Language lang = languages[i];
                insertLanguage(minLanguage + i, lang.getCode(), lang.getMainAlphabet());
            }

            // Read conversions
            setProgress(0.06f, "Reading conversions");
            final Conversion[] conversions = readConversions(ibs, minValidAlphabet, maxValidAlphabet, 0,
                    maxSymbolArrayIndex, symbolArraysIdMap);

            // Export the amount of words and concepts in order to range integers
            final int minValidConcept = StreamedDatabaseConstants.minValidConcept;
            final int maxWord = ibs.readHuffmanSymbol(naturalNumberTable) - 1;
            final int maxConcept = ibs.readHuffmanSymbol(naturalNumberTable) - 1;

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
            int[] correlationIdMap = readCorrelations(ibs, StreamedDatabaseConstants.minValidAlphabet,
                    maxValidAlphabet, symbolArraysIdMap);

            // Import correlation arrays
            setProgress(0.13f, "Reading correlation arrays");
            int[] correlationArrayIdMap = readCorrelationArrays(ibs, correlationIdMap);

            // Import correlation arrays
            setProgress(0.17f, "Reading acceptations");
            int[] acceptationIdMap = readAcceptations(ibs, wordIdMap, conceptIdMap, correlationArrayIdMap);

            // Import bunchConcepts
            setProgress(0.21f, "Reading bunch concepts");
            final ImmutableIntRange validConcepts = new ImmutableIntRange(minValidConcept, maxConcept);
            readBunchConcepts(ibs, validConcepts);

            // Import bunchAcceptations
            setProgress(0.24f, "Reading bunch acceptations");
            readBunchAcceptations(ibs, validConcepts, acceptationIdMap);

            // Import agents
            setProgress(0.27f, "Reading agents");
            ImmutableIntKeyMap<AgentBunches> agents = readAgents(ibs, maxConcept, correlationIdMap);

            // Import ruleConcepts
            if (ibs.readHuffmanSymbol(naturalNumberTable) != 0) {
                throw new UnsupportedOperationException("For now, this should always be an empty table");
            }

            setProgress(0.3f, "Indexing strings");
            fillSearchQueryTable();

            setProgress(0.4f, "Running agents");
            runAgents(agents);

            setProgress(0.8f, "Applying conversions");
            applyConversions(conversions);
        }
        finally {
            try {
                if (_is != null) {
                    _is.close();
                }
            }
            catch (IOException e) {
                // Nothing can be done
            }
        }
    }
}
