package sword.langbook3.android;

import android.util.SparseArray;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import sword.collections.IntKeyMap;
import sword.collections.MutableIntPairMap;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.sdb.ProgressListener;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;
import sword.langbook3.android.sdb.StreamedDatabaseReader;

import static sword.langbook3.android.db.DbQuery.concat;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.getColumnMax;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.insertAcceptation;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.insertBunchAcceptation;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.insertCorrelationArray;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.obtainCorrelation;
import static sword.langbook3.android.sdb.StreamedDatabaseReader.obtainSymbolArray;

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

    private int getMaxAgentSetId() {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
        return getColumnMax(_db, table, table.getSetIdColumnIndex());
    }

    private int getMaxWord() {
        LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        return getColumnMax(_db, table, table.getWordColumnIndex());
    }

    private int getMaxConcept() {
        LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        return getColumnMax(_db, table, table.getConceptColumnIndex());
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
                    resultCorrIds.put(alphabet, obtainSymbolArray(_db, resultCorr.valueAt(i)));
                }

                final int corrId = obtainCorrelation(_db, resultCorrIds);
                final int corrArrayId = insertCorrelationArray(_db, corrId);
                suggestedNewWordUsed = true;
                final int dynAccId = insertAcceptation(_db, suggestedNewWordId, newConcept, corrArrayId);
                insertRuledAcceptation(dynAccId, agentId, accId);

                for (int i = 0; i < resultCorrLength; i++) {
                    insertStringQuery(resultCorr.valueAt(i), resultCorr.valueAt(0), accId, dynAccId, resultCorr.keyAt(i));
                }

                targetAccId = dynAccId;
            }

            if (targetBunch != StreamedDatabaseConstants.nullBunchId) {
                insertBunchAcceptation(_db, targetBunch, targetAccId, agentSetSupplier.get());
            }
        }

        return suggestedNewWordUsed;
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

    private SparseArray<String> getCorrelation(int agentId, int agentColumn) {
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

    private void runAgent(int agentId) {
        LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        LangbookDbSchema.BunchAcceptationsTable bunchAccs = LangbookDbSchema.Tables.bunchAcceptations;
        LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        int maxWord = getMaxWord();
        final SparseArray<String> matcher = getCorrelation(agentId, agents.getMatcherColumnIndex());
        final SparseArray<String> adder = getCorrelation(agentId, agents.getAdderColumnIndex());

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

    private void applyConversions(StreamedDatabaseReader.Conversion[] conversions) {
        for (StreamedDatabaseReader.Conversion conversion : conversions) {
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
        final StreamedDatabaseReader.Result result = _dbReader.read();

        setProgress(0.3f, "Indexing strings");
        fillSearchQueryTable();

        setProgress(0.4f, "Running agents");
        runAgents(result.agents);

        setProgress(0.8f, "Applying conversions");
        applyConversions(result.conversions);
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
