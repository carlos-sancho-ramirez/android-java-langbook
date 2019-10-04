package sword.langbook3.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntRange;
import sword.collections.IntKeyMap;
import sword.collections.List;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntPairMap;
import sword.database.DbImporter;
import sword.database.DbInsertQuery;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbValue;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.LangbookReadableDatabase.AgentRegister;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.sdb.ProgressListener;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;
import sword.langbook3.android.sdb.StreamedDatabaseReader;

import static sword.database.DbQuery.concat;
import static sword.langbook3.android.db.LangbookDatabase.applyConversion;
import static sword.langbook3.android.db.LangbookDatabase.obtainCorrelation;
import static sword.langbook3.android.db.LangbookDatabase.obtainRuledConcept;
import static sword.langbook3.android.db.LangbookDatabase.obtainSimpleCorrelationArray;
import static sword.langbook3.android.db.LangbookDatabase.obtainSymbolArray;
import static sword.langbook3.android.db.LangbookDbInserter.insertAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertBunchAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertRuledAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertSpan;
import static sword.langbook3.android.db.LangbookDbInserter.insertStringQuery;
import static sword.langbook3.android.db.LangbookReadableDatabase.findRuledAcceptationByRuleAndMainAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAgentRegister;
import static sword.langbook3.android.db.LangbookReadableDatabase.getCorrelationWithText;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxAgentSetId;

public final class DatabaseInflater {

    private final DbImporter.Database _db;
    private final ProgressListener _listener;
    private final StreamedDatabaseReader _dbReader;

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

    private void applyAgent(int agentId, AgentSetSupplier agentSetSupplier, int accId, int concept,
            int targetBunch, IntKeyMap<String> startMatcher, IntKeyMap<String> startAdder,
            IntKeyMap<String> endMatcher, IntKeyMap<String> endAdder, int rule, IntKeyMap<String> corr) {
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
                    insertStringQuery(_db, resultCorr.valueAt(i), resultCorr.valueAt(0), accId, dynAccId, resultCorr.keyAt(i));
                }

                targetAccId = dynAccId;
            }

            if (targetBunch != StreamedDatabaseConstants.nullBunchId) {
                insertBunchAcceptation(_db, targetBunch, targetAccId, agentSetSupplier.get());
            }
        }
    }

    private int insertAgentSet(Set<Integer> agents) {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;

        if (agents.isEmpty()) {
            return table.nullReference();
        }
        else {
            final int setId = getMaxAgentSetId(_db) + 1;
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

    private final class AgentSetSupplier {
        private final int _agentId;
        private boolean _created;
        private int _agentSetId;

        AgentSetSupplier(int agentId) {
            _agentId = agentId;
        }

        int get() {
            if (!_created) {
                final Set<Integer> set = new HashSet<>();
                set.add(_agentId);
                _agentSetId = insertAgentSet(set);
                _created = true;
            }

            return _agentSetId;
        }
    }

    private ImmutableIntKeyMap<String> getCorrelation(int agentId, int agentColumn) {
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
        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        try {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int alphabet = row.get(0).toInt();
                final String str = row.get(1).toText();
                builder.put(alphabet, str);
            }
        }
        finally {
            result.close();
        }

        return builder.build();
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

        // TODO: This query does not manage the case where diff is different from null
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
                            acceptationsOffset + acceptations.getConceptColumnIndex());
        }

        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                final MutableIntKeyMap<String> corr = MutableIntKeyMap.empty();
                int accId = row.get(0).toInt();
                corr.put(row.get(1).toInt(), row.get(2).toText());
                int concept = row.get(3).toInt();

                final AgentSetSupplier agentSetSupplier = new AgentSetSupplier(agentId);
                int newAccId;
                while (result.hasNext()) {
                    row = result.next();
                    newAccId = row.get(0).toInt();
                    if (newAccId != accId) {
                        applyAgent(agentId, agentSetSupplier, accId, concept, register.targetBunch,
                                startMatcher, startAdder, endMatcher, endAdder, register.rule, corr);

                        accId = newAccId;
                        corr.clear();
                        corr.put(row.get(1).toInt(), row.get(2).toText());
                        concept = row.get(3).toInt();
                    }
                    else {
                        corr.put(row.get(1).toInt(), row.get(2).toText());
                    }
                }

                applyAgent(agentId, agentSetSupplier, accId, concept, register.targetBunch,
                        startMatcher, startAdder, endMatcher, endAdder, register.rule, corr);
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
        } while(index > 0);

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

    private void insertSentences(StreamedDatabaseReader.SentenceSpan[] spans, int[] accIdMap, StreamedDatabaseReader.RuleAcceptationPair[] ruleAcceptationPairs) {
        for (StreamedDatabaseReader.SentenceSpan span : spans) {
            final ImmutableIntRange range = new ImmutableIntRange(span.start, span.start + span.length - 1);
            final int acc;
            if (span.acceptationFileIndex < accIdMap.length) {
                acc = accIdMap[span.acceptationFileIndex];
            }
            else {
                StreamedDatabaseReader.RuleAcceptationPair pair = ruleAcceptationPairs[span.acceptationFileIndex - accIdMap.length];
                acc = findRuledAcceptationByRuleAndMainAcceptation(_db, pair.rule, pair.acceptation);
            }
            insertSpan(_db, span.symbolArray, range, acc);
        }
    }

    void read() throws IOException {
        final StreamedDatabaseReader.Result result = _dbReader.read();

        setProgress(0.2f, "Indexing strings");
        fillSearchQueryTable();

        setProgress(0.3f, "Running agents");
        runAgents(result.agents);

        setProgress(0.8f, "Applying conversions");
        applyConversions(result.conversions);

        setProgress(0.9f, "Inserting sentence spans");
        insertSentences(result.spans, result.accIdMap, result.ruleAcceptationPairs);
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
