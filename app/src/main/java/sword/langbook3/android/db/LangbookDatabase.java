package sword.langbook3.android.db;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntList;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.List;
import sword.collections.Map;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntSet;
import sword.collections.Set;
import sword.database.Database;
import sword.database.DbDeleteQuery;
import sword.database.DbExporter;
import sword.database.DbImporter;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbStringValue;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;
import sword.database.Deleter;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.collections.SyncCacheIntPairMap;
import sword.langbook3.android.collections.SyncCacheIntValueMap;
import sword.langbook3.android.collections.SyncCacheMap;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;

import static sword.langbook3.android.collections.EqualUtils.equal;
import static sword.langbook3.android.db.LangbookDbInserter.insertAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertAllPossibilities;
import static sword.langbook3.android.db.LangbookDbInserter.insertAlphabet;
import static sword.langbook3.android.db.LangbookDbInserter.insertBunchAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertConceptCompositionEntry;
import static sword.langbook3.android.db.LangbookDbInserter.insertQuizDefinition;
import static sword.langbook3.android.db.LangbookDbInserter.insertRuledAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertSearchHistoryEntry;
import static sword.langbook3.android.db.LangbookDbInserter.insertSentence;
import static sword.langbook3.android.db.LangbookDbInserter.insertSpan;
import static sword.langbook3.android.db.LangbookDbInserter.insertStringQuery;
import static sword.langbook3.android.db.LangbookDbInserter.insertSymbolArray;
import static sword.langbook3.android.db.LangbookDbSchema.MAX_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.MIN_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.db.LangbookDeleter.deleteAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteAlphabet;
import static sword.langbook3.android.db.LangbookDeleter.deleteAlphabetFromCorrelations;
import static sword.langbook3.android.db.LangbookDeleter.deleteAlphabetFromStringQueries;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunch;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchAcceptationsByAgent;
import static sword.langbook3.android.db.LangbookDeleter.deleteComplementedConcept;
import static sword.langbook3.android.db.LangbookDeleter.deleteConversion;
import static sword.langbook3.android.db.LangbookDeleter.deleteKnowledge;
import static sword.langbook3.android.db.LangbookDeleter.deleteKnowledgeForQuiz;
import static sword.langbook3.android.db.LangbookDeleter.deleteQuiz;
import static sword.langbook3.android.db.LangbookDeleter.deleteRuledAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteSearchHistoryForAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteSentence;
import static sword.langbook3.android.db.LangbookDeleter.deleteSpan;
import static sword.langbook3.android.db.LangbookDeleter.deleteSpansByDynamicAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteSpansBySentenceId;
import static sword.langbook3.android.db.LangbookDeleter.deleteStringQueriesForDynamicAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteSymbolArray;
import static sword.langbook3.android.db.LangbookReadableDatabase.alphabetsWithinLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.areAllAlphabetsFromSameLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.checkConversionConflicts;
import static sword.langbook3.android.db.LangbookReadableDatabase.conceptFromAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.correlationArrayFromAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAffectedAgentsByAcceptationCorrelationModification;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAffectedAgentsByItsDiffWithTarget;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAffectedAgentsByItsSourceWithTarget;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAgentsWithoutSourceBunches;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAgentsWithoutSourceBunchesWithTarget;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAlphabetsByLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.findBunchConceptsLinkedToJustThisLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.findBunchSet;
import static sword.langbook3.android.db.LangbookReadableDatabase.findCorrelation;
import static sword.langbook3.android.db.LangbookReadableDatabase.findCorrelationArray;
import static sword.langbook3.android.db.LangbookReadableDatabase.findIncludedAcceptationLanguages;
import static sword.langbook3.android.db.LangbookReadableDatabase.findQuestionFieldSet;
import static sword.langbook3.android.db.LangbookReadableDatabase.findQuizDefinition;
import static sword.langbook3.android.db.LangbookReadableDatabase.findQuizzesByBunch;
import static sword.langbook3.android.db.LangbookReadableDatabase.findSentencesBySymbolArrayId;
import static sword.langbook3.android.db.LangbookReadableDatabase.findSuperTypesLinkedToJustThisLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.findSymbolArray;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAcceptationsInBunchByBunchAndAgent;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAgentDetails;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAgentProcessedMap;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAllRuledAcceptationsForAgent;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAlphabetAndLanguageConcepts;
import static sword.langbook3.android.db.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.db.LangbookReadableDatabase.getConversionsMap;
import static sword.langbook3.android.db.LangbookReadableDatabase.getCorrelationWithText;
import static sword.langbook3.android.db.LangbookReadableDatabase.getCurrentKnowledge;
import static sword.langbook3.android.db.LangbookReadableDatabase.getLanguageFromAlphabet;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxConcept;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxCorrelationArrayId;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxCorrelationId;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxQuestionFieldSetId;
import static sword.langbook3.android.db.LangbookReadableDatabase.getQuizDetails;
import static sword.langbook3.android.db.LangbookReadableDatabase.getSentenceConcept;
import static sword.langbook3.android.db.LangbookReadableDatabase.getSentenceSpansWithIds;
import static sword.langbook3.android.db.LangbookReadableDatabase.getSentenceSymbolArray;
import static sword.langbook3.android.db.LangbookReadableDatabase.hasAgentsRequiringAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.isAcceptationInBunch;
import static sword.langbook3.android.db.LangbookReadableDatabase.isAcceptationPresent;
import static sword.langbook3.android.db.LangbookReadableDatabase.isAlphabetPresent;
import static sword.langbook3.android.db.LangbookReadableDatabase.isAlphabetUsedInQuestions;
import static sword.langbook3.android.db.LangbookReadableDatabase.isSymbolArrayMerelyASentence;
import static sword.langbook3.android.db.LangbookReadableDatabase.isSymbolArrayPresent;
import static sword.langbook3.android.db.LangbookReadableDatabase.readAcceptationTextsAndMain;
import static sword.langbook3.android.db.LangbookReadableDatabase.readAllPossibleAcceptations;
import static sword.langbook3.android.db.LangbookReadableDatabase.readCorrelationArrayTextAndItsAppliedConversions;
import static sword.langbook3.android.db.LangbookReadableDatabase.readMainAlphabetFromAlphabet;

public final class LangbookDatabase {

    private LangbookDatabase() {
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

    private static int insertBunchSet(DbImporter.Database db, IntSet bunchSet) {
        if (bunchSet.isEmpty()) {
            return 0;
        }

        final int setId = LangbookReadableDatabase.getMaxBunchSetId(db) + 1;
        LangbookDbInserter.insertBunchSet(db, setId, bunchSet);
        return setId;
    }

    private static int obtainBunchSet(DbImporter.Database db, IntSet bunchSet) {
        final Integer id = findBunchSet(db, bunchSet);
        return (id != null)? id : insertBunchSet(db, bunchSet);
    }

    private static int insertRuledConcept(DbImporter.Database db, int rule, int concept) {
        final int ruledConcept = getMaxConcept(db) + 1;
        LangbookDbInserter.insertRuledConcept(db, ruledConcept, rule, concept);
        return ruledConcept;
    }

    public static int obtainRuledConcept(DbImporter.Database db, int rule, int concept) {
        final Integer id = LangbookReadableDatabase.findRuledConcept(db, rule, concept);
        return (id != null)? id : insertRuledConcept(db, rule, concept);
    }

    private static int insertQuestionFieldSet(DbImporter.Database db, Iterable<QuestionFieldDetails> fields) {
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

        final ImmutableIntSet.Builder diffAccBuilder = new ImmutableIntSetCreator();
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
            final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
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
            final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
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
            AgentDetails details, ImmutableIntPairMap conversionMap,
            SyncCacheMap<ImmutableIntPair, Conversion> conversions) {
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
        final int conversionCount = conversionMap.size();
        final IntSet keySet = correlation.keySet();
        for (int conversionIndex = 0; conversionIndex < conversionCount; conversionIndex++) {
            if (keySet.contains(conversionMap.valueAt(conversionIndex))) {
                final ImmutableIntPair pair = new ImmutableIntPair(conversionMap.valueAt(conversionIndex), conversionMap.keyAt(conversionIndex));
                final String result = conversions.get(pair).convert(correlation.get(pair.left));
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
            final ImmutableIntPairMap conversionMap = getConversionsMap(db);
            final SyncCacheMap<ImmutableIntPair, Conversion> conversions = new SyncCacheMap<>(key -> getConversion(db, key));

            final SyncCacheIntPairMap mainAlphabets = new SyncCacheIntPairMap(key -> readMainAlphabetFromAlphabet(db, key));
            final ImmutableIntSet.Builder processedAccBuilder = new ImmutableIntSetCreator();

            for (int acc : matchingAcceptations) {
                final ImmutablePair<ImmutableIntKeyMap<String>, Integer> textsAndMain = readAcceptationTextsAndMain(db, acc);
                final MutableIntKeyMap<String> correlation = textsAndMain.left.mutate();

                final boolean validConversion = applyMatchersAddersAndConversions(correlation, details, conversionMap, conversions);
                if (validConversion) {
                    final ImmutableIntSet conversionTargets = conversionMap.keySet();
                    final ImmutableIntPairMap.Builder corrBuilder = new ImmutableIntPairMap.Builder();
                    for (ImmutableIntKeyMap.Entry<String> entry : correlation.entries()) {
                        if (!conversionTargets.contains(entry.key())) {
                            corrBuilder.put(entry.key(), obtainSymbolArray(db, entry.value()));
                        }
                    }

                    final int correlationId = LangbookDatabase.obtainCorrelation(db, corrBuilder.build());
                    final int correlationArrayId = obtainSimpleCorrelationArray(db, correlationId);

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
            for (int acc : processedAcceptations) {
                insertBunchAcceptation(db, details.targetBunch, acc, agentId);
            }
        }
    }

    /**
     * Run again the specified agent.
     * @return The bunch identifier in case the target bunch has changed, or null if there is no change.
     */
    private static Integer rerunAgent(Database db, int agentId, boolean acceptationCorrelationChanged, MutableIntSet deletedDynamicAcceptations) {
        final AgentDetails agentDetails = LangbookReadableDatabase.getAgentDetails(db, agentId);
        final ImmutableIntSet matchingAcceptations = findMatchingAcceptations(db,
                agentDetails.sourceBunches, agentDetails.diffBunches,
                agentDetails.startMatcher, agentDetails.endMatcher);

        boolean targetChanged = false;
        final boolean ruleApplied = agentDetails.modifyCorrelations();
        final ImmutableIntSet processedAcceptations;
        if (!ruleApplied) {
            final ImmutableIntSet alreadyProcessedAcceptations = getAcceptationsInBunchByBunchAndAgent(db, agentDetails.targetBunch, agentId);

            for (int acc : alreadyProcessedAcceptations) {
                if (!matchingAcceptations.contains(acc)) {
                    if (!deleteBunchAcceptation(db, agentDetails.targetBunch, acc, agentId)) {
                        throw new AssertionError();
                    }
                    targetChanged = true;
                }
            }
            processedAcceptations = matchingAcceptations.filterNot(alreadyProcessedAcceptations::contains);
        }
        else {
            // This is assuming that matcher, adder, rule and flags did not change from last run,
            // only its source and diff bunches and its contents
            final ImmutableIntSet toBeProcessed;
            if (acceptationCorrelationChanged) {
                for (IntPairMap.Entry accPair : getAgentProcessedMap(db, agentId).entries()) {
                    final int acc = accPair.value();
                    deleteKnowledge(db, acc);
                    deleteBunchAcceptation(db, agentDetails.targetBunch, acc, agentId);
                    deleteStringQueriesForDynamicAcceptation(db, acc);
                    if (!deleteAcceptation(db, acc) | !deleteRuledAcceptation(db, acc)) {
                        throw new AssertionError();
                    }

                    if (deletedDynamicAcceptations != null) {
                        deletedDynamicAcceptations.add(acc);
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
                        deleteBunchAcceptation(db, agentDetails.targetBunch, acc, agentId);
                        deleteStringQueriesForDynamicAcceptation(db, acc);
                        if (!deleteAcceptation(db, acc) | !deleteRuledAcceptation(db, acc)) {
                            throw new AssertionError();
                        }

                        if (deletedDynamicAcceptations != null) {
                            deletedDynamicAcceptations.add(acc);
                        }

                        targetChanged = true;
                    }
                }
            }

            final ImmutableIntPairMap conversionMap = getConversionsMap(db);
            final SyncCacheMap<ImmutableIntPair, Conversion> conversions = new SyncCacheMap<>(key -> getConversion(db, key));

            final SyncCacheIntPairMap mainAlphabets = new SyncCacheIntPairMap(key -> readMainAlphabetFromAlphabet(db, key));
            final ImmutableIntSet.Builder processedAccBuilder = new ImmutableIntSetCreator();
            for (int acc : toBeProcessed) {
                final ImmutablePair<ImmutableIntKeyMap<String>, Integer> textsAndMain = readAcceptationTextsAndMain(db, acc);
                final MutableIntKeyMap<String> correlation = textsAndMain.left.mutate();

                final boolean validConversion = applyMatchersAddersAndConversions(correlation, agentDetails, conversionMap, conversions);
                if (validConversion) {
                    final ImmutableIntSet conversionTargets = conversionMap.keySet();
                    final ImmutableIntPairMap.Builder corrBuilder = new ImmutableIntPairMap.Builder();
                    for (ImmutableIntKeyMap.Entry<String> entry : correlation.entries()) {
                        if (!conversionTargets.contains(entry.key())) {
                            corrBuilder.put(entry.key(), obtainSymbolArray(db, entry.value()));
                        }
                    }

                    final int correlationId = LangbookDatabase.obtainCorrelation(db, corrBuilder.build());
                    final int correlationArrayId = obtainSimpleCorrelationArray(db, correlationId);
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
            for (int acc : processedAcceptations) {
                insertBunchAcceptation(db, agentDetails.targetBunch, acc, agentId);
                targetChanged = true;
            }
        }

        return (targetChanged && agentDetails.targetBunch != NO_BUNCH)? agentDetails.targetBunch : null;
    }

    private static Integer addAcceptation(Database db, int concept, int correlationArrayId) {
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
            rerunAgent(db, agentId, false, null);
        }

        return acceptation;
    }

    static Integer addAcceptation(Database db, int concept, ImmutableList<ImmutableIntKeyMap<String>> correlationArray) {
        final int correlationArrayId = obtainCorrelationArray(db, correlationArray.mapToInt(correlation -> obtainCorrelation(db, correlation)));
        return addAcceptation(db, concept, correlationArrayId);
    }

    private static boolean updateAcceptationCorrelationArray(Database db, int acceptation, int newCorrelationArrayId) {
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

            final ImmutableIntSet.Builder touchedBunchesBuilder = new ImmutableIntSetCreator();

            final ImmutableIntSet.Builder affectedAgents = new ImmutableIntSetCreator();
            for (int agentId : findAgentsWithoutSourceBunches(db)) {
                affectedAgents.add(agentId);
            }

            for (int agentId : findAffectedAgentsByAcceptationCorrelationModification(db, acceptation)) {
                affectedAgents.add(agentId);
            }

            for (int agentId : affectedAgents.build()) {
                final Integer touchedBunch = rerunAgent(db, agentId, true, null);
                if (touchedBunch != null) {
                    touchedBunchesBuilder.add(touchedBunch);
                }
            }
            final ImmutableIntSet touchedBunches = touchedBunchesBuilder.build();

            final ImmutableIntSet.Builder quizIdsBuilder = new ImmutableIntSetCreator();
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

    static boolean updateAcceptationCorrelationArray(Database db, int acceptation, ImmutableList<ImmutableIntKeyMap<String>> correlationArray) {
        final int correlationArrayId = obtainCorrelationArray(db, correlationArray.mapToInt(correlation -> obtainCorrelation(db, correlation)));
        return updateAcceptationCorrelationArray(db, acceptation, correlationArrayId);
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

    private static void removeFromSentenceSpans(Database db, int acceptation) {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .build();

        db.delete(query);
    }

    static boolean removeAcceptation(Database db, int acceptation) {
        final int concept = conceptFromAcceptation(db, acceptation);
        final boolean withoutSynonymsOrTranslations = LangbookReadableDatabase.findAcceptationsByConcept(db, concept).remove(acceptation).isEmpty();
        if (withoutSynonymsOrTranslations && hasAgentsRequiringAcceptation(db, concept)) {
            return false;
        }

        LangbookDeleter.deleteKnowledge(db, acceptation);
        removeFromBunches(db, acceptation);
        removeFromStringQueryTable(db, acceptation);
        removeFromSentenceSpans(db, acceptation);
        final boolean removed = LangbookDeleter.deleteAcceptation(db, acceptation);
        deleteSearchHistoryForAcceptation(db, acceptation);

        if (removed && withoutSynonymsOrTranslations) {
            deleteBunch(db, concept);
        }

        final ImmutableIntPairMap affectedAgents = findAgentsWithoutSourceBunchesWithTarget(db);
        for (int agent : affectedAgents.keySet()) {
            rerunAgent(db, agent, false, null);
        }

        ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        for (int bunch : affectedAgents) {
            if (bunch != 0) {
                builder.add(bunch);
            }
        }

        ImmutableIntSet updatedBunches = builder.build();
        while (!updatedBunches.isEmpty()) {
            builder = new ImmutableIntSetCreator();
            for (int bunch : updatedBunches) {
                for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, bunch).entries()) {
                    rerunAgent(db, entry.key(), false, null);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }
            }
            updatedBunches = builder.build();
        }
        return removed;
    }

    static boolean removeDefinition(Database db, int complementedConcept) {
        // TODO: This method should remove any orphan concept composition to avoid rubbish
        return deleteComplementedConcept(db, complementedConcept);
    }

    private static void recheckQuizzes(Database db, ImmutableIntSet updatedBunches) {
        final ImmutableIntSet.Builder affectedQuizzesBuilder = new ImmutableIntSetCreator();
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
    static boolean addAcceptationInBunch(Database db, int bunch, int acceptation) {
        if (isAcceptationInBunch(db, bunch, acceptation)) {
            return false;
        }

        LangbookDbInserter.insertBunchAcceptation(db, bunch, acceptation, 0);

        final ImmutableIntSet.Builder allUpdatedBunchesBuilder = new ImmutableIntSetCreator();
        ImmutableIntSet updatedBunches = new ImmutableIntSetCreator().add(bunch).build();
        MutableIntSet removedDynamicAcceptations = MutableIntArraySet.empty();
        while (!updatedBunches.isEmpty()) {
            final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
            for (int b : updatedBunches) {
                allUpdatedBunchesBuilder.add(b);
                for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, b).entries()) {
                    rerunAgent(db, entry.key(), false, removedDynamicAcceptations);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }

                for (IntPairMap.Entry entry : findAffectedAgentsByItsDiffWithTarget(db, b).entries()) {
                    rerunAgent(db, entry.key(), false, removedDynamicAcceptations);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }
            }
            updatedBunches = builder.build();
        }

        for (int dynAcc : removedDynamicAcceptations) {
            deleteSpansByDynamicAcceptation(db, dynAcc);
        }

        recheckQuizzes(db, allUpdatedBunchesBuilder.build());
        return true;
    }

    static boolean removeAcceptationFromBunch(Database db, int bunch, int acceptation) {
        if (LangbookDeleter.deleteBunchAcceptation(db, bunch, acceptation, 0)) {
            final ImmutableIntSet.Builder allUpdatedBunchesBuilder = new ImmutableIntSetCreator();
            ImmutableIntSet updatedBunches = new ImmutableIntSetCreator().add(bunch).build();
            MutableIntSet removedDynamicAcceptations = MutableIntArraySet.empty();
            while (!updatedBunches.isEmpty()) {
                final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
                for (int b : updatedBunches) {
                    allUpdatedBunchesBuilder.add(b);
                    for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, b).entries()) {
                        rerunAgent(db, entry.key(), false, removedDynamicAcceptations);
                        if (entry.value() != 0) {
                            builder.add(entry.value());
                        }
                    }

                    for (IntPairMap.Entry entry : findAffectedAgentsByItsDiffWithTarget(db, b).entries()) {
                        rerunAgent(db, entry.key(), false, removedDynamicAcceptations);
                        if (entry.value() != 0) {
                            builder.add(entry.value());
                        }
                    }
                }
                updatedBunches = builder.build();
            }

            for (int dynAcc : removedDynamicAcceptations) {
                deleteSpansByDynamicAcceptation(db, dynAcc);
            }

            recheckQuizzes(db, allUpdatedBunchesBuilder.build());
            return true;
        }

        return false;
    }

    static Integer addAgent(Database db, int targetBunch, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableIntKeyMap<String> startMatcher,
            ImmutableIntKeyMap<String> startAdder, ImmutableIntKeyMap<String> endMatcher,
            ImmutableIntKeyMap<String> endAdder, int rule) {
        final int sourceBunchSetId = obtainBunchSet(db, sourceBunches);
        final int diffBunchSetId = obtainBunchSet(db, diffBunches);

        final SyncCacheIntValueMap<ImmutableIntKeyMap<String>> cachedCorrelationIds =
                new SyncCacheIntValueMap<>(corr -> obtainCorrelation(db, corr.mapToInt(str -> obtainSymbolArray(db, str))));
        final int startMatcherId = cachedCorrelationIds.get(startMatcher);
        final int startAdderId = cachedCorrelationIds.get(startAdder);
        final int endMatcherId = cachedCorrelationIds.get(endMatcher);
        final int endAdderId = cachedCorrelationIds.get(endAdder);

        final AgentRegister register;
        try {
            register = new AgentRegister(targetBunch, sourceBunchSetId,
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

        ImmutableIntSet updatedBunches = new ImmutableIntSetCreator().add(targetBunch).build();
        while (!updatedBunches.isEmpty()) {
            final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
            for (int bunch : updatedBunches) {
                for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, bunch).entries()) {
                    rerunAgent(db, entry.key(), false, null);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }
            }
            updatedBunches = builder.build();
        }

        return agentId;
    }

    static void removeAgent(Database db, int agentId) {
        // This implementation has lot of holes.
        // 1. It is assuming that there is no chained agents
        // 2. It is assuming that agents sets only contains a single agent.
        // TODO: Improve this logic once it is centralised and better defined

        deleteBunchAcceptationsByAgent(db, agentId);
        final int targetBunch = getAgentDetails(db, agentId).targetBunch;

        final ImmutableIntSet ruledAcceptations = getAllRuledAcceptationsForAgent(db, agentId);
        for (int ruleAcceptation : ruledAcceptations) {
            if (!deleteStringQueriesForDynamicAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }

            if (!deleteRuledAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }

            deleteSpansByDynamicAcceptation(db, ruleAcceptation);
        }

        if (!LangbookDeleter.deleteAgent(db, agentId)) {
            throw new AssertionError();
        }

        ImmutableIntSet updatedBunches = new ImmutableIntSetCreator().add(targetBunch).build();
        while (!updatedBunches.isEmpty()) {
            ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
            for (int bunch : updatedBunches) {
                for (IntPairMap.Entry entry : findAffectedAgentsByItsSourceWithTarget(db, bunch).entries()) {
                    rerunAgent(db, entry.key(), false, null);
                    if (entry.value() != 0) {
                        builder.add(entry.value());
                    }
                }
            }
            updatedBunches = builder.build();
        }
    }

    static Integer obtainQuiz(Database db, int bunch, ImmutableList<QuestionFieldDetails> fields) {
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

    static void updateSearchHistory(Database db, int acceptation) {
        deleteSearchHistoryForAcceptation(db, acceptation);
        insertSearchHistoryEntry(db, acceptation);
    }

    static void removeQuiz(Database db, int quizId) {
        deleteKnowledgeForQuiz(db, quizId);
        deleteQuiz(db, quizId);
    }

    private static boolean updateSymbolArray(Database db, int symbolArrayId, String text) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getIdColumnIndex(), symbolArrayId)
                .put(table.getStrColumnIndex(), text)
                .build();
        return db.update(query);
    }

    private static boolean updateSentenceConcept(Database db, int sentenceId, int concept) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getIdColumnIndex(), sentenceId)
                .put(table.getConceptColumnIndex(), concept)
                .build();
        return db.update(query);
    }

    private static boolean updateSentenceSymbolArrayId(Database db, int sentenceId, int newSymbolArrayId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getIdColumnIndex(), sentenceId)
                .put(table.getSymbolArrayColumnIndex(), newSymbolArrayId)
                .build();
        return db.update(query);
    }

    static boolean removeSentence(Database db, int sentenceId) {
        final Integer symbolArrayIdOpt = getSentenceSymbolArray(db, sentenceId);
        if (symbolArrayIdOpt == null || !deleteSentence(db, sentenceId)) {
            return false;
        }

        deleteSpansBySentenceId(db, sentenceId);
        if (isSymbolArrayMerelyASentence(db, symbolArrayIdOpt) &&
                findSentencesBySymbolArrayId(db, symbolArrayIdOpt).isEmpty() &&
                !deleteSymbolArray(db, symbolArrayIdOpt)) {
            throw new AssertionError();
        }

        return true;
    }

    static boolean copySentenceConcept(Database db, int sourceSentenceId, int targetSentenceId) {
        final Integer concept = getSentenceConcept(db, sourceSentenceId);
        return concept != null && updateSentenceConcept(db, targetSentenceId, concept);
    }

    private static Conversion updateJustConversion(Database db, Conversion newConversion) {
        final Conversion oldConversion = getConversion(db, newConversion.getAlphabets());

        final ImmutableSet<Map.Entry<String, String>> oldPairs = oldConversion.getMap().entries();
        final ImmutableSet<Map.Entry<String, String>> newPairs = newConversion.getMap().entries();

        final ImmutableSet<Map.Entry<String, String>> pairsToRemove = oldPairs.filterNot(newPairs::contains);
        final ImmutableSet<Map.Entry<String, String>> pairsToInclude = newPairs.filterNot(oldPairs::contains);

        for (Map.Entry<String, String> pair : pairsToRemove) {
            // TODO: SymbolArrays should also be removed if not used by any other, just to avoid dirty databases
            final int sourceId = findSymbolArray(db, pair.key());
            final int targetId = findSymbolArray(db, pair.value());
            if (!LangbookDeleter.deleteConversionRegister(db, newConversion.getAlphabets(), sourceId, targetId)) {
                throw new AssertionError();
            }
        }

        for (Map.Entry<String, String> pair : pairsToInclude) {
            final int sourceId = obtainSymbolArray(db, pair.key());
            final int targetId = obtainSymbolArray(db, pair.value());
            LangbookDbInserter.insertConversion(db, newConversion.getSourceAlphabet(), newConversion.getTargetAlphabet(), sourceId, targetId);
        }

        return oldConversion;
    }

    private static void updateStringQueryTableDueToConversionUpdate(Database db, Conversion newConversion) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;

        final int offset = table.columns().size();
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getDynamicAcceptationColumnIndex(), table.getDynamicAcceptationColumnIndex())
                .where(table.getStringAlphabetColumnIndex(), newConversion.getTargetAlphabet())
                .where(offset + table.getStringAlphabetColumnIndex(), newConversion.getSourceAlphabet())
                .select(table.getDynamicAcceptationColumnIndex(), table.getStringColumnIndex(), offset + table.getStringColumnIndex());

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        final DbResult dbResult = db.select(query);
        while (dbResult.hasNext()) {
            final List<DbValue> row = dbResult.next();
            final int dynAcc = row.get(0).toInt();
            final String source = row.get(2).toText();
            final String currentTarget = row.get(1).toText();
            final String newTarget = newConversion.convert(source);
            if (!equal(currentTarget, newTarget)) {
                builder.put(dynAcc, newTarget);
            }
        }

        final ImmutableIntKeyMap<String> updateMap = builder.build();
        final int updateCount = updateMap.size();
        for (int i = 0; i < updateCount; i++) {
            final DbUpdateQuery upQuery = new DbUpdateQuery.Builder(table)
                    .where(table.getStringAlphabetColumnIndex(), newConversion.getTargetAlphabet())
                    .where(table.getDynamicAcceptationColumnIndex(), updateMap.keyAt(i))
                    .put(table.getStringColumnIndex(), updateMap.valueAt(i))
                    .build();

            if (!db.update(upQuery)) {
                throw new AssertionError();
            }
        }
    }

    static boolean removeAlphabet(Database db, int alphabet) {
        // There must be at least another alphabet in the same language to avoid leaving the language without alphabets
        if (alphabetsWithinLanguage(db, alphabet).size() < 2) {
            return false;
        }

        final ImmutableIntPairMap conversionMap = getConversionsMap(db);
        if (conversionMap.contains(alphabet)) {
            return false;
        }

        if (isAlphabetUsedInQuestions(db, alphabet)) {
            return false;
        }

        boolean changed = false;
        if (conversionMap.keySet().contains(alphabet)) {
            if (!deleteConversion(db, conversionMap.get(alphabet), alphabet)) {
                throw new AssertionError();
            }
            changed = true;
        }

        changed |= deleteAlphabetFromStringQueries(db, alphabet);
        changed |= deleteAlphabetFromCorrelations(db, alphabet);
        changed |= deleteAlphabet(db, alphabet);
        return changed;
    }

    /**
     * Add a new language for the given code.
     *
     * This method can return null if the language cannot be added,
     * Usually because the code provided is not valid or already exists in the database.
     *
     * @param db Database where the language has to be included.
     * @param code 2-char lowercase language code. Such as "es" for Spanish, "en" for English of "ja" for Japanese.
     * @return A pair containing the language created concept in the left and its main alphabet on its right, or null if it cannot be added.
     */
    public static LanguageCreationResult addLanguage(Database db, String code) {
        if (LangbookReadableDatabase.findLanguageByCode(db, code) != null) {
            return null;
        }

        final int language = getMaxConcept(db) + 1;
        final int alphabet = language + 1;
        LangbookDbInserter.insertLanguage(db, language, code, alphabet);
        insertAlphabet(db, alphabet, language);

        return new LanguageCreationResult(language, alphabet);
    }

    static boolean removeLanguage(Database db, int language) {
        // For now, if there is a bunch whose concept is only linked to acceptations of the language to be removed,
        // the removal is rejected, as there will not be any way to access that bunch any more in an AcceptationsDetailsActivity.
        // Only exception to the previous rule is the case where all acceptations within the bunch belongs to the language that is about to be removed.
        final ImmutableIntSet linkedBunches = findBunchConceptsLinkedToJustThisLanguage(db, language);
        if (!linkedBunches.isEmpty()) {
            if (linkedBunches.anyMatch(bunch -> {
                final ImmutableIntSet languages = findIncludedAcceptationLanguages(db, bunch);
                return languages.size() > 1 || languages.size() == 1 && languages.valueAt(0) != language;
            })) {
                return false;
            }
        }

        // For now, if there is a super type whose concept is only linked to acceptations of the language to be removed,
        // the removal is rejected, as there will not be any way to access that supertype any more in an AcceptationsDetailsActivity
        if (!findSuperTypesLinkedToJustThisLanguage(db, language).isEmpty()) {
            return false;
        }

        final ImmutableIntSet correlationIds = LangbookReadableDatabase.findCorrelationsByLanguage(db, language);
        final ImmutableIntSet correlationUsedInAgents = LangbookReadableDatabase.findCorrelationsUsedInAgents(db);

        // For now, if there are agents using affected correlations. This rejects to remove the language
        if (!correlationIds.filter(correlationUsedInAgents::contains).isEmpty()) {
            return false;
        }

        final ImmutableIntSet acceptationIds = LangbookReadableDatabase.findAcceptationsByLanguage(db, language);
        for (int acceptation : acceptationIds) {
            if (!removeAcceptation(db, acceptation)) {
                throw new AssertionError();
            }
        }

        final ImmutableIntSet alphabets = findAlphabetsByLanguage(db, language);
        final ImmutableIntPairMap conversionMap = getConversionsMap(db);
        final int size = conversionMap.size();
        for (int i = 0; i < size; i++) {
            final int sourceAlphabet = conversionMap.valueAt(i);
            if (alphabets.contains(sourceAlphabet)) {
                final int targetAlphabet = conversionMap.keyAt(i);
                if (!replaceConversion(db, new Conversion(sourceAlphabet, targetAlphabet, ImmutableHashMap.empty()))) {
                    throw new AssertionError();
                }
            }
        }

        if (!LangbookDeleter.deleteAlphabetsForLanguage(db, language) || !LangbookDeleter.deleteLanguage(db, language)) {
            throw new AssertionError();
        }

        return true;
    }

    /**
     * Add a new alphabet to this database as a copy of the given sourceAlphabet.
     *
     * This method will check for any correlation using the sourceAlphabet and will
     * create within the same correlation a new entry for the new created alphabet,
     * pointing to the same symbol array.
     *
     * This method allows to link directly the concept of an already inserted acceptation with as a new alphabet.
     * If there is no predefined alphabet reference to be used in this method, maybe the method
     * {@link #addAlphabetCopyingFromOther(sword.database.Database, int)} should be called instead.
     *
     * If all is OK, the new alphabet will be linked to the same language that the sourceAlphabet is.
     *
     * @param db Database where the new alphabet has to be included.
     * @param alphabet The identifier for this new alphabet to be added.
     *                 This must not exist already as an alphabet or language,
     *                 but it can be a concept within an acceptation.
     * @param sourceAlphabet Existing alphabet that will be cloned. This cannot be the target of a conversion.
     * @return true if the alphabet has been successfully added, and so, the database content has change.
     */
    static boolean addAlphabetCopyingFromOther(Database db, int alphabet, int sourceAlphabet) {
        if (LangbookReadableDatabase.isAlphabetPresent(db, alphabet)) {
            return false;
        }

        if (LangbookReadableDatabase.isLanguagePresent(db, alphabet)) {
            return false;
        }

        final Integer languageOpt = getLanguageFromAlphabet(db, sourceAlphabet);
        if (languageOpt == null) {
            return false;
        }

        if (LangbookReadableDatabase.getConversionsMap(db).keySet().contains(sourceAlphabet)) {
            return false;
        }

        final int language = languageOpt;
        insertAlphabet(db, alphabet, language);

        final ImmutableIntPairMap correlations = LangbookReadableDatabase.findCorrelationsAndSymbolArrayForAlphabet(db, sourceAlphabet);
        final int correlationCount = correlations.size();
        for (int i = 0; i < correlationCount; i++) {
            LangbookDbInserter.insertCorrelationEntry(db, correlations.keyAt(i), alphabet, correlations.valueAt(i));
        }

        // Some kind of query for duplicating rows should be valuable. The following logic will be broken if a new column is added or removed for the table.
        //TODO: Change this logic
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringAlphabetColumnIndex(), sourceAlphabet)
                .select(table.getMainAcceptationColumnIndex(), table.getDynamicAcceptationColumnIndex(),
                        table.getStringColumnIndex(), table.getMainStringColumnIndex());

        final List<StringQueryTableRow> rows = db.select(query).map(row -> new StringQueryTableRow(row.get(0).toInt(), row.get(1).toInt(), row.get(2).toText(), row.get(3).toText())).toList();
        for (StringQueryTableRow row : rows) {
            LangbookDbInserter.insertStringQuery(db, row.text, row.mainText, row.mainAcceptation, row.dynamicAcceptation, alphabet);
        }

        return true;
    }

    private static int obtainConceptComposition(DbImporter.Database db, ImmutableIntSet concepts) {
        final Integer compositionConcept = LangbookReadableDatabase.findConceptComposition(db, concepts);
        if (compositionConcept == null) {
            int newCompositionConcept = getMaxConcept(db) + 1;
            if (concepts.max() >= newCompositionConcept) {
                newCompositionConcept = concepts.max() + 1;
            }

            for (int item : concepts) {
                insertConceptCompositionEntry(db, newCompositionConcept, item);
            }

            return newCompositionConcept;
        }

        return compositionConcept;
    }

    public static void addDefinition(DbImporter.Database db, int baseConcept, int concept, ImmutableIntSet complements) {
        LangbookDbInserter.insertComplementedConcept(db, baseConcept, concept, obtainConceptComposition(db, complements));
    }

    private static boolean checkValidTextAndSpans(String text, Set<SentenceSpan> spans) {
        if (text == null || text.length() == 0) {
            return false;
        }
        final int textLength = text.length();

        if (spans == null) {
            spans = ImmutableHashSet.empty();
        }

        for (SentenceSpan span : spans) {
            if (span == null || span.range.min() < 0 || span.range.max() >= textLength) {
                return false;
            }
        }

        final Set<SentenceSpan> sortedSpans = spans.sort((a, b) -> a.range.min() < b.range.min());
        final int spanCount = sortedSpans.size();

        for (int i = 1; i < spanCount; i++) {
            if (spans.valueAt(i - 1).range.max() >= spans.valueAt(i).range.min()) {
                return false;
            }
        }

        return true;
    }

    static Integer addSentence(Database db, int concept, String text, Set<SentenceSpan> spans) {
        if (!checkValidTextAndSpans(text, spans)) {
            return null;
        }

        final int symbolArray = obtainSymbolArray(db, text);
        final int sentenceId = insertSentence(db, concept, symbolArray);
        for (SentenceSpan span : spans) {
            insertSpan(db, sentenceId, span.range, span.acceptation);
        }
        return sentenceId;
    }

    static boolean updateSentenceTextAndSpans(Database db, int sentenceId, String newText, Set<SentenceSpan> newSpans) {
        final Integer oldSymbolArrayIdOpt = getSentenceSymbolArray(db, sentenceId);
        if (oldSymbolArrayIdOpt == null || !checkValidTextAndSpans(newText, newSpans)) {
            return false;
        }

        final int oldSymbolArrayId = oldSymbolArrayIdOpt;
        final ImmutableIntValueMap<SentenceSpan> oldSpanMap = getSentenceSpansWithIds(db, sentenceId);

        final Integer foundSymbolArrayId = findSymbolArray(db, newText);
        final boolean oldSymbolArrayOnlyUsedHere = isSymbolArrayMerelyASentence(db, oldSymbolArrayId) && findSentencesBySymbolArrayId(db, oldSymbolArrayId).size() == 1;
        final int newSymbolArrayId;
        if (foundSymbolArrayId == null) {
            if (oldSymbolArrayOnlyUsedHere) {
                if (!updateSymbolArray(db, oldSymbolArrayId, newText)) {
                    throw new AssertionError();
                }
                newSymbolArrayId = oldSymbolArrayId;
            }
            else {
                newSymbolArrayId = insertSymbolArray(db, newText);
            }
        }
        else {
            if (oldSymbolArrayOnlyUsedHere && !deleteSymbolArray(db, oldSymbolArrayId)) {
                throw new AssertionError();
            }
            newSymbolArrayId = foundSymbolArrayId;
        }

        if (newSymbolArrayId != oldSymbolArrayId && !updateSentenceSymbolArrayId(db, sentenceId, newSymbolArrayId)) {
            throw new AssertionError();
        }

        final ImmutableSet<SentenceSpan> oldSpans = oldSpanMap.keySet();
        for (SentenceSpan span : oldSpans.filterNot(newSpans::contains)) {
            if (!deleteSpan(db, oldSpanMap.get(span))) {
                throw new AssertionError();
            }
        }

        for (SentenceSpan span : newSpans.filterNot(oldSpans::contains)) {
            insertSpan(db, sentenceId, span.range, span.acceptation);
        }

        return true;
    }

    private static final class StringQueryTableRow {
        final int mainAcceptation;
        final int dynamicAcceptation;
        final String text;
        final String mainText;

        StringQueryTableRow(int mainAcceptation, int dynamicAcceptation, String text, String mainText) {
            this.mainAcceptation = mainAcceptation;
            this.dynamicAcceptation = dynamicAcceptation;
            this.text = text;
            this.mainText = mainText;
        }
    }

    public static void applyConversion(DbImporter.Database db, Conversion conversion) {
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

    private static void unapplyConversion(Deleter db, Conversion conversion) {
        final int targetAlphabet = conversion.getTargetAlphabet();

        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getStringAlphabetColumnIndex(), targetAlphabet)
                .build();

        db.delete(query);
    }

    private static void updateConceptsInComplementedConcepts(Database db, int oldConcept, int newConcept) {
        final LangbookDbSchema.ComplementedConceptsTable table = LangbookDbSchema.Tables.complementedConcepts;

        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBaseColumnIndex(), oldConcept)
                .put(table.getBaseColumnIndex(), newConcept)
                .build();
        db.update(query);

        query = new DbUpdateQuery.Builder(table)
                .where(table.getIdColumnIndex(), oldConcept)
                .put(table.getIdColumnIndex(), newConcept)
                .build();
        db.update(query);

        query = new DbUpdateQuery.Builder(table)
                .where(table.getComplementColumnIndex(), oldConcept)
                .put(table.getComplementColumnIndex(), newConcept)
                .build();
        db.update(query);
    }

    private static void updateBunchAcceptationConcepts(Database db, int oldConcept, int newConcept) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldConcept)
                .put(table.getBunchColumnIndex(), newConcept)
                .build();
        db.update(query);
    }

    private static void updateQuestionRules(Database db, int oldRule, int newRule) {
        final LangbookDbSchema.QuestionFieldSets table = LangbookDbSchema.Tables.questionFieldSets;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getRuleColumnIndex(), oldRule)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        db.update(query);
    }

    private static void updateQuizBunches(Database db, int oldBunch, int newBunch) {
        final LangbookDbSchema.QuizDefinitionsTable table = LangbookDbSchema.Tables.quizDefinitions;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldBunch)
                .put(table.getBunchColumnIndex(), newBunch)
                .build();
        db.update(query);
    }

    private static void updateBunchSetBunches(Database db, int oldBunch, int newBunch) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldBunch)
                .put(table.getBunchColumnIndex(), newBunch)
                .build();
        db.update(query);
    }

    private static void updateAgentRules(Database db, int oldRule, int newRule) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getRuleColumnIndex(), oldRule)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        db.update(query);
    }

    private static void updateAgentTargetBunches(Database db, int oldBunch, int newBunch) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getTargetBunchColumnIndex(), oldBunch)
                .put(table.getTargetBunchColumnIndex(), newBunch)
                .build();
        db.update(query);
    }

    private static void updateAcceptationConcepts(Database db, int oldConcept, int newConcept) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getConceptColumnIndex(), oldConcept)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        db.update(query);
    }

    static void updateScore(Database db, int quizId, int acceptation, int score) {
        if (score < MIN_ALLOWED_SCORE || score > MAX_ALLOWED_SCORE) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.KnowledgeTable table = LangbookDbSchema.Tables.knowledge;
        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getQuizDefinitionColumnIndex(), quizId)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .put(table.getScoreColumnIndex(), score)
                .build();
        db.update(query);
    }

    /**
     * Join 2 concepts in a single one, removing any reference to the given old concept.
     *
     * This method extracts the concept from the given acceptation and replace
     * any reference to the oldConcept for the extracted acceptation concept in the database.
     *
     * @param db Database to be updated.
     * @param linkedAcceptation Acceptation from where the concept will be extracted.
     * @param oldConcept Concept to be replaced by the linked one.
     * @return Whether the database has changed.
     */
    static boolean shareConcept(Database db, int linkedAcceptation, int oldConcept) {
        final int linkedConcept = conceptFromAcceptation(db, linkedAcceptation);
        if (oldConcept == linkedConcept) {
            return false;
        }

        final ImmutableIntSet nonLinkableConcepts = getAlphabetAndLanguageConcepts(db);
        if (nonLinkableConcepts.contains(linkedConcept)) {
            return false;
        }

        if (oldConcept == 0 || linkedConcept == 0) {
            throw new AssertionError();
        }

        updateConceptsInComplementedConcepts(db, oldConcept, linkedConcept);
        updateBunchAcceptationConcepts(db, oldConcept, linkedConcept);
        updateQuestionRules(db, oldConcept, linkedConcept);
        updateQuizBunches(db, oldConcept, linkedConcept);
        updateBunchSetBunches(db, oldConcept, linkedConcept);
        updateAgentRules(db, oldConcept, linkedConcept);
        updateAgentTargetBunches(db, oldConcept, linkedConcept);
        updateAcceptationConcepts(db, oldConcept, linkedConcept);
        return true;
    }

    /**
     * Extract the correlation array assigned to the given linkedAcceptation and
     * creates a new acceptation with the same correlation array but with the given concept.
     * @param db Database where the new acceptation has to be inserted.
     * @param linkedAcceptation Acceptation from where the correlation array reference has to be copied.
     * @param concept Concept to be applied to the new acceptation created.
     */
    static void duplicateAcceptationWithThisConcept(Database db, int linkedAcceptation, int concept) {
        if (concept == 0) {
            throw new AssertionError();
        }

        final int correlationArray = correlationArrayFromAcceptation(db, linkedAcceptation);
        addAcceptation(db, concept, correlationArray);
    }

    /**
     * Add a new alphabet and a new conversion at once, being the resulting alphabet the target of the given conversion.
     * @param db Database where the alphabet and conversion will be included.
     * @param conversion Conversion to be evaluated and stored if no conflicts are found.
     * @return Whether the action was completed successfully, and so the database state content has changed.
     */
    static boolean addAlphabetAsConversionTarget(Database db, Conversion conversion) {
        final Integer languageOpt = getLanguageFromAlphabet(db, conversion.getSourceAlphabet());
        if (languageOpt == null) {
            return false;
        }

        if (isAlphabetPresent(db, conversion.getTargetAlphabet())) {
            return false;
        }

        final int language = languageOpt;
        if (!checkConversionConflicts(db, conversion)) {
            return false;
        }

        insertAlphabet(db, conversion.getTargetAlphabet(), language);

        if (!updateJustConversion(db, conversion).getMap().isEmpty()) {
            throw new AssertionError();
        }

        applyConversion(db, conversion);
        return true;
    }

    /**
     * Replace a conversion in the database, insert a new one if non existing, or remove and existing one if the given is empty.
     * This will trigger the update of any word where this conversion may apply.
     * This will fail if the alphabets for the conversions are not existing or they do not belong to the same language.
     *
     * @param db Database where the conversion has to be replaces and where words related must be adjusted.
     * @param conversion New conversion to be included.
     * @return True if something changed in the database. Usually false in case the new conversion cannot be applied.
     */
    static boolean replaceConversion(Database db, Conversion conversion) {
        final int sourceAlphabet = conversion.getSourceAlphabet();
        final int targetAlphabet = conversion.getTargetAlphabet();
        final Integer languageObj = getLanguageFromAlphabet(db, sourceAlphabet);
        if (languageObj == null) {
            return false;
        }

        final Integer languageObj2 = getLanguageFromAlphabet(db, targetAlphabet);
        if (languageObj2 == null || languageObj2.intValue() != languageObj.intValue()) {
            return false;
        }

        if (conversion.getMap().isEmpty()) {
            final Conversion oldConversion = getConversion(db, conversion.getAlphabets());
            if (oldConversion.getMap().isEmpty()) {
                return false;
            }
            else {
                unapplyConversion(db, oldConversion);
                updateJustConversion(db, conversion);
                return true;
            }
        }

        if (checkConversionConflicts(db, conversion)) {
            final Conversion oldConversion = updateJustConversion(db, conversion);
            if (oldConversion.getMap().isEmpty()) {
                applyConversion(db, conversion);
            }
            else {
                updateStringQueryTableDueToConversionUpdate(db, conversion);
            }
            return true;
        }

        return false;
    }

    /**
     * Add a new correlation to the database.
     *
     * This method will fail if the keys within the correlation map does not match valid alphabets,
     * alphabets are not from the same language, or any of the symbol array reference is wrong.
     *
     * @param db Database where the correlation should be included.
     * @param correlation IntPairMap whose keys are alphabets and values are symbol arrays identifiers.
     * @return An identifier for the new correlation included, or null in case of error.
     */
    public static Integer obtainCorrelation(DbImporter.Database db, IntPairMap correlation) {
        final Integer foundId = findCorrelation(db, correlation);
        if (foundId != null) {
            return foundId;
        }

        if (!areAllAlphabetsFromSameLanguage(db, correlation.keySet())) {
            return null;
        }

        if (correlation.anyMatch(strId -> !isSymbolArrayPresent(db, strId))) {
            return null;
        }

        final int newCorrelationId = getMaxCorrelationId(db) + 1;
        LangbookDbInserter.insertCorrelation(db, newCorrelationId, correlation);
        return newCorrelationId;
    }

    /**
     * Add a new correlation to the database.
     *
     * This method will fail if the keys within the correlation map does not match valid alphabets,
     * or alphabets are not from the same language.
     *
     * @param db Database where the correlation should be included.
     * @param correlation IntKeyMap whose keys are alphabets and values are symbol arrays to be included as well.
     * @return An identifier for the new correlation included, or null in case of error.
     */
    public static Integer obtainCorrelation(DbImporter.Database db, IntKeyMap<String> correlation) {
        if (correlation.anyMatch(str -> findSymbolArray(db, str) == null)) {
            if (!areAllAlphabetsFromSameLanguage(db, correlation.keySet())) {
                return null;
            }

            final int newCorrelationId = getMaxCorrelationId(db) + 1;
            LangbookDbInserter.insertCorrelation(db, newCorrelationId, correlation.mapToInt(str -> obtainSymbolArray(db, str)));
            return newCorrelationId;
        }

        return obtainCorrelation(db, correlation.mapToInt(str -> obtainSymbolArray(db, str)));
    }

    /**
     * Add a new correlation array to the database.
     *
     * Correlations composing a correlation array:
     * <li>must contain alphabets from the same language in relation with other correlations.</li>
     * <li>must not include alphabets that are target of a conversion.</li>
     *
     * In addition the resulting string of concatenating all symbol arrays must be fully convertible
     * if its alphabets matches the source alphabet of an already entered conversion.
     *
     * This method will return null if any of the conditions said before cannot be achieved.
     *
     * @param db Database where the correlation array will be stored.
     * @param correlations list of correlations to be entered.
     * @return An identifier for the correlation array, or null if it cannot be inserted into the database.
     */
    public static Integer obtainCorrelationArray(DbImporter.Database db, IntList correlations) {
        final Integer foundId = findCorrelationArray(db, correlations);
        if (foundId != null) {
            return foundId;
        }

        if (correlations.isEmpty()) {
            return null;
        }

        final List<ImmutableIntKeyMap<String>> array = correlations.map(id -> getCorrelationWithText(db, id));
        if (array.anyMatch(ImmutableIntKeyMap::isEmpty)) {
            return null;
        }

        final ImmutableIntSet alphabets = array.map(ImmutableIntKeyMap::keySet).reduce(ImmutableIntSet::addAll);
        if (!areAllAlphabetsFromSameLanguage(db, alphabets)) {
            return null;
        }

        final ImmutableIntPairMap conversionMap = getConversionsMap(db);
        if (alphabets.anyMatch(conversionMap.keySet()::contains)) {
            return null;
        }

        for (int alphabet : alphabets) {
            final int index = conversionMap.indexOf(alphabet);
            if (index >= 0) {
                final int targetAlphabet = conversionMap.keyAt(index);
                final Conversion conversion = getConversion(db, new ImmutableIntPair(alphabet, targetAlphabet));
                final String sourceText = array.map(c -> c.get(alphabet, "")).reduce((a, b) -> a + b);
                final String targetText = conversion.convert(sourceText);
                if (targetText == null) {
                    return null;
                }
            }
        }

        final int maxArrayId = getMaxCorrelationArrayId(db);
        final int newArrayId = maxArrayId + ((maxArrayId + 1 != StreamedDatabaseConstants.nullCorrelationArrayId)? 1 : 2);
        LangbookDbInserter.insertCorrelationArray(db, newArrayId, correlations);
        return newArrayId;
    }

    /**
     * Shortcut for {@link #obtainCorrelationArray(DbImporter.Database, IntList)}.
     */
    public static Integer obtainSimpleCorrelationArray(DbImporter.Database db, int correlationId) {
        return obtainCorrelationArray(db, new ImmutableIntList.Builder().append(correlationId).build());
    }

    private static boolean addSpan(DbImporter.Database db, int symbolArray, ImmutableIntRange range, int dynamicAcceptation) {
        if (isSymbolArrayPresent(db, symbolArray) && isAcceptationPresent(db, dynamicAcceptation)) {
            insertSpan(db, symbolArray, range, dynamicAcceptation);
            return true;
        }

        return false;
    }

    private static boolean removeSpan(Deleter db, int id) {
        return deleteSpan(db, id);
    }
}
