package sword.langbook3.android;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntList;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.List;
import sword.collections.MutableIntKeyMap;
import sword.langbook3.android.LangbookReadableDatabase.AgentDetails;
import sword.langbook3.android.LangbookReadableDatabase.QuizDetails;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbDeleteQuery;
import sword.langbook3.android.db.DbExporter;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbStringValue;
import sword.langbook3.android.db.DbUpdateQuery;
import sword.langbook3.android.db.DbValue;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;

import static sword.langbook3.android.LangbookDatabaseUtils.convertText;
import static sword.langbook3.android.LangbookDbInserter.insertAcceptation;
import static sword.langbook3.android.LangbookDbInserter.insertAllPossibilities;
import static sword.langbook3.android.LangbookDbInserter.insertBunchAcceptation;
import static sword.langbook3.android.LangbookDbInserter.insertQuizDefinition;
import static sword.langbook3.android.LangbookDbInserter.insertRuledAcceptation;
import static sword.langbook3.android.LangbookDbInserter.insertSearchHistoryEntry;
import static sword.langbook3.android.LangbookDbInserter.insertSentenceMeaning;
import static sword.langbook3.android.LangbookDbInserter.insertStringQuery;
import static sword.langbook3.android.LangbookDbInserter.insertSymbolArray;
import static sword.langbook3.android.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.LangbookDeleter.deleteAcceptation;
import static sword.langbook3.android.LangbookDeleter.deleteAgentSet;
import static sword.langbook3.android.LangbookDeleter.deleteBunchAcceptation;
import static sword.langbook3.android.LangbookDeleter.deleteBunchAcceptationsForAgentSet;
import static sword.langbook3.android.LangbookDeleter.deleteKnowledge;
import static sword.langbook3.android.LangbookDeleter.deleteKnowledgeForQuiz;
import static sword.langbook3.android.LangbookDeleter.deleteQuiz;
import static sword.langbook3.android.LangbookDeleter.deleteRuledAcceptation;
import static sword.langbook3.android.LangbookDeleter.deleteSearchHistoryForAcceptation;
import static sword.langbook3.android.LangbookDeleter.deleteSentenceMeaning;
import static sword.langbook3.android.LangbookDeleter.deleteSpanBySymbolArrayId;
import static sword.langbook3.android.LangbookDeleter.deleteStringQueriesForDynamicAcceptation;
import static sword.langbook3.android.LangbookDeleter.deleteSymbolArray;
import static sword.langbook3.android.LangbookReadableDatabase.conceptFromAcceptation;
import static sword.langbook3.android.LangbookReadableDatabase.findAffectedAgentsByAcceptationCorrelationModification;
import static sword.langbook3.android.LangbookReadableDatabase.findAffectedAgentsByItsDiffWithTarget;
import static sword.langbook3.android.LangbookReadableDatabase.findAffectedAgentsByItsSourceWithTarget;
import static sword.langbook3.android.LangbookReadableDatabase.findAgentSet;
import static sword.langbook3.android.LangbookReadableDatabase.findAgentsWithoutSourceBunches;
import static sword.langbook3.android.LangbookReadableDatabase.findAgentsWithoutSourceBunchesWithTarget;
import static sword.langbook3.android.LangbookReadableDatabase.findBunchSet;
import static sword.langbook3.android.LangbookReadableDatabase.findConversions;
import static sword.langbook3.android.LangbookReadableDatabase.findCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.findCorrelationArray;
import static sword.langbook3.android.LangbookReadableDatabase.findQuestionFieldSet;
import static sword.langbook3.android.LangbookReadableDatabase.findQuizDefinition;
import static sword.langbook3.android.LangbookReadableDatabase.findQuizzesByBunch;
import static sword.langbook3.android.LangbookReadableDatabase.findSentenceIdsMatchingMeaning;
import static sword.langbook3.android.LangbookReadableDatabase.findSymbolArray;
import static sword.langbook3.android.LangbookReadableDatabase.getAcceptationsAndAgentSetsInBunch;
import static sword.langbook3.android.LangbookReadableDatabase.getAgentDetails;
import static sword.langbook3.android.LangbookReadableDatabase.getAgentProcessedMap;
import static sword.langbook3.android.LangbookReadableDatabase.getAllAgentSetsContaining;
import static sword.langbook3.android.LangbookReadableDatabase.getAllRuledAcceptationsForAgent;
import static sword.langbook3.android.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.LangbookReadableDatabase.getCurrentKnowledge;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxAgentSetId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxCorrelationArrayId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxCorrelationId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxQuestionFieldSetId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxSentenceMeaning;
import static sword.langbook3.android.LangbookReadableDatabase.getQuizDetails;
import static sword.langbook3.android.LangbookReadableDatabase.getSentenceMeaning;
import static sword.langbook3.android.LangbookReadableDatabase.isAcceptationInBunch;
import static sword.langbook3.android.LangbookReadableDatabase.isSymbolArrayMerelyASentence;
import static sword.langbook3.android.LangbookReadableDatabase.readAcceptationTextsAndMain;
import static sword.langbook3.android.LangbookReadableDatabase.readAllPossibleAcceptations;
import static sword.langbook3.android.LangbookReadableDatabase.readCorrelationArrayTextAndItsAppliedConversions;
import static sword.langbook3.android.LangbookReadableDatabase.readMainAlphabetFromAlphabet;

public final class LangbookDatabase {

    private final Database db;
    private final LangbookReadableDatabase selector;
    private final LangbookDbInserter inserter;

    LangbookDatabase(Database db) {
        this.db = db;
        selector = new LangbookReadableDatabase(db);
        inserter = new LangbookDbInserter(db);
    }

    public static int obtainSymbolArray(DbImporter.Database db, String str) {
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

    public static int insertCorrelation(DbImporter.Database db, IntPairMap correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }

        final int newCorrelationId = getMaxCorrelationId(db) + 1;
        LangbookDbInserter.insertCorrelation(db, newCorrelationId, correlation);
        return newCorrelationId;
    }

    public static int obtainCorrelation(DbImporter.Database db, IntPairMap correlation) {
        final Integer id = findCorrelation(db, correlation);
        return (id != null)? id : insertCorrelation(db, correlation);
    }

    public static int insertCorrelation(DbImporter.Database db, IntKeyMap<String> correlation) {
        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        for (IntKeyMap.Entry<String> entry : correlation.entries()) {
            builder.put(entry.key(), obtainSymbolArray(db, entry.value()));
        }

        return insertCorrelation(db, builder.build());
    }

    public static int insertCorrelationArray(DbImporter.Database db, int... correlation) {
        final int maxArrayId = getMaxCorrelationArrayId(db);
        final int newArrayId = maxArrayId + ((maxArrayId + 1 != StreamedDatabaseConstants.nullCorrelationArrayId)? 1 : 2);
        LangbookDbInserter.insertCorrelationArray(db, newArrayId, correlation);
        return newArrayId;
    }

    public static int insertCorrelationArray(DbImporter.Database db, IntList correlations) {
        final int[] values = new int[correlations.size()];
        int index = 0;
        for (int value : correlations) {
            values[index++] = value;
        }
        return insertCorrelationArray(db, values);
    }

    public static int obtainCorrelationArray(DbImporter.Database db, IntList correlations) {
        final Integer foundId = findCorrelationArray(db, correlations);
        return (foundId == null) ? insertCorrelationArray(db, correlations) : foundId;
    }

    public static int insertBunchSet(DbImporter.Database db, IntSet bunchSet) {
        if (bunchSet.isEmpty()) {
            return 0;
        }

        final int setId = LangbookReadableDatabase.getMaxBunchSetId(db) + 1;
        LangbookDbInserter.insertBunchSet(db, setId, bunchSet);
        return setId;
    }

    public static int insertBunchSet(DbImporter.Database db, int... bunches) {
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        for (int bunch : bunches) {
            builder.add(bunch);
        }
        return insertBunchSet(db, builder.build());
    }

    public static int obtainBunchSet(DbImporter.Database db, IntSet bunchSet) {
        final Integer id = findBunchSet(db, bunchSet);
        return (id != null)? id : insertBunchSet(db, bunchSet);
    }

    public static int insertAgentSet(DbImporter.Database db, IntSet agentSet) {
        if (agentSet.isEmpty()) {
            return 0;
        }

        final int setId = getMaxAgentSetId(db) + 1;
        LangbookDbInserter.insertAgentSet(db, setId, agentSet);
        return setId;
    }

    public static int obtainAgentSet(DbImporter.Database db, IntSet set) {
        final Integer setId = findAgentSet(db, set);
        return (setId != null)? setId : insertAgentSet(db, set);
    }

    public static int insertRuledConcept(DbImporter.Database db, int rule, int concept) {
        final int ruledConcept = LangbookReadableDatabase.getMaxConcept(db) + 1;
        LangbookDbInserter.insertRuledConcept(db, ruledConcept, rule, concept);
        return ruledConcept;
    }

    public static int obtainRuledConcept(DbImporter.Database db, int rule, int concept) {
        final Integer id = LangbookReadableDatabase.findRuledConcept(db, rule, concept);
        return (id != null)? id : insertRuledConcept(db, rule, concept);
    }

    public static int insertQuestionFieldSet(DbImporter.Database db, Iterable<LangbookReadableDatabase.QuestionFieldDetails> fields) {
        if (!fields.iterator().hasNext()) {
            return 0;
        }

        final int setId = getMaxQuestionFieldSetId(db) + 1;
        LangbookDbInserter.insertQuestionFieldSet(db, setId, fields);
        return setId;
    }

    private static ImmutableIntSet findMatchingAcceptations(DbExporter.Database db,
            ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches,
            ImmutableIntKeyMap<String> startMatcher, ImmutableIntKeyMap<String> endMatcher) {

        final ImmutableIntSetBuilder diffAccBuilder = new ImmutableIntSetBuilder();
        for (int bunch : diffBunches) {
            final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
            final DbQuery query = new DbQuery.Builder(table)
                    .where(table.getBunchColumnIndex(), bunch)
                    .select(table.getAcceptationColumnIndex());
            try (DbResult result = db.select(query)) {
                while (result.hasNext()) {
                    diffAccBuilder.add(result.next().get(0).toInt());
                }
            }
        }
        final ImmutableIntSet diffAcceptations = diffAccBuilder.build();

        ImmutableIntSet matchingAcceptations = null;
        if (!sourceBunches.isEmpty()) {
            final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
            for (int bunch : sourceBunches) {
                final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
                final DbQuery query = new DbQuery.Builder(table)
                        .where(table.getBunchColumnIndex(), bunch)
                        .select(table.getAcceptationColumnIndex());
                try (DbResult result = db.select(query)) {
                    while (result.hasNext()) {
                        final int acc = result.next().get(0).toInt();
                        if (!diffAcceptations.contains(acc)) {
                            builder.add(acc);
                        }
                    }
                }
            }

            matchingAcceptations = builder.build();
        }

        final ImmutableIntSet matchingAlphabets = startMatcher.keySet().addAll(endMatcher.keySet());
        final LangbookDbSchema.StringQueriesTable strTable = LangbookDbSchema.Tables.stringQueries;
        for (int alphabet : matchingAlphabets) {
            final String startMatch = startMatcher.get(alphabet, "");
            final String endMatch = endMatcher.get(alphabet, "");

            final boolean matchWordStarting = startMatch.length() > endMatch.length();
            final String queryValue = matchWordStarting? startMatch : endMatch;
            final int restrictionType = matchWordStarting? DbQuery.RestrictionStringTypes.STARTS_WITH : DbQuery.RestrictionStringTypes.ENDS_WITH;
            final DbQuery matchQuery = new DbQuery.Builder(strTable)
                    .where(strTable.getStringAlphabetColumnIndex(), alphabet)
                    .where(strTable.getStringColumnIndex(), new DbQuery.Restriction(
                            new DbStringValue(queryValue), restrictionType))
                    .select(strTable.getDynamicAcceptationColumnIndex());
            final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
            try (DbResult result = db.select(matchQuery)) {
                while (result.hasNext()) {
                    final int acc = result.next().get(0).toInt();
                    if (matchingAcceptations == null && !diffAcceptations.contains(acc) ||
                            matchingAcceptations != null && matchingAcceptations.contains(acc)) {

                        // This code is only checking start or end per each alphabet, but not both.
                        // But as it is not an expected case so far I leave it this way to make it more efficient.
                        // TODO: Check both start and end if required
                        builder.add(acc);
                    }
                }
            }

            matchingAcceptations = builder.build();
        }

        if (matchingAcceptations == null) {
            throw new AssertionError("Unable to select all acceptations from the database");
        }

        return matchingAcceptations;
    }

    private static boolean applyMatchersAddersAndConversions(MutableIntKeyMap<String> correlation,
            AgentDetails details, ImmutableSet<ImmutableIntPair> conversionPairs,
            SyncCacheMap<ImmutableIntPair, ImmutableList<ImmutablePair<String, String>>> conversions) {
        for (IntKeyMap.Entry<String> entry : details.startMatcher.entries()) {
            final int length = entry.value().length();
            final String text = correlation.get(entry.key()).substring(length);
            correlation.put(entry.key(), text);
        }

        for (IntKeyMap.Entry<String> entry : details.startAdder.entries()) {
            final String text = entry.value() + correlation.get(entry.key());
            correlation.put(entry.key(), text);
        }

        for (IntKeyMap.Entry<String> entry : details.endMatcher.entries()) {
            final int length = entry.value().length();
            String text = correlation.get(entry.key());
            text = text.substring(0, text.length() - length);
            correlation.put(entry.key(), text);
        }

        for (IntKeyMap.Entry<String> entry : details.endAdder.entries()) {
            final String text = correlation.get(entry.key()) + entry.value();
            correlation.put(entry.key(), text);
        }

        boolean validConversion = true;
        for (ImmutableIntPair pair : conversionPairs) {
            final IntSet keySet = correlation.keySet();
            if (keySet.contains(pair.left)) {
                final String result = convertText(conversions.get(pair), correlation.get(pair.left));
                if (result == null) {
                    validConversion = false;
                    break;
                }
                correlation.put(pair.right, result);
            }
        }

        return validConversion;
    }

    private static void runAgent(Database db, int agentId, AgentDetails details) {
        final ImmutableIntSet matchingAcceptations = findMatchingAcceptations(db, details.sourceBunches, details.diffBunches, details.startMatcher, details.endMatcher);
        final ImmutableIntSet processedAcceptations;
        if (!details.modifyCorrelations()) {
            processedAcceptations = matchingAcceptations;
        }
        else {
            final ImmutableSet<ImmutableIntPair> conversionPairs = findConversions(db);
            final SyncCacheMap<ImmutableIntPair, ImmutableList<ImmutablePair<String, String>>> conversions =
                    new SyncCacheMap<>(key -> getConversion(db, key));

            final SyncCacheIntPairMap mainAlphabets = new SyncCacheIntPairMap(key -> readMainAlphabetFromAlphabet(db, key));
            final ImmutableIntSetBuilder processedAccBuilder = new ImmutableIntSetBuilder();

            for (int acc : matchingAcceptations) {
                final ImmutablePair<ImmutableIntKeyMap<String>, Integer> textsAndMain = readAcceptationTextsAndMain(db, acc);
                final MutableIntKeyMap<String> correlation = textsAndMain.left.mutate();

                final boolean validConversion = applyMatchersAddersAndConversions(correlation, details, conversionPairs, conversions);
                if (validConversion) {
                    final ImmutableIntPairMap.Builder corrBuilder = new ImmutableIntPairMap.Builder();
                    for (ImmutableIntKeyMap.Entry<String> entry : correlation.entries()) {
                        corrBuilder.put(entry.key(), obtainSymbolArray(db, entry.value()));
                    }

                    final int correlationId = LangbookDatabase.obtainCorrelation(db, corrBuilder.build());
                    final int correlationArrayId = insertCorrelationArray(db, correlationId);

                    final int baseConcept = conceptFromAcceptation(db, acc);
                    final int ruledConcept = obtainRuledConcept(db, details.rule, baseConcept);
                    final int newAcc = insertAcceptation(db, ruledConcept, correlationArrayId);
                    insertRuledAcceptation(db, newAcc, agentId, acc);

                    for (IntKeyMap.Entry<String> entry : correlation.entries()) {
                        final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                        insertStringQuery(db, entry.value(), mainText, textsAndMain.right, newAcc, entry.key());
                    }
                    processedAccBuilder.add(newAcc);
                }
            }
            processedAcceptations = processedAccBuilder.build();
        }

        if (details.targetBunch != NO_BUNCH) {
            final int agentSetId = obtainAgentSet(db, new ImmutableIntSetBuilder().add(agentId).build());
            for (int acc : processedAcceptations) {
                insertBunchAcceptation(db, details.targetBunch, acc, agentSetId);
            }
        }
    }

    /**
     * Run again the specified agent.
     * @return The bunch identifier in case the target bunch has changed, or null if there is no change.
     */
    private static Integer rerunAgent(Database db, int agentId, boolean acceptationCorrelationChanged) {
        final AgentDetails agentDetails = LangbookReadableDatabase.getAgentDetails(db, agentId);
        final ImmutableIntSet matchingAcceptations = findMatchingAcceptations(db,
                agentDetails.sourceBunches, agentDetails.diffBunches,
                agentDetails.startMatcher, agentDetails.endMatcher);

        boolean targetChanged = false;
        final boolean ruleApplied = agentDetails.modifyCorrelations();
        final ImmutableIntSet processedAcceptations;
        if (!ruleApplied) {
            final ImmutableIntKeyMap<ImmutableIntSet> agentSets = getAllAgentSetsContaining(db, agentId);
            final ImmutableIntPairMap acceptationAgentSetMap = getAcceptationsAndAgentSetsInBunch(db, agentDetails.targetBunch);
            final ImmutableIntSet alreadyProcessedAcceptations = acceptationAgentSetMap.keySet();

            for (IntPairMap.Entry entry : acceptationAgentSetMap.entries()) {
                if (!matchingAcceptations.contains(entry.key())) {
                    if (agentSets.get(entry.value()).size() != 1) {
                        throw new UnsupportedOperationException("Unimplemented");
                    }

                    if (!deleteBunchAcceptation(db, agentDetails.targetBunch, entry.key())) {
                        throw new AssertionError();
                    }
                    targetChanged = true;
                }
            }
            processedAcceptations = matchingAcceptations.filterNot(alreadyProcessedAcceptations::contains);
        }
        else {
            // This is assuming that matcher, adder and rule and flags did not change from last run,
            // only its source and diff bunches and its contents
            final ImmutableIntSet toBeProcessed;
            if (acceptationCorrelationChanged) {
                for (IntPairMap.Entry accPair : getAgentProcessedMap(db, agentId).entries()) {
                    final int acc = accPair.value();
                    deleteKnowledge(db, acc);
                    deleteBunchAcceptation(db, agentDetails.targetBunch, acc);
                    deleteStringQueriesForDynamicAcceptation(db, acc);
                    if (!deleteAcceptation(db, acc) | !deleteRuledAcceptation(db, acc)) {
                        throw new AssertionError();
                    }
                    targetChanged = true;
                }

                toBeProcessed = matchingAcceptations;
            }
            else {
                final ImmutableIntPairMap alreadyProcessedMap = getAgentProcessedMap(db, agentId);
                final ImmutableIntSet alreadyProcessedAcceptations = alreadyProcessedMap.keySet();
                toBeProcessed = matchingAcceptations.filterNot(alreadyProcessedAcceptations::contains);

                for (IntPairMap.Entry accPair : alreadyProcessedMap.entries()) {
                    if (!matchingAcceptations.contains(accPair.key())) {
                        final int acc = accPair.value();
                        deleteKnowledge(db, acc);
                        deleteBunchAcceptation(db, agentDetails.targetBunch, acc);
                        deleteStringQueriesForDynamicAcceptation(db, acc);
                        if (!deleteAcceptation(db, acc) | !deleteRuledAcceptation(db, acc)) {
                            throw new AssertionError();
                        }
                        targetChanged = true;
                    }
                }
            }

            final ImmutableSet<ImmutableIntPair> conversionPairs = findConversions(db);
            final SyncCacheMap<ImmutableIntPair, ImmutableList<ImmutablePair<String, String>>> conversions =
                    new SyncCacheMap<>(key -> getConversion(db, key));

            final SyncCacheIntPairMap mainAlphabets = new SyncCacheIntPairMap(key -> readMainAlphabetFromAlphabet(db, key));
            final ImmutableIntSetBuilder processedAccBuilder = new ImmutableIntSetBuilder();
            for (int acc : toBeProcessed) {
                final ImmutablePair<ImmutableIntKeyMap<String>, Integer> textsAndMain = readAcceptationTextsAndMain(db, acc);
                final MutableIntKeyMap<String> correlation = textsAndMain.left.mutate();

                final boolean validConversion = applyMatchersAddersAndConversions(correlation, agentDetails, conversionPairs, conversions);
                if (validConversion) {
                    final ImmutableIntPairMap.Builder corrBuilder = new ImmutableIntPairMap.Builder();
                    for (ImmutableIntKeyMap.Entry<String> entry : correlation.entries()) {
                        corrBuilder.put(entry.key(), obtainSymbolArray(db, entry.value()));
                    }

                    final int correlationId = LangbookDatabase.obtainCorrelation(db, corrBuilder.build());
                    final int correlationArrayId = insertCorrelationArray(db, correlationId);

                    final int baseConcept = conceptFromAcceptation(db, acc);
                    final int ruledConcept = obtainRuledConcept(db, agentDetails.rule, baseConcept);
                    final int newAcc = insertAcceptation(db, ruledConcept, correlationArrayId);
                    insertRuledAcceptation(db, newAcc, agentId, acc);

                    for (IntKeyMap.Entry<String> entry : correlation.entries()) {
                        final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                        insertStringQuery(db, entry.value(), mainText, textsAndMain.right, newAcc, entry.key());
                    }
                    processedAccBuilder.add(newAcc);
                }
            }
            processedAcceptations = processedAccBuilder.build();
        }

        if (agentDetails.targetBunch != NO_BUNCH) {
            final int agentSetId = obtainAgentSet(db, new ImmutableIntSetBuilder().add(agentId).build());
            for (int acc : processedAcceptations) {
                insertBunchAcceptation(db, agentDetails.targetBunch, acc, agentSetId);
                targetChanged = true;
            }
        }

        return (targetChanged && agentDetails.targetBunch != NO_BUNCH)? agentDetails.targetBunch : null;
    }

    public static Integer addAcceptation(Database db, int concept, int correlationArrayId) {
        final IntKeyMap<String> texts = readCorrelationArrayTextAndItsAppliedConversions(db, correlationArrayId);
        if (texts == null) {
            return null;
        }

        final String mainStr = texts.valueAt(0);
        final int acceptation = insertAcceptation(db, concept, correlationArrayId);
        for (IntKeyMap.Entry<String> entry : texts.entries()) {
            final int alphabet = entry.key();
            final String str = entry.value();
            insertStringQuery(db, str, mainStr, acceptation, acceptation, alphabet);
        }

        for (int agentId : findAgentsWithoutSourceBunches(db)) {
            rerunAgent(db, agentId, false);
        }

        return acceptation;
    }

    public static boolean updateAcceptationCorrelationArray(Database db, int acceptation, int newCorrelationArrayId) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getIdColumnIndex(), acceptation)
                .put(table.getCorrelationArrayColumnIndex(), newCorrelationArrayId)
                .build();
        final boolean changed = db.update(query);
        if (changed) {
            final IntKeyMap<String> texts = readCorrelationArrayTextAndItsAppliedConversions(db, newCorrelationArrayId);
            if (texts == null) {
                throw new AssertionError();
            }

            final String mainStr = texts.valueAt(0);
            for (IntKeyMap.Entry<String> entry : texts.entries()) {
                final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
                final DbUpdateQuery updateQuery = new DbUpdateQuery.Builder(strings)
                        .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                        .where(strings.getStringAlphabetColumnIndex(), entry.key())
                        .put(strings.getMainStringColumnIndex(), mainStr)
                        .put(strings.getStringColumnIndex(), entry.value())
                        .build();

                if (!db.update(updateQuery)) {
                    throw new AssertionError();
                }
            }

            final ImmutableIntSetBuilder touchedBunchesBuilder = new ImmutableIntSetBuilder();

            final ImmutableIntSetBuilder affectedAgents = new ImmutableIntSetBuilder();
            for (int agentId : findAgentsWithoutSourceBunches(db)) {
                affectedAgents.add(agentId);
            }

            for (int agentId : findAffectedAgentsByAcceptationCorrelationModification(db, acceptation)) {
                affectedAgents.add(agentId);
            }

            for (int agentId : affectedAgents.build()) {
                final Integer touchedBunch = rerunAgent(db, agentId, true);
                if (touchedBunch != null) {
                    touchedBunchesBuilder.add(touchedBunch);
                }
            }
            final ImmutableIntSet touchedBunches = touchedBunchesBuilder.build();

            final ImmutableIntSetBuilder quizIdsBuilder = new ImmutableIntSetBuilder();
            final LangbookDbSchema.QuizDefinitionsTable quizzes = LangbookDbSchema.Tables.quizDefinitions;
            final DbQuery quizQuery = new DbQuery.Builder(quizzes)
                    .select(quizzes.getIdColumnIndex(), quizzes.getBunchColumnIndex());
            try (DbResult result = db.select(quizQuery)) {
                while (result.hasNext()) {
                    final List<DbValue> row = result.next();
                    final int quizBunch = row.get(1).toInt();
                    if (quizBunch == 0 || touchedBunches.contains(quizBunch)) {
                        quizIdsBuilder.add(row.get(0).toInt());
                    }
                }
            }

            for (int quizId : quizIdsBuilder.build()) {
                recheckPossibleQuestions(db, quizId);
            }
        }

        return changed;
    }

    private static void removeFromStringQueryTable(Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getMainAcceptationColumnIndex(), acceptation)
                .build();

        db.delete(query);
    }

    private static void removeFromBunches(Database db, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .build();

        db.delete(query);
    }

    public static boolean removeAcceptation(Database db, int acceptation) {
        LangbookDeleter.deleteKnowledge(db, acceptation);
        removeFromBunches(db, acceptation);
        removeFromStringQueryTable(db, acceptation);
        final boolean removed = LangbookDeleter.deleteAcceptation(db, acceptation);
        deleteSearchHistoryForAcceptation(db, acceptation);

        final ImmutableIntPairMap affectedAgents = findAgentsWithoutSourceBunchesWithTarget(db);
        for (int agent : affectedAgents.keySet()) {
            rerunAgent(db, agent, false);
        }

        ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        for (int bunch : affectedAgents) {
            if (bunch != 0) {
                builder.add(bunch);
            }
        }

        ImmutableIntSet updatedBunches = builder.build();
        while (!updatedBunches.isEmpty()) {
            builder = new ImmutableIntSetBuilder();
            for (int bunch : updatedBunches) {
                for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, bunch).entries()) {
                    rerunAgent(db, entry.key(), false);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }
            }
            updatedBunches = builder.build();
        }
        return removed;
    }

    private static void recheckQuizzes(Database db, ImmutableIntSet updatedBunches) {
        final ImmutableIntSetBuilder affectedQuizzesBuilder = new ImmutableIntSetBuilder();
        for (int b : updatedBunches) {
            for (int quizId : findQuizzesByBunch(db, b)) {
                affectedQuizzesBuilder.add(quizId);
            }
        }

        for (int quizId : affectedQuizzesBuilder.build()) {
            recheckPossibleQuestions(db, quizId);
        }
    }

    /**
     * Include an acceptation within a bunch in a secure way.
     *
     * This method will check that the given combination is not already registered in the database table,
     * if so, it will do nothing and will return false.
     *
     * @param db Database to be used.
     * @param bunch Bunch identifier.
     * @param acceptation Acceptation identifier.
     * @return Whether the acceptation has been properly included.
     *         False if the acceptation is already included in the bunch.
     */
    public static boolean addAcceptationInBunch(Database db, int bunch, int acceptation) {
        if (isAcceptationInBunch(db, bunch, acceptation)) {
            return false;
        }

        LangbookDbInserter.insertBunchAcceptation(db, bunch, acceptation, 0);

        final ImmutableIntSetBuilder allUpdatedBunchesBuilder = new ImmutableIntSetBuilder();
        ImmutableIntSet updatedBunches = new ImmutableIntSetBuilder().add(bunch).build();
        while (!updatedBunches.isEmpty()) {
            ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
            for (int b : updatedBunches) {
                allUpdatedBunchesBuilder.add(b);
                for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, b).entries()) {
                    rerunAgent(db, entry.key(), false);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }

                for (IntPairMap.Entry entry : findAffectedAgentsByItsDiffWithTarget(db, b).entries()) {
                    rerunAgent(db, entry.key(), false);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }
            }
            updatedBunches = builder.build();
        }

        recheckQuizzes(db, allUpdatedBunchesBuilder.build());
        return true;
    }

    public static boolean removeAcceptationFromBunch(Database db, int bunch, int acceptation) {
        if (LangbookDeleter.deleteBunchAcceptation(db, bunch, acceptation)) {
            final ImmutableIntSetBuilder allUpdatedBunchesBuilder = new ImmutableIntSetBuilder();
            ImmutableIntSet updatedBunches = new ImmutableIntSetBuilder().add(bunch).build();
            while (!updatedBunches.isEmpty()) {
                ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
                for (int b : updatedBunches) {
                    allUpdatedBunchesBuilder.add(b);
                    for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, b).entries()) {
                        rerunAgent(db, entry.key(), false);
                        if (entry.value() != 0) {
                            builder.add(entry.value());
                        }
                    }

                    for (IntPairMap.Entry entry : findAffectedAgentsByItsDiffWithTarget(db, b).entries()) {
                        rerunAgent(db, entry.key(), false);
                        if (entry.value() != 0) {
                            builder.add(entry.value());
                        }
                    }
                }
                updatedBunches = builder.build();
            }

            recheckQuizzes(db, allUpdatedBunchesBuilder.build());
            return true;
        }

        return false;
    }

    public static Integer addAgent(Database db, int targetBunch, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableIntKeyMap<String> startMatcher,
            ImmutableIntKeyMap<String> startAdder, ImmutableIntKeyMap<String> endMatcher,
            ImmutableIntKeyMap<String> endAdder, int rule) {
        final int sourceBunchSetId = obtainBunchSet(db, sourceBunches);
        final int diffBunchSetId = obtainBunchSet(db, diffBunches);

        final SyncCacheIntValueMap<ImmutableIntKeyMap<String>> cachedCorrelationIds =
                new SyncCacheIntValueMap<>(corr -> obtainCorrelation(db, corr.map((String str) -> obtainSymbolArray(db, str))));
        final int startMatcherId = cachedCorrelationIds.get(startMatcher);
        final int startAdderId = cachedCorrelationIds.get(startAdder);
        final int endMatcherId = cachedCorrelationIds.get(endMatcher);
        final int endAdderId = cachedCorrelationIds.get(endAdder);

        final LangbookReadableDatabase.AgentRegister register;
        try {
            register = new LangbookReadableDatabase.AgentRegister(targetBunch, sourceBunchSetId,
                    diffBunchSetId, startMatcherId, startAdderId, endMatcherId, endAdderId, rule);
        }
        catch (IllegalArgumentException e) {
            return null;
        }

        final Integer agentId = LangbookDbInserter.insertAgent(db, register);
        if (agentId != null) {
            final AgentDetails details = new AgentDetails(targetBunch, sourceBunches, diffBunches,
                        startMatcher, startAdder, endMatcher, endAdder, rule);
            runAgent(db, agentId, details);
        }

        ImmutableIntSet updatedBunches = new ImmutableIntSetBuilder().add(targetBunch).build();
        while (!updatedBunches.isEmpty()) {
            ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
            for (int bunch : updatedBunches) {
                for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, bunch).entries()) {
                    rerunAgent(db, entry.key(), false);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }
            }
            updatedBunches = builder.build();
        }

        return agentId;
    }

    public static void removeAgent(Database db, int agentId) {
        // This implementation has lot of holes.
        // 1. It is assuming that there is no chained agents
        // 2. It is assuming that agents sets only contains a single agent.
        // TODO: Improve this logic once it is centralised and better defined

        final int targetBunch = getAgentDetails(db, agentId).targetBunch;
        final ImmutableIntKeyMap<ImmutableIntSet> agentSets = getAllAgentSetsContaining(db, agentId);
        final ImmutableIntSetBuilder removableAgentSetsBuilder = new ImmutableIntSetBuilder();
        for (IntKeyMap.Entry<ImmutableIntSet> entry : agentSets.entries()) {
            if (entry.value().size() == 1) {
                removableAgentSetsBuilder.add(entry.key());
            }
            else {
                throw new UnsupportedOperationException("Unimplemented: Multiple agents");
            }
        }

        for (int setId : removableAgentSetsBuilder.build()) {
            if (!deleteBunchAcceptationsForAgentSet(db, setId)) {
                throw new AssertionError();
            }

            if (!deleteAgentSet(db, setId)) {
                throw new AssertionError();
            }
        }

        final ImmutableIntSet ruledAcceptations = getAllRuledAcceptationsForAgent(db, agentId);
        for (int ruleAcceptation : ruledAcceptations) {
            if (!deleteStringQueriesForDynamicAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }

            if (!deleteRuledAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }
        }

        if (!LangbookDeleter.deleteAgent(db, agentId)) {
            throw new AssertionError();
        }

        ImmutableIntSet updatedBunches = new ImmutableIntSetBuilder().add(targetBunch).build();
        while (!updatedBunches.isEmpty()) {
            ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
            for (int bunch : updatedBunches) {
                for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, bunch).entries()) {
                    rerunAgent(db, entry.key(), false);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }
            }
            updatedBunches = builder.build();
        }
    }

    public static Integer obtainQuiz(Database db, int bunch, ImmutableList<LangbookReadableDatabase.QuestionFieldDetails> fields) {
        final Integer existingSetId = findQuestionFieldSet(db, fields);
        final Integer existingQuizId = (existingSetId != null)? findQuizDefinition(db, bunch, existingSetId) : null;

        final Integer quizId;
        if (existingQuizId == null) {
            final ImmutableIntSet acceptations = readAllPossibleAcceptations(db, bunch, fields.toSet());
            final int setId = (existingSetId != null) ? existingSetId : insertQuestionFieldSet(db, fields);
            quizId = insertQuizDefinition(db, bunch, setId);
            insertAllPossibilities(db, quizId, acceptations);
        }
        else {
            quizId = existingQuizId;
        }

        return quizId;
    }

    private static void recheckPossibleQuestions(Database db, int quizId) {
        final QuizDetails quiz = getQuizDetails(db, quizId);
        final ImmutableIntSet possibleAcceptations = readAllPossibleAcceptations(db, quiz.bunch, quiz.fields.toSet());
        final ImmutableIntSet registeredAcceptations = getCurrentKnowledge(db, quizId).keySet();

        for (int acceptation : registeredAcceptations.filterNot(possibleAcceptations::contains)) {
            if (!deleteKnowledge(db, quizId, acceptation)) {
                throw new AssertionError();
            }
        }

        insertAllPossibilities(db, quizId, possibleAcceptations.filterNot(registeredAcceptations::contains));
    }

    public static void updateSearchHistory(Database db, int acceptation) {
        deleteSearchHistoryForAcceptation(db, acceptation);
        insertSearchHistoryEntry(db, acceptation);
    }

    public static void removeQuiz(Database db, int quizId) {
        deleteKnowledgeForQuiz(db, quizId);
        deleteQuiz(db, quizId);
    }

    public static boolean updateSymbolArray(Database db, int symbolArrayId, String text) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getIdColumnIndex(), symbolArrayId)
                .put(table.getStrColumnIndex(), text)
                .build();
        return db.update(query);
    }

    public static boolean removeSentenceMeaning(Database db, int symbolArrayId) {
        final ImmutableIntSet others = findSentenceIdsMatchingMeaning(db, symbolArrayId);
        final boolean result = deleteSentenceMeaning(db, symbolArrayId);
        if (result && others.size() == 1) {
            deleteSentenceMeaning(db, others.valueAt(0));
        }

        return result;
    }

    public static boolean removeSentence(Database db, int symbolArrayId) {
        removeSentenceMeaning(db, symbolArrayId);
        deleteSpanBySymbolArrayId(db, symbolArrayId);
        return isSymbolArrayMerelyASentence(db, symbolArrayId) && deleteSymbolArray(db, symbolArrayId);
    }

    /**
     * Replaces copies the meaning assigned to the one symbol array to another.
     *
     * This will ensure that both sentences share the same meaning after
     * calling this method.
     *
     * This will override any meaning in the target and will remove any meaning
     * from other symbol arrays if required in order to keep clean the table.
     *
     * In case the source symbol array is not found in the sentence-meaning
     * table, a new one will be assigned to both source and target, in order to
     * ensure that both share the same meaning.
     *
     * @param db Database to be updated
     * @param sourceSymbolArray Source identifier for the symbol array.
     * @param targetSymbolArray Target identifier for the symbol array.
     * @return True if any update has been performed in the database, false if
     * source and target already were sharing its meaning.
     */
    public static boolean copySentenceMeaning(Database db, int sourceSymbolArray, int targetSymbolArray) {
        final Integer foundMeaning = getSentenceMeaning(db, sourceSymbolArray);
        final int meaning;
        boolean dbUpdated = false;
        if (foundMeaning == null) {
            meaning = getMaxSentenceMeaning(db) + 1;
            insertSentenceMeaning(db, sourceSymbolArray, meaning);
            dbUpdated = true;
        }
        else {
            meaning = foundMeaning;
        }

        final Integer foundTargetMeaning = getSentenceMeaning(db, targetSymbolArray);
        if (foundTargetMeaning == null || foundTargetMeaning != meaning) {
            removeSentenceMeaning(db, meaning);
            insertSentenceMeaning(db, targetSymbolArray, meaning);
            dbUpdated = true;
        }

        return dbUpdated;
    }
}
