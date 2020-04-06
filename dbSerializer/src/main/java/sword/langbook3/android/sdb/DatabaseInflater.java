package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.InputStream;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.IntKeyMap;
import sword.collections.List;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntPairMap;
import sword.database.DbExporter;
import sword.database.DbImporter;
import sword.database.DbInsertQuery;
import sword.database.DbInserter;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbValue;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.Conversion;

import static sword.database.DbQuery.concat;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.getCorrelationWithText;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.getMaxConcept;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.insertAcceptation;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.insertBunchAcceptation;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.obtainCorrelation;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.obtainCorrelationArray;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.obtainSymbolArray;

public final class DatabaseInflater {

    private final DbImporter.Database _db;
    private final ProgressListener _listener;
    private final StreamedDatabaseReader _dbReader;

    /**
     * Prepares the reader with the given parameters.
     *
     * @param db It is assumed to be a database where all the tables and indexes has been
     *           initialized according to {@link sword.langbook3.android.db.LangbookDbSchema},
     *           but they are empty.
     * @param is Input stream for the data to be decoded.
     *           This input stream should not be in the first position of the file, but 20 bytes
     *           after, skipping the header and hash.
     * @param listener Optional callback to display in the UI the current state. This can be null.
     */
    public DatabaseInflater(DbImporter.Database db, InputStream is, ProgressListener listener) {
        _db = db;
        _listener = listener;
        _dbReader = new StreamedDatabaseReader(db, is, (listener != null)? new Listener(listener, 0.3f) : null);
    }

    private void setProgress(float progress, String message) {
        if (_listener != null) {
            _listener.setProgress(progress, message);
        }
    }

    private static List<DbValue> selectOptionalSingleRow(DbExporter.Database db, DbQuery query) {
        try (DbResult result = db.select(query)) {
            return result.hasNext()? result.next() : null;
        }
    }

    private static AgentRegister getAgentRegister(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), agentId)
                .select(table.getTargetBunchSetColumnIndex(),
                        table.getSourceBunchSetColumnIndex(),
                        table.getDiffBunchSetColumnIndex(),
                        table.getStartMatcherColumnIndex(),
                        table.getStartAdderColumnIndex(),
                        table.getEndMatcherColumnIndex(),
                        table.getEndAdderColumnIndex(),
                        table.getRuleColumnIndex());

        final List<DbValue> agentRow = selectOptionalSingleRow(db, query);
        if (agentRow != null) {
            final int targetBunchSetId = agentRow.get(0).toInt();
            final int sourceBunchSetId = agentRow.get(1).toInt();
            final int diffBunchSetId = agentRow.get(2).toInt();
            final int startMatcherId = agentRow.get(3).toInt();
            final int startAdderId = agentRow.get(4).toInt();
            final int endMatcherId = agentRow.get(5).toInt();
            final int endAdderId = agentRow.get(6).toInt();
            return new AgentRegister(targetBunchSetId, sourceBunchSetId, diffBunchSetId,
                    startMatcherId, startAdderId, endMatcherId, endAdderId, agentRow.get(7).toInt());
        }

        return null;
    }

    private static Integer findRuledConcept(DbExporter.Database db, int rule, int concept) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getRuleColumnIndex(), rule)
                .where(table.getConceptColumnIndex(), concept)
                .select(table.getIdColumnIndex());

        try (DbResult result = db.select(query)) {
            final Integer id = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError("There should not be repeated ruled concepts");
            }
            return id;
        }
    }

    private static Integer findRuledAcceptationByAgentAndBaseAcceptation(DbExporter.Database db, int agentId, int baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(ruledAccs)
                .where(ruledAccs.getAcceptationColumnIndex(), baseAcceptation)
                .where(ruledAccs.getAgentColumnIndex(), agentId)
                .select(ruledAccs.getIdColumnIndex());
        final DbResult dbResult = db.select(query);
        final Integer result = dbResult.hasNext()? dbResult.next().get(0).toInt() : null;
        if (dbResult.hasNext()) {
            throw new AssertionError();
        }

        return result;
    }

    private static void insertRuledConcept(DbInserter db, int ruledConcept, int rule, int baseConcept) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), ruledConcept)
                .put(table.getRuleColumnIndex(), rule)
                .put(table.getConceptColumnIndex(), baseConcept)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    private static int insertRuledConcept(DbImporter.Database db, int rule, int concept) {
        final int ruledConcept = getMaxConcept(db) + 1;
        insertRuledConcept(db, ruledConcept, rule, concept);
        return ruledConcept;
    }

    private static void insertRuledAcceptation(DbInserter db, int ruledAcceptation, int agent, int baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), ruledAcceptation)
                .put(table.getAgentColumnIndex(), agent)
                .put(table.getAcceptationColumnIndex(), baseAcceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    private static void insertSpan(DbInserter db, int sentenceId, ImmutableIntRange range, int dynamicAcceptation) {
        if (range == null || range.min() < 0) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getSentenceIdColumnIndex(), sentenceId)
                .put(table.getStartColumnIndex(), range.min())
                .put(table.getLengthColumnIndex(), range.size())
                .put(table.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    private static void insertStringQuery(DbInserter db, String str,
            String mainStr, int mainAcceptation, int dynAcceptation, int strAlphabet) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getStringColumnIndex(), str)
                .put(table.getMainStringColumnIndex(), mainStr)
                .put(table.getMainAcceptationColumnIndex(), mainAcceptation)
                .put(table.getDynamicAcceptationColumnIndex(), dynAcceptation)
                .put(table.getStringAlphabetColumnIndex(), strAlphabet)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    private static int obtainRuledConcept(DbImporter.Database db, int rule, int concept) {
        final Integer id = findRuledConcept(db, rule, concept);
        return (id != null)? id : insertRuledConcept(db, rule, concept);
    }

    private static Integer obtainSimpleCorrelationArray(DbImporter.Database db, int correlationId) {
        return obtainCorrelationArray(db, new ImmutableIntList.Builder().append(correlationId).build());
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
                final List<DbValue> row = result.next();
                final int accId = row.get(0).toInt();
                final int alphabet = row.get(1).toInt();
                final String str = row.get(2).toText();
                final String mainStr = row.get(3).toText();

                // TODO: Change this to point to the dynamic acceptation in Japanese. More JOINS are required whenever agents are applied
                final int dynAccId = accId;

                insertStringQuery(_db, str, mainStr, accId, dynAccId, alphabet);
            }
        }
        finally {
            result.close();
        }
    }

    private static void applyConversion(DbImporter.Database db, Conversion conversion) {
        final int sourceAlphabet = conversion.getSourceAlphabet();

        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringAlphabetColumnIndex(), sourceAlphabet)
                .select(
                        table.getStringColumnIndex(),
                        table.getMainStringColumnIndex(),
                        table.getMainAcceptationColumnIndex(),
                        table.getDynamicAcceptationColumnIndex());

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final String str = conversion.convert(row.get(0).toText());
                if (str == null) {
                    throw new AssertionError("Unable to convert word " + row.get(0).toText());
                }

                final String mainStr = row.get(1).toText();
                final int mainAcc = row.get(2).toInt();
                final int dynAcc = row.get(3).toInt();

                insertStringQuery(db, str, mainStr, mainAcc, dynAcc, conversion.getTargetAlphabet());
            }
        }
    }

    private void applyAgent(int agentId, int accId, int concept,
            ImmutableIntSet targetBunches, IntKeyMap<String> startMatcher, IntKeyMap<String> startAdder,
            IntKeyMap<String> endMatcher, IntKeyMap<String> endAdder, int rule, IntKeyMap<String> corr, int mainAcc) {
        boolean matching = true;

        final int startMatcherLength = startMatcher.size();
        for (int i = 0; matching && i < startMatcherLength; i++) {
            final int alphabet = startMatcher.keyAt(i);
            final String corrStr = corr.get(alphabet, null);
            final String matcherStr = startMatcher.valueAt(i);
            matching = corrStr != null && corrStr.startsWith(matcherStr);
        }

        final int endMatcherLength = endMatcher.size();
        for (int i = 0; matching && i < endMatcherLength; i++) {
            final int alphabet = endMatcher.keyAt(i);
            final String corrStr = corr.get(alphabet, null);
            final String matcherStr = endMatcher.valueAt(i);
            matching = corrStr != null && corrStr.endsWith(matcherStr);
        }

        matching &= !startAdder.keySet().anyMatch(key -> corr.get(key, null) == null);
        matching &= !endAdder.keySet().anyMatch(key -> corr.get(key, null) == null);

        if (matching) {
            int targetAccId = accId;
            final boolean modifyWords = !startMatcher.equals(startAdder) || !endMatcher.equals(endAdder);

            if (modifyWords) {
                final MutableIntKeyMap<String> resultCorr = MutableIntKeyMap.empty();
                final int corrLength = corr.size();
                for (int i = 0; i < corrLength; i++) {
                    final int alphabet = corr.keyAt(i);

                    final String startMatcherStr = startMatcher.get(alphabet, null);
                    final int startRemoveLength = (startMatcherStr != null) ? startMatcherStr.length() : 0;

                    final String endMatcherStr = endMatcher.get(alphabet, null);
                    final int endRemoveLength = (endMatcherStr != null) ? endMatcherStr.length() : 0;

                    final String corrStr = corr.valueAt(i);
                    final int endSubstringIndex = corrStr.length() - endRemoveLength;
                    if (endSubstringIndex > startRemoveLength) {
                        final String resultStr = corrStr.substring(startRemoveLength, corrStr.length() - endRemoveLength);
                        resultCorr.put(alphabet, resultStr);
                    }
                }

                final int startAdderLength = startAdder.size();
                for (int i = 0; i < startAdderLength; i++) {
                    final int alphabet = startAdder.keyAt(i);
                    final String prefix = startAdder.valueAt(i);
                    final String currentStr = resultCorr.get(alphabet, null);
                    final String resultStr = (currentStr != null) ? prefix + currentStr : prefix;
                    resultCorr.put(alphabet, resultStr);
                }

                final int endAdderLength = endAdder.size();
                for (int i = 0; i < endAdderLength; i++) {
                    final int alphabet = endAdder.keyAt(i);
                    final String suffix = endAdder.valueAt(i);
                    final String currentStr = resultCorr.get(alphabet, null);
                    final String resultStr = (currentStr != null) ? currentStr + suffix : suffix;
                    resultCorr.put(alphabet, resultStr);
                }

                final int newConcept = obtainRuledConcept(_db, rule, concept);
                final int resultCorrLength = resultCorr.size();
                final MutableIntPairMap resultCorrIds = MutableIntPairMap.empty();
                for (int i = 0; i < resultCorrLength; i++) {
                    final int alphabet = resultCorr.keyAt(i);
                    resultCorrIds.put(alphabet, obtainSymbolArray(_db, resultCorr.valueAt(i)));
                }

                final int corrId = obtainCorrelation(_db, resultCorrIds);
                final int corrArrayId = obtainSimpleCorrelationArray(_db, corrId);
                final int dynAccId = insertAcceptation(_db, newConcept, corrArrayId);
                insertRuledAcceptation(_db, dynAccId, agentId, accId);

                for (int i = 0; i < resultCorrLength; i++) {
                    insertStringQuery(_db, resultCorr.valueAt(i), resultCorr.valueAt(0), mainAcc, dynAccId, resultCorr.keyAt(i));
                }

                targetAccId = dynAccId;
            }

            for (int targetBunch : targetBunches) {
                insertBunchAcceptation(_db, targetBunch, targetAccId, agentId);
            }
        }
    }

    private void runAgent(int agentId) {
        LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        LangbookDbSchema.BunchAcceptationsTable bunchAccs = LangbookDbSchema.Tables.bunchAcceptations;
        LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final AgentRegister register = getAgentRegister(_db, agentId);
        final SyncCacheIntKeyNonNullValueMap<ImmutableIntKeyMap<String>> correlationCache =
                new SyncCacheIntKeyNonNullValueMap<>(id -> getCorrelationWithText(_db, id));
        final ImmutableIntKeyMap<String> startMatcher = correlationCache.get(register.startMatcherId);
        final ImmutableIntKeyMap<String> startAdder = correlationCache.get(register.startAdderId);
        final ImmutableIntKeyMap<String> endMatcher = correlationCache.get(register.endMatcherId);
        final ImmutableIntKeyMap<String> endAdder = correlationCache.get(register.endAdderId);

        final int bunchAccsOffset = bunchSets.columns().size();
        final int stringsOffset = bunchAccsOffset + bunchAccs.columns().size();
        final int acceptationsOffset = stringsOffset + strings.columns().size();

        final ImmutableIntSet targetBunches;
        if (register.targetBunchSetId == 0) {
            targetBunches = ImmutableIntArraySet.empty();
        }
        else {
            final DbQuery query = new DbQuery.Builder(bunchSets)
                    .where(bunchSets.getSetIdColumnIndex(), register.targetBunchSetId)
                    .select(bunchSets.getBunchColumnIndex());
            targetBunches = _db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
        }

        final ImmutableIntSet diffAccs;
        if (register.diffBunchSetId == 0) {
            diffAccs = ImmutableIntArraySet.empty();
        }
        else {
            final DbQuery query = new DbQuery.Builder(bunchSets)
                    .join(bunchAccs, bunchSets.getBunchColumnIndex(), bunchAccs.getBunchColumnIndex())
                    .where(bunchSets.getSetIdColumnIndex(), register.diffBunchSetId)
                    .select(bunchAccsOffset + bunchAccs.getAcceptationColumnIndex());
            diffAccs = _db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
        }

        final DbQuery query;
        if (register.sourceBunchSetId == 0) {
            query = new DbQuery.Builder(strings)
                    .join(acceptations, strings.getDynamicAcceptationColumnIndex(), acceptations.getIdColumnIndex())
                    .whereColumnValueMatch(strings.getDynamicAcceptationColumnIndex(), strings.getMainAcceptationColumnIndex())
                    .orderBy(strings.getDynamicAcceptationColumnIndex(), strings.getStringAlphabetColumnIndex())
                    .select(
                            strings.getDynamicAcceptationColumnIndex(),
                            strings.getStringAlphabetColumnIndex(),
                            strings.getStringColumnIndex(),
                            strings.getMainAcceptationColumnIndex(),
                            strings.columns().size() + acceptations.getConceptColumnIndex());
        }
        else {
            query = new DbQuery.Builder(bunchSets)
                    .join(bunchAccs, bunchSets.getBunchColumnIndex(), bunchAccs.getBunchColumnIndex())
                    .join(strings, bunchAccsOffset + bunchAccs.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                    .join(acceptations, bunchAccsOffset + bunchAccs.getAcceptationColumnIndex(), acceptations.getIdColumnIndex())
                    .where(bunchSets.getSetIdColumnIndex(), register.sourceBunchSetId)
                    .orderBy(bunchAccsOffset + bunchAccs.getAcceptationColumnIndex(), stringsOffset + strings.getStringAlphabetColumnIndex())
                    .select(
                            bunchAccsOffset + bunchAccs.getAcceptationColumnIndex(),
                            stringsOffset + strings.getStringAlphabetColumnIndex(),
                            stringsOffset + strings.getStringColumnIndex(),
                            stringsOffset + strings.getMainAcceptationColumnIndex(),
                            acceptationsOffset + acceptations.getConceptColumnIndex());
        }

        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int accId = row.get(0).toInt();
                boolean noExcludedAcc = !diffAccs.contains(accId);
                final MutableIntKeyMap<String> corr = MutableIntKeyMap.empty();
                if (noExcludedAcc) {
                    corr.put(row.get(1).toInt(), row.get(2).toText());
                }
                int mainAcc = row.get(3).toInt();
                int concept = row.get(4).toInt();

                int newAccId;
                while (result.hasNext()) {
                    row = result.next();
                    newAccId = row.get(0).toInt();
                    if (newAccId != accId) {
                        if (noExcludedAcc) {
                            applyAgent(agentId, accId, concept, targetBunches,
                                    startMatcher, startAdder, endMatcher, endAdder, register.rule, corr, mainAcc);
                        }

                        accId = newAccId;
                        noExcludedAcc = !diffAccs.contains(accId);
                        corr.clear();
                        if (noExcludedAcc) {
                            corr.put(row.get(1).toInt(), row.get(2).toText());
                            mainAcc = row.get(3).toInt();
                            concept = row.get(4).toInt();
                        }
                    }
                    else if (noExcludedAcc) {
                        corr.put(row.get(1).toInt(), row.get(2).toText());
                    }
                }

                if (noExcludedAcc) {
                    applyAgent(agentId, accId, concept, targetBunches,
                            startMatcher, startAdder, endMatcher, endAdder, register.rule, corr, mainAcc);
                }
            }
        }
    }

    private int[] sortAgents(IntKeyMap<StreamedDatabaseReader.AgentBunches> agents) {
        final int agentCount = agents.size();
        int[] ids = new int[agentCount];
        if (agentCount == 0) {
            return ids;
        }

        StreamedDatabaseReader.AgentBunches[] result = new StreamedDatabaseReader.AgentBunches[agentCount];

        for (int i = 0; i < agentCount; i++) {
            ids[i] = agents.keyAt(i);
            result[i] = agents.valueAt(i);
        }

        int index = agentCount;
        do {
            final StreamedDatabaseReader.AgentBunches agent = result[--index];

            int firstDependency = -1;
            for (int i = 0; i < index; i++) {
                if (result[i].dependsOn(agent)) {
                    firstDependency = i;
                    break;
                }
            }

            if (firstDependency >= 0) {
                int id = ids[firstDependency];
                ids[firstDependency] = ids[index];
                ids[index] = id;

                StreamedDatabaseReader.AgentBunches temp = result[firstDependency];
                result[firstDependency] = result[index];
                result[index++] = temp;
            }
        } while (index > 0);

        return ids;
    }

    private void runAgents(IntKeyMap<StreamedDatabaseReader.AgentBunches> agents) {
        final int agentCount = agents.size();
        int index = 0;
        for (int agentId : sortAgents(agents)) {
            setProgress(0.4f + ((0.8f - 0.4f) / agentCount) * index, "Running agent " + (++index) + " out of " + agentCount);
            runAgent(agentId);
        }
    }

    private void applyConversions(Conversion[] conversions) {
        for (Conversion conversion : conversions) {
            applyConversion(_db, conversion);
        }
    }

    private void insertSentences(StreamedDatabaseReader.SentenceSpan[] spans, int[] accIdMap, StreamedDatabaseReader.AgentAcceptationPair[] ruleAcceptationPairs) {
        for (StreamedDatabaseReader.SentenceSpan span : spans) {
            final ImmutableIntRange range = new ImmutableIntRange(span.start, span.start + span.length - 1);
            final int acc;
            if (span.acceptationFileIndex < accIdMap.length) {
                acc = accIdMap[span.acceptationFileIndex];
            }
            else {
                StreamedDatabaseReader.AgentAcceptationPair pair = ruleAcceptationPairs[span.acceptationFileIndex - accIdMap.length];
                acc = findRuledAcceptationByAgentAndBaseAcceptation(_db, pair.agent, pair.acceptation);
            }
            insertSpan(_db, span.sentenceId, range, acc);
        }
    }

    public void read() throws IOException {
        final StreamedDatabaseReader.Result result = _dbReader.read();

        setProgress(0.2f, "Indexing strings");
        fillSearchQueryTable();

        setProgress(0.3f, "Running agents");
        runAgents(result.agents);

        setProgress(0.8f, "Applying conversions");
        applyConversions(result.conversions);

        setProgress(0.9f, "Inserting sentence spans");
        insertSentences(result.spans, result.accIdMap, result.agentAcceptationPairs);
    }

    private static final class Listener implements ProgressListener {

        private final ProgressListener _listener;
        private final float _fraction;

        Listener(ProgressListener listener, float fraction) {
            if (fraction <= 0.0f || fraction >= 1.0f) {
                throw new IllegalArgumentException();
            }

            _listener = listener;
            _fraction = fraction;
        }

        @Override
        public void setProgress(float progress, String message) {
            _listener.setProgress(progress * _fraction, message);
        }
    }
}
