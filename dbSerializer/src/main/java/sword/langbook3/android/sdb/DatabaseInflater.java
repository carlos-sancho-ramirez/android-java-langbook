package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.IntKeyMap;
import sword.collections.List;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntList;
import sword.collections.MutableIntSet;
import sword.collections.MutableList;
import sword.collections.MutableSet;
import sword.collections.Traversable;
import sword.database.DbExporter;
import sword.database.DbImporter;
import sword.database.DbInsertQuery;
import sword.database.DbInserter;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbValue;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.sdb.models.AgentRegister;

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
        _dbReader = new StreamedDatabaseReader(db, is, (listener != null)? new Listener(listener, 0.25f) : null);
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

    private void insertTexts(int acceptationId, List<? extends IntKeyMap<String>> correlationArray, IntKeyMap<? extends Traversable<Conversion>> conversions) {
        final MutableIntKeyMap<String> plainTexts = MutableIntKeyMap.empty();
        for (IntKeyMap<String> correlation : correlationArray) {
            for (IntKeyMap.Entry<String> entry : correlation.entries()) {
                plainTexts.put(entry.key(), plainTexts.get(entry.key(), "") + entry.value());
            }
        }

        final MutableSet<String> inserted = plainTexts.toSet().mutate();
        for (IntKeyMap.Entry<String> entry : plainTexts.entries()) {
            final int alphabet = entry.key();
            final String text = entry.value();
            final String mainText = plainTexts.valueAt(0);
            insertStringQuery(_db, text, mainText, acceptationId, acceptationId, alphabet);

            final Traversable<Conversion> conversionsToApply = conversions.get(alphabet, null);
            if (conversionsToApply != null) {
                for (Conversion conversion : conversionsToApply) {
                    final String convertedText = conversion.convert(text);
                    if (convertedText == null) {
                        throw new AssertionError();
                    }

                    insertStringQuery(_db, convertedText, mainText, acceptationId, acceptationId, conversion.getTargetAlphabet());
                    inserted.add(convertedText);
                }
            }
        }

        insertPossibleCombinations(acceptationId, acceptationId, plainTexts.valueAt(0), inserted, "", correlationArray.toImmutable());
    }

    private void fillSearchQueryTable(IntKeyMap<? extends Traversable<Conversion>> conversions) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations; // J0
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = LangbookDbSchema.Tables.correlationArrays; // J1
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations; // J2
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays; // J3

        final int corrArrayOffset = acceptations.columns().size();
        final int corrOffset = corrArrayOffset + correlationArrays.columns().size();
        final int symbolArrayOffset = corrOffset + correlations.columns().size();

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(correlationArrays, acceptations.getCorrelationArrayColumnIndex(), correlationArrays.getArrayIdColumnIndex())
                .join(correlations, corrArrayOffset + correlationArrays.getCorrelationColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbolArrays, corrOffset + correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .orderBy(acceptations.getIdColumnIndex())
                .select(
                        acceptations.getIdColumnIndex(),
                        corrArrayOffset + correlationArrays.getArrayPositionColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex(),
                        symbolArrayOffset + symbolArrays.getStrColumnIndex());

        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int accId = row.get(0).toInt();
                final MutableList<MutableIntKeyMap<String>> correlationArray = MutableList.empty();
                int arrayPos = row.get(1).toInt();
                while (correlationArray.size() <= arrayPos) {
                    correlationArray.append(MutableIntKeyMap.empty());
                }

                correlationArray.get(arrayPos).put(row.get(2).toInt(), row.get(3).toText());

                while (result.hasNext()) {
                    row = result.next();
                    final int newAccId = row.get(0).toInt();
                    if (newAccId != accId) {
                        insertTexts(accId, correlationArray, conversions);
                        accId = newAccId;
                        correlationArray.clear();
                    }

                    arrayPos = row.get(1).toInt();
                    while (correlationArray.size() <= arrayPos) {
                        correlationArray.append(MutableIntKeyMap.empty());
                    }

                    correlationArray.get(arrayPos).put(row.get(2).toInt(), row.get(3).toText());
                }

                insertTexts(accId, correlationArray, conversions);
            }
        }
    }

    private ImmutablePair<ImmutableList<ImmutableIntKeyMap<String>>, ImmutableIntList> getAcceptationCorrelationArrayWithText(int acceptationId) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = LangbookDbSchema.Tables.correlationArrays;
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbols = LangbookDbSchema.Tables.symbolArrays;

        final int corrArraysOffset = acceptations.columns().size();
        final int corrOffset = corrArraysOffset + correlationArrays.columns().size();
        final int symbolsOffset = corrOffset + correlations.columns().size();

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(correlationArrays, acceptations.getCorrelationArrayColumnIndex(), correlationArrays.getArrayIdColumnIndex())
                .join(correlations, corrArraysOffset + correlationArrays.getCorrelationColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbols, corrOffset + correlations.getSymbolArrayColumnIndex(), symbols.getIdColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptationId)
                .orderBy(
                        corrArraysOffset + correlationArrays.getArrayPositionColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex())
                .select(
                        corrArraysOffset + correlationArrays.getArrayPositionColumnIndex(),
                        corrOffset + correlations.getCorrelationIdColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex(),
                        symbolsOffset + symbols.getStrColumnIndex()
                );

        final MutableIntList correlationIds = MutableIntList.empty();
        final MutableIntKeyMap<ImmutableIntKeyMap<String>> correlationMap = MutableIntKeyMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
                int pos = row.get(0).toInt();
                int correlationId = row.get(1).toInt();
                if (pos != correlationIds.size()) {
                    throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                }

                builder.put(row.get(2).toInt(), row.get(3).toText());

                while (dbResult.hasNext()) {
                    row = dbResult.next();
                    int newPos = row.get(0).toInt();
                    if (newPos != pos) {
                        correlationMap.put(correlationId, builder.build());
                        correlationIds.append(correlationId);
                        correlationId = row.get(1).toInt();
                        builder = new ImmutableIntKeyMap.Builder<>();
                        pos = newPos;
                    }

                    if (newPos != correlationIds.size()) {
                        throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                    }
                    builder.put(row.get(2).toInt(), row.get(3).toText());
                }
                correlationMap.put(correlationId, builder.build());
                correlationIds.append(correlationId);
            }
        }

        final ImmutableIntList imCorrelationIds = correlationIds.toImmutable();
        return new ImmutablePair<>(imCorrelationIds.map(correlationMap::get), imCorrelationIds);
    }

    private void insertPossibleCombinations(int mainAcceptation, int dynAcceptation, String mainStr, MutableSet<String> inserted, String accumulatedText, ImmutableList<? extends IntKeyMap<String>> remainingCorrelations) {
        if (remainingCorrelations.isEmpty()) {
            if (accumulatedText.length() > 0 && !inserted.contains(accumulatedText)) {
                inserted.add(accumulatedText);
                insertStringQuery(_db, accumulatedText, mainStr, mainAcceptation, dynAcceptation, 0);
            }
        }
        else {
            final ImmutableList<? extends IntKeyMap<String>> newList = remainingCorrelations.skip(1);
            for (String text : remainingCorrelations.valueAt(0)) {
                insertPossibleCombinations(mainAcceptation, dynAcceptation, mainStr, inserted, accumulatedText + text, newList);
            }
        }
    }

    private static class ApplyResult {
        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray;
        final ImmutableIntKeyMap<String> plainCorrelation;
        final ImmutableIntList knownCorrelationIds;

        ApplyResult(ImmutableList<ImmutableIntKeyMap<String>> correlationArray, ImmutableIntKeyMap<String> plainCorrelation, ImmutableIntList knownCorrelationIds) {
            if (knownCorrelationIds.size() != correlationArray.size()) {
                throw new IllegalArgumentException();
            }

            this.correlationArray = correlationArray;
            this.plainCorrelation = plainCorrelation;
            this.knownCorrelationIds = knownCorrelationIds;
        }
    }

    private final class AgentApplier {
        final int _agentId;
        final AgentRegister _register;
        final ImmutableIntSet _targetBunches;
        final ImmutableIntKeyMap<String> _startMatcher;
        final ImmutableIntKeyMap<String> _startAdder;
        final ImmutableIntKeyMap<String> _endMatcher;
        final ImmutableIntKeyMap<String> _endAdder;
        final int _rule;
        final boolean _modifyWords;
        final IntKeyMap<? extends Traversable<Conversion>> _conversions;
        final IntSupplier _correlationIdSupplier;
        final IntSupplier _correlationArrayIdSupplier;

        final ImmutableIntSet _startAdderKeys;
        final ImmutableIntSet _endAdderKeys;

        AgentApplier(int agentId, AgentRegister register, ImmutableIntSet targetBunches, ImmutableIntKeyMap<String> startMatcher, ImmutableIntKeyMap<String> startAdder, ImmutableIntKeyMap<String> endMatcher, ImmutableIntKeyMap<String> endAdder, int rule, boolean modifyWords, IntKeyMap<? extends Traversable<Conversion>> conversions, IntSupplier correlationIdSupplier, IntSupplier correlationArrayIdSupplier) {
            _agentId = agentId;
            _register = register;
            _targetBunches = targetBunches;
            _startMatcher = startMatcher;
            _startAdder = startAdder;
            _endMatcher = endMatcher;
            _endAdder = endAdder;
            _rule = rule;
            _modifyWords = modifyWords;
            _conversions = conversions;
            _correlationIdSupplier = correlationIdSupplier;
            _correlationArrayIdSupplier = correlationArrayIdSupplier;

            _startAdderKeys = startAdder.keySet();
            _endAdderKeys = endAdder.keySet();
        }

        private ApplyResult applyMatchersAddersAndConversions(ImmutableList<ImmutableIntKeyMap<String>> correlationArray, ImmutableIntList knownCorrelationIds) {
            final int correlationArrayLength = correlationArray.size();
            if (correlationArrayLength == 0) {
                return null;
            }

            final Iterator<ImmutableIntKeyMap<String>> correlationArrayIt = correlationArray.iterator();
            final ImmutableIntSet correlationAlphabets = correlationArrayIt.next().keySet();

            while (correlationArrayIt.hasNext()) {
                final MutableIntSet missingAlphabets = correlationAlphabets.mutate();
                for (int alphabet : correlationArrayIt.next().keySet()) {
                    if (!missingAlphabets.remove(alphabet)) {
                        return null;
                    }
                }

                if (!missingAlphabets.isEmpty()) {
                    return null;
                }
            }

            if (_startAdderKeys.anyMatch(key -> !correlationAlphabets.contains(key))) {
                return null;
            }

            if (_endAdderKeys.anyMatch(key -> !correlationAlphabets.contains(key))) {
                return null;
            }

            final MutableIntList modifiedKnownCorrelationIds = knownCorrelationIds.mutate();
            ImmutableList<ImmutableIntKeyMap<String>> modifiedCorrelationArray = correlationArray.toList();
            for (IntKeyMap.Entry<String> entry : _startMatcher.entries()) {
                final int alphabet = entry.key();
                if (!correlationAlphabets.contains(alphabet)) {
                    return null;
                }

                final int length = entry.value().length();
                while (modifiedCorrelationArray.size() > 1 && length > modifiedCorrelationArray.valueAt(0).get(alphabet).length()) {
                    final int currentSize = modifiedCorrelationArray.size();
                    final MutableIntKeyMap<String> newFirstCorrelation = modifiedCorrelationArray.valueAt(0).mutate();
                    final ImmutableIntKeyMap<String> secondCorrelation = modifiedCorrelationArray.valueAt(1);
                    for (int alp : correlationAlphabets) {
                        newFirstCorrelation.put(alp, newFirstCorrelation.get(alp) + secondCorrelation.get(alp));
                    }

                    final ImmutableList.Builder<ImmutableIntKeyMap<String>> builder = new ImmutableList.Builder<>();
                    builder.append(newFirstCorrelation.toImmutable());
                    for (int i = 2; i < currentSize; i++) {
                        builder.append(modifiedCorrelationArray.valueAt(i));
                    }

                    modifiedKnownCorrelationIds.removeAt(0);
                    modifiedKnownCorrelationIds.put(0, 0);
                    modifiedCorrelationArray = builder.build();
                }

                final ImmutableIntKeyMap<String> oldCorrelation = modifiedCorrelationArray.valueAt(0);
                final ImmutableIntKeyMap<String> newCorrelation = oldCorrelation.put(alphabet, oldCorrelation.get(alphabet).substring(length));
                modifiedCorrelationArray = modifiedCorrelationArray.skip(1).prepend(newCorrelation);
                modifiedKnownCorrelationIds.put(0, 0);
            }

            final ImmutableIntKeyMap<String> firstCorrelation = modifiedCorrelationArray.valueAt(0);
            if (firstCorrelation.anyMatch(String::isEmpty)) {
                if (!firstCorrelation.anyMatch(text -> !text.isEmpty())) {
                    modifiedCorrelationArray = modifiedCorrelationArray.removeAt(0);
                    modifiedKnownCorrelationIds.removeAt(0);
                }
                else {
                    return null;
                }
            }

            for (IntKeyMap.Entry<String> entry : _endMatcher.entries()) {
                final int alphabet = entry.key();
                if (!correlationAlphabets.contains(alphabet)) {
                    return null;
                }

                final int length = entry.value().length();
                while (modifiedCorrelationArray.size() > 1 && length > modifiedCorrelationArray.valueAt(modifiedCorrelationArray.size() - 1).get(alphabet).length()) {
                    final int currentSize = modifiedCorrelationArray.size();
                    final MutableIntKeyMap<String> newLastCorrelation = modifiedCorrelationArray.valueAt(currentSize - 2).mutate();
                    final ImmutableIntKeyMap<String> lastCorrelation = modifiedCorrelationArray.valueAt(currentSize - 1);
                    for (int alp : correlationAlphabets) {
                        newLastCorrelation.put(alp, newLastCorrelation.get(alp) + lastCorrelation.get(alp));
                    }

                    final ImmutableList.Builder<ImmutableIntKeyMap<String>> builder = new ImmutableList.Builder<>();
                    for (int i = 0; i < currentSize - 2; i++) {
                        builder.append(modifiedCorrelationArray.valueAt(i));
                    }
                    builder.append(newLastCorrelation.toImmutable());

                    modifiedCorrelationArray = builder.build();
                    modifiedKnownCorrelationIds.removeAt(currentSize - 1);
                    modifiedKnownCorrelationIds.put(currentSize - 2, 0);
                }

                final ImmutableIntKeyMap<String> oldCorrelation = modifiedCorrelationArray.valueAt(modifiedCorrelationArray.size() - 1);
                final String oldText = oldCorrelation.get(alphabet);
                final int substringLimit = oldText.length() - length;
                if (substringLimit < 0) {
                    return null;
                }

                final ImmutableIntKeyMap<String> newCorrelation = oldCorrelation.put(alphabet, oldText.substring(0, substringLimit));
                modifiedCorrelationArray = modifiedCorrelationArray.skipLast(1).append(newCorrelation);
                modifiedKnownCorrelationIds.put(modifiedKnownCorrelationIds.size() - 1, 0);
            }

            final ImmutableIntKeyMap<String> lastCorrelation = modifiedCorrelationArray.valueAt(modifiedCorrelationArray.size() - 1);
            if (lastCorrelation.anyMatch(String::isEmpty)) {
                if (!lastCorrelation.anyMatch(text -> !text.isEmpty())) {
                    modifiedCorrelationArray = modifiedCorrelationArray.skipLast(1);
                    modifiedKnownCorrelationIds.removeAt(modifiedKnownCorrelationIds.size() - 1);
                }
                else {
                    return null;
                }
            }

            if (!_startAdder.isEmpty()) {
                modifiedCorrelationArray = modifiedCorrelationArray.prepend(_startAdder);
                modifiedKnownCorrelationIds.prepend(_register.startAdderId);
            }

            if (!_endAdder.isEmpty()) {
                modifiedCorrelationArray = modifiedCorrelationArray.append(_endAdder);
                modifiedKnownCorrelationIds.append(_register.endAdderId);
            }

            // Create plain correlation
            MutableIntKeyMap<String> correlation = correlationAlphabets.assign(alp -> "").mutate();
            for (ImmutableIntKeyMap<String> corr : modifiedCorrelationArray) {
                for (IntKeyMap.Entry<String> entry : corr.entries()) {
                    final int alphabet = entry.key();
                    correlation.put(alphabet, correlation.get(alphabet) + entry.value());
                }
            }

            // Apply and verify conversions
            for (int alphabet : correlationAlphabets) {
                final Traversable<Conversion> conversionToApply = _conversions.get(alphabet, null);
                if (conversionToApply != null) {
                    for (Conversion conversion : conversionToApply) {
                        final String convertedText = conversion.convert(correlation.get(alphabet));
                        if (convertedText == null) {
                            return null;
                        }
                        correlation.put(conversion.getTargetAlphabet(), convertedText);
                    }
                }
            }

            return new ApplyResult(modifiedCorrelationArray, correlation.toImmutable(), modifiedKnownCorrelationIds.toImmutable());
        }

        void apply(int accId, int concept, IntKeyMap<String> corr, int mainAcc) {
            boolean matching = true;

            final int startMatcherLength = _startMatcher.size();
            for (int i = 0; matching && i < startMatcherLength; i++) {
                final int alphabet = _startMatcher.keyAt(i);
                final String corrStr = corr.get(alphabet, null);
                final String matcherStr = _startMatcher.valueAt(i);
                matching = corrStr != null && corrStr.startsWith(matcherStr);
            }

            final int endMatcherLength = _endMatcher.size();
            for (int i = 0; matching && i < endMatcherLength; i++) {
                final int alphabet = _endMatcher.keyAt(i);
                final String corrStr = corr.get(alphabet, null);
                final String matcherStr = _endMatcher.valueAt(i);
                matching = corrStr != null && corrStr.endsWith(matcherStr);
            }

            matching &= !_startAdderKeys.anyMatch(key -> corr.get(key, null) == null);
            matching &= !_endAdderKeys.anyMatch(key -> corr.get(key, null) == null);

            if (matching) {
                int targetAccId = accId;

                if (_modifyWords) {
                    final ImmutablePair<ImmutableList<ImmutableIntKeyMap<String>>, ImmutableIntList> correlationArrayResult = getAcceptationCorrelationArrayWithText(accId);
                    final ApplyResult processResult = applyMatchersAddersAndConversions(correlationArrayResult.left, correlationArrayResult.right);

                    if (processResult != null) {
                        final int newConcept = obtainRuledConcept(_db, _rule, concept);
                        final int corrArrayId = obtainCorrelationArray(_db, processResult.correlationArray.indexes().mapToInt(index -> {
                            final int knownId = processResult.knownCorrelationIds.valueAt(index);
                            if (knownId != 0) {
                                return knownId;
                            }
                            else {
                                final ImmutableIntKeyMap<String> correlation = processResult.correlationArray.valueAt(index);
                                final ImmutableIntPairMap intPairCorrelation = correlation.mapToInt(text -> obtainSymbolArray(_db, text));
                                return obtainCorrelation(_db, intPairCorrelation, _correlationIdSupplier);
                            }
                        }), _correlationArrayIdSupplier);

                        final int dynAccId = insertAcceptation(_db, newConcept, corrArrayId);
                        insertRuledAcceptation(_db, dynAccId, _agentId, accId);

                        final MutableSet<String> inserted = MutableHashSet.empty();
                        final ImmutableIntKeyMap<String> correlation = processResult.plainCorrelation;
                        final String mainStr = correlation.valueAt(0);
                        for (IntKeyMap.Entry<String> entry : correlation.entries()) {
                            final String text = entry.value();
                            inserted.add(text);
                            insertStringQuery(_db, text, mainStr, mainAcc, dynAccId, entry.key());
                        }
                        insertPossibleCombinations(mainAcc, dynAccId, mainStr, inserted, "", processResult.correlationArray);

                        targetAccId = dynAccId;
                    }
                    else {
                        matching = false;
                    }
                }

                if (matching) {
                    for (int targetBunch : _targetBunches) {
                        insertBunchAcceptation(_db, targetBunch, targetAccId, _agentId);
                    }
                }
            }
        }
    }

    private void runAgent(int agentId, IntKeyMap<? extends Traversable<Conversion>> conversions, IntSupplier correlationIdSupplier, IntSupplier correlationArrayIdSupplier) {
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
        final boolean modifyWords = !startMatcher.equals(startAdder) || !endMatcher.equals(endAdder);

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

        final AgentApplier agentApplier = new AgentApplier(agentId, register, targetBunches, startMatcher, startAdder, endMatcher, endAdder, register.rule, modifyWords, conversions, correlationIdSupplier, correlationArrayIdSupplier);
        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int accId = row.get(0).toInt();
                int alphabet = row.get(1).toInt();
                boolean noExcludedAcc = !diffAccs.contains(accId);
                final MutableIntKeyMap<String> corr = MutableIntKeyMap.empty();
                if (alphabet != 0 && noExcludedAcc) {
                    corr.put(alphabet, row.get(2).toText());
                }
                int mainAcc = row.get(3).toInt();
                int concept = row.get(4).toInt();

                int newAccId;
                while (result.hasNext()) {
                    row = result.next();
                    newAccId = row.get(0).toInt();
                    alphabet = row.get(1).toInt();
                    if (newAccId != accId) {
                        if (noExcludedAcc) {
                            agentApplier.apply(accId, concept, corr, mainAcc);
                        }

                        accId = newAccId;
                        noExcludedAcc = !diffAccs.contains(accId);
                        corr.clear();
                        if (noExcludedAcc) {
                            corr.put(alphabet, row.get(2).toText());
                            mainAcc = row.get(3).toInt();
                            concept = row.get(4).toInt();
                        }
                    }
                    else if (alphabet != 0 && noExcludedAcc) {
                        corr.put(alphabet, row.get(2).toText());
                    }
                }

                if (noExcludedAcc) {
                    agentApplier.apply(accId, concept, corr, mainAcc);
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

    private void runAgents(IntKeyMap<StreamedDatabaseReader.AgentBunches> agents, IntKeyMap<? extends Traversable<Conversion>> conversions, IntSupplier correlationIdSupplier, IntSupplier correlationArrayIdSupplier) {
        final int agentCount = agents.size();
        int index = 0;
        for (int agentId : sortAgents(agents)) {
            setProgress(0.4f + ((0.8f - 0.4f) / agentCount) * index, "Running agent " + (++index) + " out of " + agentCount);
            runAgent(agentId, conversions, correlationIdSupplier, correlationArrayIdSupplier);
        }
    }

    private int findDynamicAcceptationFromPairs(int acceptationFileIndex, int[] accIdMap,
            StreamedDatabaseReader.AgentAcceptationPair[] ruleAcceptationPairs) {
        if (acceptationFileIndex < accIdMap.length) {
            return accIdMap[acceptationFileIndex];
        }
        else {
            final StreamedDatabaseReader.AgentAcceptationPair pair = ruleAcceptationPairs[acceptationFileIndex - accIdMap.length];
            final int dynAcc = findDynamicAcceptationFromPairs(pair.acceptation, accIdMap, ruleAcceptationPairs);
            return findRuledAcceptationByAgentAndBaseAcceptation(_db, pair.agent, dynAcc);
        }
    }

    private void insertSentences(StreamedDatabaseReader.SentenceSpan[] spans, int[] accIdMap, StreamedDatabaseReader.AgentAcceptationPair[] ruleAcceptationPairs) {
        for (StreamedDatabaseReader.SentenceSpan span : spans) {
            final ImmutableIntRange range = new ImmutableIntRange(span.start, span.start + span.length - 1);
            final int acc = findDynamicAcceptationFromPairs(span.acceptationFileIndex, accIdMap, ruleAcceptationPairs);
            insertSpan(_db, span.sentenceId, range, acc);
        }
    }

    private static final class IdSupplier implements IntSupplier {
        private int _lastAssignedId;

        IdSupplier(int lastAssignedId) {
            _lastAssignedId = lastAssignedId;
        }

        @Override
        public int get() {
            return ++_lastAssignedId;
        }
    }

    public void read() throws IOException {
        final StreamedDatabaseReader.Result result = _dbReader.read();

        setProgress(0.25f, "Indexing strings");
        final IntKeyMap<? extends Traversable<Conversion>> conversions = result.composeConversionMap();
        fillSearchQueryTable(conversions);

        setProgress(0.3f, "Running agents");
        final IntSupplier correlationIdSupplier = new IdSupplier(result.numberOfCorrelations);
        final IntSupplier correlationArrayIdSupplier = new IdSupplier(result.numberOfCorrelationArrays);
        runAgents(result.agents, conversions, correlationIdSupplier, correlationArrayIdSupplier);

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
