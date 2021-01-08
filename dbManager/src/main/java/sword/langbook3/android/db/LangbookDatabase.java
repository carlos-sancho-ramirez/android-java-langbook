package sword.langbook3.android.db;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableIntValueHashMap;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntList;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.IntValueMap;
import sword.collections.List;
import sword.collections.Map;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntPairMap;
import sword.collections.MutableIntSet;
import sword.collections.Set;
import sword.database.Database;
import sword.database.DbDeleteQuery;
import sword.database.DbExporter;
import sword.database.DbImporter;
import sword.database.DbInsertQuery;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbStringValue;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;
import sword.database.Deleter;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
import sword.langbook3.android.collections.SyncCacheIntValueMap;
import sword.langbook3.android.collections.SyncCacheMap;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;
import sword.langbook3.android.models.SentenceSpan;

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
import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.NULL_CORRELATION_ARRAY_ID;
import static sword.langbook3.android.db.LangbookDeleter.deleteAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteAlphabet;
import static sword.langbook3.android.db.LangbookDeleter.deleteAlphabetFromCorrelations;
import static sword.langbook3.android.db.LangbookDeleter.deleteAlphabetFromStringQueries;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunch;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchAcceptationsByAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchAcceptationsByAgent;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchAcceptationsByAgentAndAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchAcceptationsByAgentAndBunch;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchSet;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchSetBunch;
import static sword.langbook3.android.db.LangbookDeleter.deleteComplementedConcept;
import static sword.langbook3.android.db.LangbookDeleter.deleteConversion;
import static sword.langbook3.android.db.LangbookDeleter.deleteCorrelation;
import static sword.langbook3.android.db.LangbookDeleter.deleteCorrelationArray;
import static sword.langbook3.android.db.LangbookDeleter.deleteKnowledge;
import static sword.langbook3.android.db.LangbookDeleter.deleteKnowledgeForQuiz;
import static sword.langbook3.android.db.LangbookDeleter.deleteQuiz;
import static sword.langbook3.android.db.LangbookDeleter.deleteRuledAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteRuledAcceptationByAgent;
import static sword.langbook3.android.db.LangbookDeleter.deleteRuledConcept;
import static sword.langbook3.android.db.LangbookDeleter.deleteSearchHistoryForAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteSentence;
import static sword.langbook3.android.db.LangbookDeleter.deleteSpan;
import static sword.langbook3.android.db.LangbookDeleter.deleteSpansByDynamicAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteSpansBySentenceId;
import static sword.langbook3.android.db.LangbookDeleter.deleteStringQueriesForDynamicAcceptation;
import static sword.langbook3.android.db.LangbookDeleter.deleteSymbolArray;
import static sword.langbook3.android.db.LangbookReadableDatabase.alphabetsWithinLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.checkConversionConflicts;
import static sword.langbook3.android.db.LangbookReadableDatabase.conceptFromAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.correlationArrayFromAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAffectedAgentsByAcceptationCorrelationModification;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAffectedAgentsByItsDiffWithTarget;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAffectedAgentsByItsSourceWithTarget;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAgentsByRule;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAgentsWithoutSourceBunches;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAlphabetsByLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.findBunchConceptsLinkedToJustThisLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.findBunchSet;
import static sword.langbook3.android.db.LangbookReadableDatabase.findCorrelation;
import static sword.langbook3.android.db.LangbookReadableDatabase.findCorrelationArray;
import static sword.langbook3.android.db.LangbookReadableDatabase.findIncludedAcceptationLanguages;
import static sword.langbook3.android.db.LangbookReadableDatabase.findQuestionFieldSet;
import static sword.langbook3.android.db.LangbookReadableDatabase.findQuizDefinition;
import static sword.langbook3.android.db.LangbookReadableDatabase.findQuizzesByBunch;
import static sword.langbook3.android.db.LangbookReadableDatabase.findRuledAcceptationByBaseAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.findRuledConceptsByConceptInvertedMap;
import static sword.langbook3.android.db.LangbookReadableDatabase.findRuledConceptsByRuleInvertedMap;
import static sword.langbook3.android.db.LangbookReadableDatabase.findSentencesBySymbolArrayId;
import static sword.langbook3.android.db.LangbookReadableDatabase.findSuperTypesLinkedToJustThisLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.findSymbolArray;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAcceptationTexts;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAcceptationsInBunchByBunchAndAgent;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAgentExecutionOrder;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAgentProcessedMap;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAgentRegister;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAllRuledAcceptationsForAgent;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAlphabetAndLanguageConcepts;
import static sword.langbook3.android.db.LangbookReadableDatabase.getBunchSet;
import static sword.langbook3.android.db.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.db.LangbookReadableDatabase.getConversionsMap;
import static sword.langbook3.android.db.LangbookReadableDatabase.getCorrelationWithText;
import static sword.langbook3.android.db.LangbookReadableDatabase.getCurrentKnowledge;
import static sword.langbook3.android.db.LangbookReadableDatabase.getFilteredAgentProcessedMap;
import static sword.langbook3.android.db.LangbookReadableDatabase.getLanguageFromAlphabet;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxConcept;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxCorrelationArrayId;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxCorrelationId;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxQuestionFieldSetId;
import static sword.langbook3.android.db.LangbookReadableDatabase.getQuizDetails;
import static sword.langbook3.android.db.LangbookReadableDatabase.getRuleByRuledConcept;
import static sword.langbook3.android.db.LangbookReadableDatabase.getSentenceConcept;
import static sword.langbook3.android.db.LangbookReadableDatabase.getSentenceSpansWithIds;
import static sword.langbook3.android.db.LangbookReadableDatabase.getSentenceSymbolArray;
import static sword.langbook3.android.db.LangbookReadableDatabase.hasAgentsRequiringAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.isAcceptationStaticallyInBunch;
import static sword.langbook3.android.db.LangbookReadableDatabase.isAlphabetPresent;
import static sword.langbook3.android.db.LangbookReadableDatabase.isAlphabetUsedInQuestions;
import static sword.langbook3.android.db.LangbookReadableDatabase.isBunchAcceptationPresentByAgent;
import static sword.langbook3.android.db.LangbookReadableDatabase.isBunchSetInUse;
import static sword.langbook3.android.db.LangbookReadableDatabase.isCorrelationInUse;
import static sword.langbook3.android.db.LangbookReadableDatabase.isSymbolArrayInUse;
import static sword.langbook3.android.db.LangbookReadableDatabase.isSymbolArrayMerelyASentence;
import static sword.langbook3.android.db.LangbookReadableDatabase.isSymbolArrayPresent;
import static sword.langbook3.android.db.LangbookReadableDatabase.readAcceptationTextsAndMain;
import static sword.langbook3.android.db.LangbookReadableDatabase.readAllPossibleAcceptations;
import static sword.langbook3.android.db.LangbookReadableDatabase.readBunchSetsWhereBunchIsIncluded;
import static sword.langbook3.android.db.LangbookReadableDatabase.readCorrelationArrayTextAndItsAppliedConversions;
import static sword.langbook3.android.db.LangbookReadableDatabase.readMainAlphabetFromAlphabet;

public final class LangbookDatabase {

    private LangbookDatabase() {
    }

    private static int obtainSymbolArray(DbImporter.Database db, String str) {
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

    private static int obtainRuledConcept(DbImporter.Database db, int rule, int concept) {
        final Integer id = LangbookReadableDatabase.findRuledConcept(db, rule, concept);
        return (id != null)? id : insertRuledConcept(db, rule, concept);
    }

    private static <AlphabetId extends AlphabetIdInterface> int insertQuestionFieldSet(DbImporter.Database db, Iterable<QuestionFieldDetails<AlphabetId>> fields) {
        if (!fields.iterator().hasNext()) {
            return 0;
        }

        final int setId = getMaxQuestionFieldSetId(db) + 1;
        LangbookDbInserter.insertQuestionFieldSet(db, setId, fields);
        return setId;
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet findMatchingAcceptations(DbExporter.Database db,
            ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> endMatcher) {

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

        final ImmutableSet<AlphabetId> matchingAlphabets = startMatcher.keySet().addAll(endMatcher.keySet());
        final LangbookDbSchema.StringQueriesTable strTable = LangbookDbSchema.Tables.stringQueries;
        for (AlphabetId alphabet : matchingAlphabets) {
            final String startMatch = startMatcher.get(alphabet, "");
            final String endMatch = endMatcher.get(alphabet, "");

            final boolean matchWordStarting = startMatch.length() > endMatch.length();
            final String queryValue = matchWordStarting? startMatch : endMatch;
            final int restrictionType = matchWordStarting? DbQuery.RestrictionStringTypes.STARTS_WITH : DbQuery.RestrictionStringTypes.ENDS_WITH;
            final DbQuery.Builder queryBuilder = new DbQuery.Builder(strTable);
            alphabet.where(strTable.getStringAlphabetColumnIndex(), queryBuilder);
            final DbQuery matchQuery = queryBuilder
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

    private static <AlphabetId extends AlphabetIdInterface> boolean applyMatchersAddersAndConversions(
            MutableCorrelation<AlphabetId> correlation,
            AgentDetails<AlphabetId> details, ImmutableMap<AlphabetId, AlphabetId> conversionMap,
            SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions) {
        final ImmutableSet<AlphabetId> correlationAlphabets = correlation.keySet().toImmutable();
        for (Map.Entry<AlphabetId, String> entry : details.startMatcher.entries()) {
            final AlphabetId key = entry.key();
            if (!correlationAlphabets.contains(key)) {
                return false;
            }

            final int length = entry.value().length();
            final String text = correlation.get(key).substring(length);
            correlation.put(key, text);
        }

        for (Map.Entry<AlphabetId, String> entry : details.startAdder.entries()) {
            final AlphabetId key = entry.key();
            if (!correlationAlphabets.contains(key)) {
                return false;
            }

            correlation.put(key, entry.value() + correlation.get(key));
        }

        for (Map.Entry<AlphabetId, String> entry : details.endMatcher.entries()) {
            final AlphabetId key = entry.key();
            if (!correlationAlphabets.contains(key)) {
                return false;
            }

            final int length = entry.value().length();
            String text = correlation.get(key);
            text = text.substring(0, text.length() - length);
            correlation.put(key, text);
        }

        for (Map.Entry<AlphabetId, String> entry : details.endAdder.entries()) {
            final AlphabetId key = entry.key();
            if (!correlationAlphabets.contains(key)) {
                return false;
            }

            correlation.put(key, correlation.get(key) + entry.value());
        }

        boolean validConversion = true;
        final int conversionCount = conversionMap.size();
        final Set<AlphabetId> keySet = correlation.keySet();
        for (int conversionIndex = 0; conversionIndex < conversionCount; conversionIndex++) {
            if (keySet.contains(conversionMap.valueAt(conversionIndex))) {
                final ImmutablePair<AlphabetId, AlphabetId> pair = new ImmutablePair<>(conversionMap.valueAt(conversionIndex), conversionMap.keyAt(conversionIndex));
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

    private static <AlphabetId extends AlphabetIdInterface> void runAgent(Database db, IntSetter<AlphabetId> alphabetIntSetter, int agentId, AgentDetails<AlphabetId> details) {
        final ImmutableIntSet matchingAcceptations = findMatchingAcceptations(db, details.sourceBunches, details.diffBunches, details.startMatcher, details.endMatcher);
        final ImmutableIntSet processedAcceptations;
        if (!details.modifyCorrelations()) {
            processedAcceptations = matchingAcceptations;
        }
        else {
            final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap(db, alphabetIntSetter);
            final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions = new SyncCacheMap<>(key -> getConversion(db, key));

            final SyncCacheMap<AlphabetId, AlphabetId> mainAlphabets = new SyncCacheMap<>(key -> readMainAlphabetFromAlphabet(db, alphabetIntSetter, key));
            final ImmutableIntSet.Builder processedAccBuilder = new ImmutableIntSetCreator();

            for (int acc : matchingAcceptations) {
                final ImmutablePair<ImmutableCorrelation<AlphabetId>, Integer> textsAndMain = readAcceptationTextsAndMain(db, alphabetIntSetter, acc);
                final MutableCorrelation<AlphabetId> correlation = textsAndMain.left.mutate();

                final boolean validConversion = applyMatchersAddersAndConversions(correlation, details, conversionMap, conversions);
                if (validConversion) {
                    final ImmutableSet<AlphabetId> conversionTargets = conversionMap.keySet();
                    final ImmutableIntValueMap.Builder<AlphabetId> corrBuilder = new ImmutableIntValueHashMap.Builder<>();
                    for (ImmutableMap.Entry<AlphabetId, String> entry : correlation.entries()) {
                        if (!conversionTargets.contains(entry.key())) {
                            corrBuilder.put(entry.key(), obtainSymbolArray(db, entry.value()));
                        }
                    }

                    final int correlationId = LangbookDatabase.obtainCorrelation(db, alphabetIntSetter, corrBuilder.build());
                    final int correlationArrayId = obtainSimpleCorrelationArray(db, alphabetIntSetter, correlationId);

                    final int baseConcept = conceptFromAcceptation(db, acc);
                    final int ruledConcept = obtainRuledConcept(db, details.rule, baseConcept);
                    final int newAcc = insertAcceptation(db, ruledConcept, correlationArrayId);
                    insertRuledAcceptation(db, newAcc, agentId, acc);

                    for (Map.Entry<AlphabetId, String> entry : correlation.entries()) {
                        final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                        insertStringQuery(db, entry.value(), mainText, textsAndMain.right, newAcc, entry.key());
                    }
                    processedAccBuilder.add(newAcc);
                }
            }
            processedAcceptations = processedAccBuilder.build();
        }

        for (int targetBunch : details.targetBunches) {
            for (int acc : processedAcceptations) {
                insertBunchAcceptation(db, targetBunch, acc, agentId);
            }
        }
    }

    /**
     * Run again the specified agent.
     * @param sourceAgentChangedText If a ruled is applied, forces the recheck of the resulting texts.
     *                               This should be true at least if one source agent has changed its adders,
     *                               resulting in a different source acceptation to be ruled here.
     * @return A bunch set containing all target bunches that changed, or empty if there is no change.
     */
    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet rerunAgent(Database db, IntSetter<AlphabetId> alphabetIdSetter, int agentId, MutableIntSet deletedDynamicAcceptations, boolean sourceAgentChangedText) {
        final AgentDetails<AlphabetId> agentDetails = LangbookReadableDatabase.getAgentDetails(db, alphabetIdSetter, agentId);
        final ImmutableIntSet matchingAcceptations = findMatchingAcceptations(db,
                agentDetails.sourceBunches, agentDetails.diffBunches,
                agentDetails.startMatcher, agentDetails.endMatcher);

        boolean targetChanged = false;
        final boolean ruleApplied = agentDetails.modifyCorrelations();
        final ImmutableIntPairMap processedAcceptationsMap;
        if (!ruleApplied) {
            final ImmutableIntKeyMap<ImmutableIntSet> alreadyProcessedAcceptations;
            // TODO: Ruled concept should also be removed if they are not used by other agent
            if (deleteRuledAcceptationByAgent(db, agentId)) {
                deleteBunchAcceptationsByAgent(db, agentId);
                targetChanged = true;
                alreadyProcessedAcceptations = ImmutableIntKeyMap.empty();
            }
            else {
                alreadyProcessedAcceptations = agentDetails.targetBunches
                        .assign(targetBunch -> getAcceptationsInBunchByBunchAndAgent(db, targetBunch, agentId));
            }

            for (int targetBunch : alreadyProcessedAcceptations.keySet()) {
                for (int acc : alreadyProcessedAcceptations.get(targetBunch)) {
                    if (!matchingAcceptations.contains(acc)) {
                        if (!deleteBunchAcceptation(db, targetBunch, acc, agentId)) {
                            throw new AssertionError();
                        }
                        targetChanged = true;
                    }
                }
            }

            final ImmutableIntSet allAlreadyProcessedAcceptations = alreadyProcessedAcceptations
                    .reduce((a, b) -> a.filter(b::contains), ImmutableIntArraySet.empty());
            final ImmutableIntSet processedAcceptations = matchingAcceptations.filterNot(allAlreadyProcessedAcceptations::contains);
            processedAcceptationsMap = processedAcceptations.assignToInt(key -> key);
        }
        else {
            final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap(db, alphabetIdSetter);
            final ImmutableSet<AlphabetId> conversionTargets = conversionMap.keySet();
            final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions = new SyncCacheMap<>(key -> getConversion(db, key));
            final SyncCacheMap<AlphabetId, AlphabetId> mainAlphabets = new SyncCacheMap<>(key -> readMainAlphabetFromAlphabet(db, alphabetIdSetter, key));

            // This is assuming that matcher, adder and flags did not change from last run,
            // only its source and diff bunches and its contents
            final ImmutableIntPairMap oldProcessedMap = getAgentProcessedMap(db, agentId);
            final ImmutableIntSet toBeProcessed;
            final ImmutableIntSet alreadyProcessedAcceptations = oldProcessedMap.keySet();
            final boolean noRuleBefore = alreadyProcessedAcceptations.isEmpty() &&
                    isBunchAcceptationPresentByAgent(db, agentId);
            if (noRuleBefore && !deleteBunchAcceptationsByAgent(db, agentId)) {
                throw new AssertionError();
            }

            final int sampleStaticAcc = matchingAcceptations.findFirst(alreadyProcessedAcceptations::contains, 0);
            final boolean hasSameRule;
            final boolean canReuseOldRuledConcept;
            final boolean mustChangeResultingText;
            if (sampleStaticAcc != 0) {
                final int sampleDynAcc = oldProcessedMap.get(sampleStaticAcc);
                final int sampleRuledConcept = LangbookReadableDatabase.conceptFromAcceptation(db, sampleDynAcc);
                final int sampleRule = getRuleByRuledConcept(db, sampleRuledConcept);
                hasSameRule = sampleRule == agentDetails.rule;
                canReuseOldRuledConcept = hasSameRule || findAgentsByRule(db, sampleRule).isEmpty();

                if (sourceAgentChangedText) {
                    mustChangeResultingText = true;
                }
                else {
                    final MutableCorrelation<AlphabetId> accText = getAcceptationTexts(db, alphabetIdSetter, sampleStaticAcc).mutate();
                    final ImmutableCorrelation<AlphabetId> sampleDynAccText = getAcceptationTexts(db, alphabetIdSetter, sampleDynAcc);
                    final boolean validConversion = applyMatchersAddersAndConversions(accText, agentDetails, conversionMap, conversions);
                    mustChangeResultingText = !validConversion || !accText.equalCorrelation(sampleDynAccText);
                }
            }
            else {
                hasSameRule = false;
                canReuseOldRuledConcept = false;
                mustChangeResultingText = sourceAgentChangedText;
            }

            if (!hasSameRule) {
                final ImmutableIntPairMap ruledConceptsInvertedMap = findRuledConceptsByRuleInvertedMap(db, agentDetails.rule);
                for (int staticAcc : matchingAcceptations.filter(alreadyProcessedAcceptations::contains)) {
                    final int dynAcc = oldProcessedMap.get(staticAcc);
                    final int baseConcept = LangbookReadableDatabase.conceptFromAcceptation(db, staticAcc);
                    final int foundRuledConcept = ruledConceptsInvertedMap.get(baseConcept, 0);

                    if (canReuseOldRuledConcept) {
                        final int ruledConcept = LangbookReadableDatabase.conceptFromAcceptation(db, dynAcc);
                        if (foundRuledConcept != 0) {
                            updateAcceptationConcept(db, dynAcc, foundRuledConcept);
                            if (!deleteRuledConcept(db, ruledConcept)) {
                                throw new AssertionError();
                            }
                        }
                        else {
                            final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
                            final DbUpdateQuery updateQuery = new DbUpdateQuery.Builder(table)
                                    .put(table.getRuleColumnIndex(), agentDetails.rule)
                                    .where(table.getIdColumnIndex(), ruledConcept)
                                    .build();
                            if (!db.update(updateQuery)) {
                                throw new AssertionError();
                            }
                        }
                    }
                    else {
                        final int newRuledConcept = (foundRuledConcept != 0)? foundRuledConcept : insertRuledConcept(db, agentDetails.rule, baseConcept);
                        updateAcceptationConcept(db, dynAcc, newRuledConcept);
                    }
                }
            }

            if (mustChangeResultingText) {
                for (int staticAcc : matchingAcceptations.filter(alreadyProcessedAcceptations::contains)) {
                    final MutableCorrelation<AlphabetId> correlation = getAcceptationTexts(db, alphabetIdSetter, staticAcc).mutate();
                    final boolean validConversion = applyMatchersAddersAndConversions(correlation, agentDetails,
                            conversionMap, conversions);
                    if (validConversion) {
                        final ImmutableIntValueMap.Builder<AlphabetId> corrBuilder = new ImmutableIntValueHashMap.Builder<>();
                        for (ImmutableMap.Entry<AlphabetId, String> entry : correlation.entries()) {
                            if (!conversionTargets.contains(entry.key())) {
                                corrBuilder.put(entry.key(), obtainSymbolArray(db, entry.value()));
                            }
                        }

                        final int correlationId = LangbookDatabase.obtainCorrelation(db, alphabetIdSetter, corrBuilder.build());
                        final int correlationArrayId = obtainSimpleCorrelationArray(db, alphabetIdSetter, correlationId);
                        final int dynAcc = oldProcessedMap.get(staticAcc);

                        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
                        DbUpdateQuery updateQuery = new DbUpdateQuery.Builder(table)
                                .put(table.getCorrelationArrayColumnIndex(), correlationArrayId)
                                .where(table.getIdColumnIndex(), dynAcc)
                                .build();
                        if (!db.update(updateQuery)) {
                            throw new AssertionError();
                        }
                        targetChanged = true;

                        for (Map.Entry<AlphabetId, String> entry : correlation.entries()) {
                            final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
                            final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                            final DbUpdateQuery.Builder builder = new DbUpdateQuery.Builder(strings)
                                    .put(strings.getStringColumnIndex(), entry.value())
                                    .put(strings.getMainStringColumnIndex(), mainText)
                                    .where(strings.getDynamicAcceptationColumnIndex(), dynAcc);
                            entry.key().where(strings.getStringAlphabetColumnIndex(), builder);
                            updateQuery = builder.build();
                            if (!db.update(updateQuery)) {
                                throw new AssertionError();
                            }
                        }
                    }
                }
            }

            toBeProcessed = matchingAcceptations.filterNot(alreadyProcessedAcceptations::contains);

            final MutableIntPairMap resultProcessedMap = oldProcessedMap.mutate();
            for (IntPairMap.Entry accPair : oldProcessedMap.entries()) {
                if (!matchingAcceptations.contains(accPair.key())) {
                    final int acc = accPair.value();
                    deleteKnowledge(db, acc);
                    for (int targetBunch : agentDetails.targetBunches) {
                        deleteBunchAcceptation(db, targetBunch, acc, agentId);
                    }
                    deleteStringQueriesForDynamicAcceptation(db, acc);
                    deleteSpansByDynamicAcceptation(db, acc);
                    if (!deleteAcceptation(db, acc) | !deleteRuledAcceptation(db, acc)) {
                        throw new AssertionError();
                    }

                    if (deletedDynamicAcceptations != null) {
                        deletedDynamicAcceptations.add(acc);
                    }

                    targetChanged = true;
                    resultProcessedMap.remove(accPair.key());
                }
            }

            final ImmutableIntPairMap.Builder processedAccMapBuilder = new ImmutableIntPairMap.Builder();
            for (int acc : matchingAcceptations) {
                if (toBeProcessed.contains(acc)) {
                    final ImmutablePair<ImmutableCorrelation<AlphabetId>, Integer> textsAndMain = readAcceptationTextsAndMain(db, alphabetIdSetter, acc);
                    final MutableCorrelation<AlphabetId> correlation = textsAndMain.left.mutate();

                    final boolean validConversion = applyMatchersAddersAndConversions(correlation, agentDetails, conversionMap, conversions);
                    if (validConversion) {
                        final ImmutableIntValueMap.Builder<AlphabetId> corrBuilder = new ImmutableIntValueHashMap.Builder<>();
                        for (ImmutableMap.Entry<AlphabetId, String> entry : correlation.entries()) {
                            if (!conversionTargets.contains(entry.key())) {
                                corrBuilder.put(entry.key(), obtainSymbolArray(db, entry.value()));
                            }
                        }

                        final int correlationId = LangbookDatabase.obtainCorrelation(db, alphabetIdSetter, corrBuilder.build());
                        final int correlationArrayId = obtainSimpleCorrelationArray(db, alphabetIdSetter, correlationId);
                        final int baseConcept = conceptFromAcceptation(db, acc);
                        final int ruledConcept = obtainRuledConcept(db, agentDetails.rule, baseConcept);
                        final int newAcc = insertAcceptation(db, ruledConcept, correlationArrayId);
                        insertRuledAcceptation(db, newAcc, agentId, acc);

                        for (Map.Entry<AlphabetId, String> entry : correlation.entries()) {
                            final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                            insertStringQuery(db, entry.value(), mainText, textsAndMain.right, newAcc, entry.key());
                        }
                        processedAccMapBuilder.put(acc, newAcc);
                    }
                }
                else {
                    processedAccMapBuilder.put(acc, oldProcessedMap.get(acc));
                }
            }
            processedAcceptationsMap = processedAccMapBuilder.build();
        }

        for (int targetBunch : agentDetails.targetBunches) {
            final ImmutableIntSet alreadyIncludedAcceptations = getAcceptationsInBunchByBunchAndAgent(db, targetBunch, agentId);
            for (int acc : processedAcceptationsMap.filterNot(alreadyIncludedAcceptations::contains)) {
                insertBunchAcceptation(db, targetBunch, acc, agentId);
                targetChanged = true;
            }
        }

        return targetChanged? agentDetails.targetBunches : ImmutableIntArraySet.empty();
    }

    private static <AlphabetId extends AlphabetIdInterface> Integer addAcceptation(Database db, IntSetter<AlphabetId> alphabetIdManager, int concept, int correlationArrayId) {
        final Correlation<AlphabetId> texts = readCorrelationArrayTextAndItsAppliedConversions(db, alphabetIdManager, correlationArrayId);
        if (texts == null) {
            return null;
        }

        final String mainStr = texts.valueAt(0);
        final int acceptation = insertAcceptation(db, concept, correlationArrayId);
        for (Map.Entry<AlphabetId, String> entry : texts.entries()) {
            final AlphabetId alphabet = entry.key();
            final String str = entry.value();
            insertStringQuery(db, str, mainStr, acceptation, acceptation, alphabet);
        }

        final ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableIntSet>> sortedAgents = getAgentExecutionOrder(db);
        final ImmutableIntKeyMap<ImmutableIntSet> agentDependencies = sortedAgents.right;
        final MutableIntSet touchedBunches = MutableIntArraySet.empty();
        touchedBunches.add(0);

        for (int agentId : sortedAgents.left) {
            if (!agentDependencies.get(agentId).filter(touchedBunches::contains).isEmpty()) {
                touchedBunches.addAll(rerunAgent(db, alphabetIdManager, agentId, null, false));
            }
        }

        return acceptation;
    }

    static <AlphabetId extends AlphabetIdInterface> Integer addAcceptation(Database db, IntSetter<AlphabetId> alphabetIdManager, int concept, ImmutableCorrelationArray<AlphabetId> correlationArray) {
        final int correlationArrayId = obtainCorrelationArray(db, alphabetIdManager, correlationArray.mapToInt(correlation -> obtainCorrelation(db, alphabetIdManager, correlation)));
        return addAcceptation(db, alphabetIdManager, concept, correlationArrayId);
    }

    private static <AlphabetId extends AlphabetIdInterface> boolean updateAcceptationCorrelationArray(Database db, IntSetter<AlphabetId> alphabetIdManager, int acceptation, int newCorrelationArrayId) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getIdColumnIndex(), acceptation)
                .put(table.getCorrelationArrayColumnIndex(), newCorrelationArrayId)
                .build();
        final boolean changed = db.update(query);
        if (changed) {
            final Correlation<AlphabetId> texts = readCorrelationArrayTextAndItsAppliedConversions(db, alphabetIdManager, newCorrelationArrayId);
            if (texts == null) {
                throw new AssertionError();
            }

            final String mainStr = texts.valueAt(0);
            for (Map.Entry<AlphabetId, String> entry : texts.entries()) {
                final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
                final DbUpdateQuery.Builder builder = new DbUpdateQuery.Builder(strings)
                        .where(strings.getDynamicAcceptationColumnIndex(), acceptation);
                entry.key().where(strings.getStringAlphabetColumnIndex(), builder);
                final DbUpdateQuery updateQuery = builder
                        .put(strings.getMainStringColumnIndex(), mainStr)
                        .put(strings.getStringColumnIndex(), entry.value())
                        .build();

                if (!db.update(updateQuery)) {
                    throw new AssertionError();
                }
            }

            final MutableIntArraySet touchedBunches = MutableIntArraySet.empty();

            final ImmutableIntSet.Builder affectedAgentsBuilder = new ImmutableIntSetCreator();
            for (int agentId : findAgentsWithoutSourceBunches(db)) {
                affectedAgentsBuilder.add(agentId);
            }

            for (int agentId : findAffectedAgentsByAcceptationCorrelationModification(db, acceptation)) {
                affectedAgentsBuilder.add(agentId);
            }

            final ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableIntSet>> agentExecutionOrder = getAgentExecutionOrder(db);
            final ImmutableIntSet affectedAgents = affectedAgentsBuilder.build();
            for (int thisAgentId : agentExecutionOrder.left) {
                final ImmutableIntSet dependencies = agentExecutionOrder.right.get(thisAgentId);
                if (affectedAgents.contains(thisAgentId) || dependencies.anyMatch(touchedBunches::contains)) {
                    touchedBunches.addAll(rerunAgent(db, alphabetIdManager, thisAgentId, null, false));
                }
            }

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
                recheckPossibleQuestions(db, alphabetIdManager, quizId);
            }
        }

        return changed;
    }

    static <AlphabetId extends AlphabetIdInterface> boolean updateAcceptationCorrelationArray(Database db, IntSetter<AlphabetId> alphabetIdManager, int acceptation, ImmutableCorrelationArray<AlphabetId> correlationArray) {
        final int correlationArrayId = obtainCorrelationArray(db, alphabetIdManager, correlationArray.mapToInt(correlation -> obtainCorrelation(db, alphabetIdManager, correlation)));
        return updateAcceptationCorrelationArray(db, alphabetIdManager, acceptation, correlationArrayId);
    }

    private static void removeFromStringQueryTable(Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
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

    private static boolean canAcceptationBeRemoved(Database db, int acceptation) {
        final int concept = conceptFromAcceptation(db, acceptation);
        final boolean withSynonymsOrTranslations = !LangbookReadableDatabase.findAcceptationsByConcept(db, concept).remove(acceptation).isEmpty();
        return (withSynonymsOrTranslations || !hasAgentsRequiringAcceptation(db, concept)) &&
                !findRuledAcceptationByBaseAcceptation(db, acceptation).anyMatch(acc -> !canAcceptationBeRemoved(db, acc));
    }

    private static void removeCorrelationArrayIfUnused(Database db, int correlationArray) {
        if (!LangbookReadableDatabase.isCorrelationArrayInUse(db, correlationArray)) {
            final int[] correlationIds = LangbookReadableDatabase.getCorrelationArray(db, correlationArray);
            if (!deleteCorrelationArray(db, correlationArray)) {
                throw new AssertionError();
            }

            for (int correlationId : correlationIds) {
                if (!isCorrelationInUse(db, correlationId)) {
                    final ImmutableIntSet symbolArrayIds = LangbookReadableDatabase.getCorrelationSymbolArrayIds(db, correlationId);

                    if (!deleteCorrelation(db, correlationId)) {
                        throw new AssertionError();
                    }

                    for (int symbolArrayId : symbolArrayIds) {
                        if (!isSymbolArrayInUse(db, symbolArrayId) && !deleteSymbolArray(db, symbolArrayId)) {
                            throw new AssertionError();
                        }
                    }
                }
            }
        }
    }

    private static boolean removeAcceptationInternal(Database db, int acceptation) {
        if (findRuledAcceptationByBaseAcceptation(db, acceptation).anyMatch(acc -> !removeAcceptationInternal(db, acc))) {
            throw new AssertionError();
        }

        LangbookDeleter.deleteKnowledge(db, acceptation);
        removeFromBunches(db, acceptation);
        removeFromStringQueryTable(db, acceptation);
        removeFromSentenceSpans(db, acceptation);

        final int concept = conceptFromAcceptation(db, acceptation);
        final int correlationArray = correlationArrayFromAcceptation(db, acceptation);
        final boolean removed = LangbookDeleter.deleteAcceptation(db, acceptation);
        deleteSearchHistoryForAcceptation(db, acceptation);

        if (removed) {
            final boolean withoutSynonymsOrTranslations = LangbookReadableDatabase.findAcceptationsByConcept(db, concept).size() <= 1;
            if (withoutSynonymsOrTranslations) {
                deleteBunch(db, concept);
            }

            deleteRuledAcceptation(db, acceptation);
            removeCorrelationArrayIfUnused(db, correlationArray);
        }

        return removed;
    }

    static boolean removeAcceptation(Database db, int acceptation) {
        if (!canAcceptationBeRemoved(db, acceptation)) {
            return false;
        }

        if (!removeAcceptationInternal(db, acceptation)) {
            throw new AssertionError();
        }

        return true;
    }

    static boolean removeDefinition(Database db, int complementedConcept) {
        // TODO: This method should remove any orphan concept composition to avoid rubbish
        return deleteComplementedConcept(db, complementedConcept);
    }

    private static <AlphabetId extends AlphabetIdInterface> void recheckQuizzes(Database db, IntSetter<AlphabetId> alphabetIdManager, ImmutableIntSet updatedBunches) {
        final ImmutableIntSet.Builder affectedQuizzesBuilder = new ImmutableIntSetCreator();
        for (int b : updatedBunches) {
            for (int quizId : findQuizzesByBunch(db, b)) {
                affectedQuizzesBuilder.add(quizId);
            }
        }

        for (int quizId : affectedQuizzesBuilder.build()) {
            recheckPossibleQuestions(db, alphabetIdManager, quizId);
        }
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet findMatchingAcceptationsAmongGiven(DbExporter.Database db,
            IntSetter<AlphabetId> alphabetIdSetter, IntSet acceptations, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableCorrelation<AlphabetId> startMatcher,
            ImmutableCorrelation<AlphabetId> endMatcher) {

        final MutableIntSet filteredAcceptations = acceptations.mutate();
        for (int bunch : diffBunches) {
            final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
            final DbQuery query = new DbQuery.Builder(table)
                    .where(table.getBunchColumnIndex(), bunch)
                    .select(table.getAcceptationColumnIndex());
            try (DbResult result = db.select(query)) {
                while (result.hasNext()) {
                    filteredAcceptations.remove(result.next().get(0).toInt());
                }
            }
        }

        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        if (!sourceBunches.isEmpty()) {
            for (int bunch : sourceBunches) {
                final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
                final DbQuery query = new DbQuery.Builder(table)
                        .where(table.getBunchColumnIndex(), bunch)
                        .select(table.getAcceptationColumnIndex());
                try (DbResult result = db.select(query)) {
                    while (result.hasNext()) {
                        final int acc = result.next().get(0).toInt();
                        if (filteredAcceptations.contains(acc)) {
                            builder.add(acc);
                        }
                    }
                }
            }
        }
        final ImmutableIntSet matchingAcceptations = builder.build();

        final ImmutableSet<AlphabetId> matchingAlphabets = startMatcher.keySet().addAll(endMatcher.keySet());
        for (AlphabetId alphabet : matchingAlphabets) {
            if (startMatcher.get(alphabet, null) == null) {
                startMatcher = startMatcher.put(alphabet, "");
            }

            if (endMatcher.get(alphabet, null) == null) {
                endMatcher = endMatcher.put(alphabet, "");
            }
        }

        final ImmutableCorrelation<AlphabetId> sMatcher = startMatcher;
        final ImmutableCorrelation<AlphabetId> eMatcher = endMatcher;

        return matchingAcceptations.filterNot(acc -> {
            final ImmutableCorrelation<AlphabetId> texts = getAcceptationTexts(db, alphabetIdSetter, acc);
            return matchingAlphabets.anyMatch(alphabet -> {
                final String text = texts.get(alphabet, null);
                return text == null || !text.startsWith(sMatcher.get(alphabet)) || !text.endsWith(eMatcher.get(alphabet));
            });
        });
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet rerunAgentWhenAcceptationIncludedInBunch(Database db, IntSetter<AlphabetId> alphabetIdManager, int agentId, MutableIntSet addedAcceptations) {
        final AgentDetails<AlphabetId> agentDetails = LangbookReadableDatabase.getAgentDetails(db, alphabetIdManager, agentId);
        final ImmutableIntSet matchingAcceptations = findMatchingAcceptationsAmongGiven(db,
                alphabetIdManager, addedAcceptations, agentDetails.sourceBunches, agentDetails.diffBunches,
                agentDetails.startMatcher, agentDetails.endMatcher);

        boolean targetChanged = false;
        final boolean ruleApplied = agentDetails.modifyCorrelations();
        if (!ruleApplied) {
            final ImmutableIntSet acceptationAlreadyInTarget = getAcceptationsInBunchByBunchAndAgent(db, agentDetails.targetBunches.valueAt(0), agentId).filter(addedAcceptations::contains);

            for (int acc : addedAcceptations) {
                final boolean alreadyInTarget = acceptationAlreadyInTarget.contains(acc);
                final boolean isMatching = matchingAcceptations.contains(acc);
                if (isMatching && !alreadyInTarget) {
                    for (int target : agentDetails.targetBunches) {
                        insertBunchAcceptation(db, target, acc, agentId);
                    }

                    targetChanged = true;
                }
                else if (!isMatching && alreadyInTarget) {
                    if (!deleteBunchAcceptationsByAgentAndAcceptation(db, agentId, acc)) {
                        throw new AssertionError();
                    }

                    targetChanged = true;
                }
            }
        }
        else {
            final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap(db, alphabetIdManager);
            final ImmutableSet<AlphabetId> conversionTargets = conversionMap.keySet();
            final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions = new SyncCacheMap<>(key -> getConversion(db, key));
            final SyncCacheMap<AlphabetId, AlphabetId> mainAlphabets = new SyncCacheMap<>(key -> readMainAlphabetFromAlphabet(db, alphabetIdManager, key));

            // This is assuming that matcher, adder, rule and flags did not change from last run,
            // only its source and diff bunches and its contents
            final ImmutableIntPairMap oldProcessedMap = getFilteredAgentProcessedMap(db, agentId, addedAcceptations);
            for (int acc : addedAcceptations.toImmutable()) {
                final boolean isMatching = matchingAcceptations.contains(acc);
                final int dynAcc = oldProcessedMap.get(acc, 0);
                if (!isMatching && dynAcc != 0) {
                    targetChanged |= !agentDetails.targetBunches.isEmpty();
                    removeAcceptationInternal(db, dynAcc);
                }
                else if (isMatching && dynAcc == 0) {
                    final ImmutablePair<ImmutableCorrelation<AlphabetId>, Integer> textsAndMain = readAcceptationTextsAndMain(
                            db, alphabetIdManager, acc);
                    final MutableCorrelation<AlphabetId> correlation = textsAndMain.left.mutate();

                    final boolean validConversion = applyMatchersAddersAndConversions(correlation, agentDetails,
                            conversionMap, conversions);
                    if (validConversion) {
                        final ImmutableIntValueMap.Builder<AlphabetId> corrBuilder = new ImmutableIntValueHashMap.Builder<>();
                        for (ImmutableMap.Entry<AlphabetId, String> entry : correlation.entries()) {
                            if (!conversionTargets.contains(entry.key())) {
                                corrBuilder.put(entry.key(), obtainSymbolArray(db, entry.value()));
                            }
                        }

                        final int correlationId = LangbookDatabase.obtainCorrelation(db, alphabetIdManager, corrBuilder.build());
                        final int correlationArrayId = obtainSimpleCorrelationArray(db, alphabetIdManager, correlationId);
                        final int baseConcept = conceptFromAcceptation(db, acc);
                        final int ruledConcept = obtainRuledConcept(db, agentDetails.rule, baseConcept);
                        final int newAcc = insertAcceptation(db, ruledConcept, correlationArrayId);
                        insertRuledAcceptation(db, newAcc, agentId, acc);
                        addedAcceptations.add(newAcc);

                        for (Map.Entry<AlphabetId, String> entry : correlation.entries()) {
                            final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                            insertStringQuery(db, entry.value(), mainText, textsAndMain.right, newAcc, entry.key());
                        }

                        for (int targetBunch : agentDetails.targetBunches) {
                            insertBunchAcceptation(db, targetBunch, newAcc, agentId);
                            targetChanged = true;
                        }
                    }
                }
            }
        }

        return targetChanged? agentDetails.targetBunches : ImmutableIntArraySet.empty();
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
    static <AlphabetId extends AlphabetIdInterface> boolean addAcceptationInBunch(Database db, IntSetter<AlphabetId> alphabetIdManager, int bunch, int acceptation) {
        if (isAcceptationStaticallyInBunch(db, bunch, acceptation)) {
            return false;
        }

        LangbookDbInserter.insertBunchAcceptation(db, bunch, acceptation, 0);

        final ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableIntSet>> agentSortingResult = getAgentExecutionOrder(db);
        final MutableIntSet updatedBunches = MutableIntArraySet.empty();
        updatedBunches.add(bunch);

        MutableIntSet addedAcceptations = MutableIntArraySet.empty();
        addedAcceptations.add(acceptation);

        for (int agentId : agentSortingResult.left) {
            if (agentSortingResult.right.get(agentId).anyMatch(updatedBunches::contains)) {
                updatedBunches.addAll(rerunAgentWhenAcceptationIncludedInBunch(db, alphabetIdManager, agentId, addedAcceptations));
            }
        }

        recheckQuizzes(db, alphabetIdManager, updatedBunches.toImmutable());
        return true;
    }

    static <AlphabetId extends AlphabetIdInterface> boolean removeAcceptationFromBunch(Database db, IntSetter<AlphabetId> alphabetIdManager, int bunch, int acceptation) {
        if (LangbookDeleter.deleteBunchAcceptation(db, bunch, acceptation, 0)) {
            final ImmutableIntSet.Builder allUpdatedBunchesBuilder = new ImmutableIntSetCreator();
            ImmutableIntSet updatedBunches = new ImmutableIntSetCreator().add(bunch).build();
            MutableIntSet removedDynamicAcceptations = MutableIntArraySet.empty();
            while (!updatedBunches.isEmpty()) {
                final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
                for (int b : updatedBunches) {
                    allUpdatedBunchesBuilder.add(b);
                    for (IntKeyMap.Entry<ImmutableIntSet> entry : findAffectedAgentsByItsSourceWithTarget(db, b).entries()) {
                        rerunAgent(db, alphabetIdManager, entry.key(), removedDynamicAcceptations, false);
                        for (int bb : entry.value()) {
                            builder.add(bb);
                        }
                    }

                    for (IntKeyMap.Entry<ImmutableIntSet> entry : findAffectedAgentsByItsDiffWithTarget(db, b).entries()) {
                        rerunAgent(db, alphabetIdManager, entry.key(), removedDynamicAcceptations, false);
                        for (int bb : entry.value()) {
                            builder.add(bb);
                        }
                    }
                }
                updatedBunches = builder.build();
            }

            for (int dynAcc : removedDynamicAcceptations) {
                deleteSpansByDynamicAcceptation(db, dynAcc);
            }

            recheckQuizzes(db, alphabetIdManager, allUpdatedBunchesBuilder.build());
            return true;
        }

        return false;
    }

    static <AlphabetId extends AlphabetIdInterface> Integer addAgent(Database db, IntSetter<AlphabetId> alphabetIdSetter, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableCorrelation<AlphabetId> startMatcher,
            ImmutableCorrelation<AlphabetId> startAdder, ImmutableCorrelation<AlphabetId> endMatcher,
            ImmutableCorrelation<AlphabetId> endAdder, int rule) {
        if (sourceBunches.anyMatch(diffBunches::contains)) {
            return null;
        }

        final int targetBunchSetId = obtainBunchSet(db, targetBunches);
        final int sourceBunchSetId = obtainBunchSet(db, sourceBunches);
        final int diffBunchSetId = obtainBunchSet(db, diffBunches);

        final SyncCacheIntValueMap<ImmutableCorrelation<AlphabetId>> cachedCorrelationIds =
                new SyncCacheIntValueMap<>(corr -> obtainCorrelation(db, alphabetIdSetter, corr.mapToInt(str -> obtainSymbolArray(db, str))));
        final int startMatcherId = cachedCorrelationIds.get(startMatcher);
        final int startAdderId = cachedCorrelationIds.get(startAdder);
        final int endMatcherId = cachedCorrelationIds.get(endMatcher);
        final int endAdderId = cachedCorrelationIds.get(endAdder);

        final boolean ruleExpected = startMatcherId != startAdderId || endMatcherId != endAdderId;
        final boolean rulePresent = rule != 0;
        if (ruleExpected != rulePresent) {
            return null;
        }

        final AgentRegister register;
        try {
            register = new AgentRegister(targetBunchSetId, sourceBunchSetId,
                    diffBunchSetId, startMatcherId, startAdderId, endMatcherId, endAdderId, rule);
        }
        catch (IllegalArgumentException e) {
            return null;
        }

        final Integer agentId = LangbookDbInserter.insertAgent(db, register);
        if (agentId != null) {
            final AgentDetails<AlphabetId> details = new AgentDetails<>(targetBunches, sourceBunches, diffBunches,
                        startMatcher, startAdder, endMatcher, endAdder, rule);
            runAgent(db, alphabetIdSetter, agentId, details);
        }

        ImmutableIntSet updatedBunches = targetBunches.isEmpty()? new ImmutableIntSetCreator().add(0).build() : targetBunches;
        while (!updatedBunches.isEmpty()) {
            final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
            for (int bunch : updatedBunches) {
                for (IntKeyMap.Entry<ImmutableIntSet> entry : findAffectedAgentsByItsSourceWithTarget(db, bunch).entries()) {
                    rerunAgent(db, alphabetIdSetter, entry.key(), null, false);
                    for (int b : entry.value()) {
                        builder.add(b);
                    }
                }
            }
            updatedBunches = builder.build();
        }

        return agentId;
    }

    static <AlphabetId extends AlphabetIdInterface> boolean updateAgent(Database db, IntSetter<AlphabetId> alphabetIdSetter, int agentId, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableCorrelation<AlphabetId> startMatcher,
            ImmutableCorrelation<AlphabetId> startAdder, ImmutableCorrelation<AlphabetId> endMatcher,
            ImmutableCorrelation<AlphabetId> endAdder, int rule) {
        if (sourceBunches.anyMatch(diffBunches::contains)) {
            return false;
        }

        final AgentRegister register = getAgentRegister(db, agentId);
        if (register == null) {
            return false;
        }

        final MutableIntArraySet touchedBunches = MutableIntArraySet.empty();
        final ImmutableIntSet currentTargetBunches = LangbookReadableDatabase.getBunchSet(db, register.targetBunchSetId);
        for (int targetBunch : currentTargetBunches.filterNot(targetBunches::contains)) {
            if (deleteBunchAcceptationsByAgentAndBunch(db, agentId, targetBunch)) {
                touchedBunches.add(targetBunch);
            }
        }

        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbUpdateQuery.Builder updateQueryBuilder = new DbUpdateQuery.Builder(table);

        boolean somethingChanged = false;
        boolean correlationChanged = false;
        final int targetBunchSetId = obtainBunchSet(db, targetBunches);
        if (targetBunchSetId != register.targetBunchSetId) {
            // TODO: old bunch set should be removed if not used by any other agent, in order to keep clean the database
            updateQueryBuilder.put(table.getTargetBunchSetColumnIndex(), targetBunchSetId);
            somethingChanged = true;
        }

        final int sourceBunchSetId = obtainBunchSet(db, sourceBunches);
        if (sourceBunchSetId != register.sourceBunchSetId) {
            // TODO: old bunch set should be removed if not used by any other agent, in order to keep clean the database
            updateQueryBuilder.put(table.getSourceBunchSetColumnIndex(), sourceBunchSetId);
            somethingChanged = true;
        }

        final int diffBunchSetId = obtainBunchSet(db, diffBunches);
        if (diffBunchSetId != register.diffBunchSetId) {
            // TODO: old bunch set should be removed if not used by any other agent, in order to keep clean the database
            updateQueryBuilder.put(table.getDiffBunchSetColumnIndex(), diffBunchSetId);
            somethingChanged = true;
        }

        final int startMatcherId = obtainCorrelation(db, alphabetIdSetter, startMatcher);
        if (startMatcherId != register.startMatcherId) {
            // TODO: old correlation should be removed if not used by any other acceptation or agent, in order to keep clean the database
            updateQueryBuilder.put(table.getStartMatcherColumnIndex(), startMatcherId);
            correlationChanged = true;
            somethingChanged = true;
        }

        final int startAdderId = obtainCorrelation(db, alphabetIdSetter, startAdder);
        if (startAdderId != register.startAdderId) {
            // TODO: old correlation should be removed if not used by any other acceptation or agent, in order to keep clean the database
            updateQueryBuilder.put(table.getStartAdderColumnIndex(), startAdderId);
            correlationChanged = true;
            somethingChanged = true;
        }

        final int endMatcherId = obtainCorrelation(db, alphabetIdSetter, endMatcher);
        if (endMatcherId != register.endMatcherId) {
            // TODO: old correlation should be removed if not used by any other acceptation or agent, in order to keep clean the database
            updateQueryBuilder.put(table.getEndMatcherColumnIndex(), endMatcherId);
            correlationChanged = true;
            somethingChanged = true;
        }

        final int endAdderId = obtainCorrelation(db, alphabetIdSetter, endAdder);
        if (endAdderId != register.endAdderId) {
            // TODO: old correlation should be removed if not used by any other acceptation or agent, in order to keep clean the database
            updateQueryBuilder.put(table.getEndAdderColumnIndex(), endAdderId);
            correlationChanged = true;
            somethingChanged = true;
        }

        final boolean ruleExpected = startMatcherId != startAdderId || endMatcherId != endAdderId;
        final boolean rulePresent = rule != 0;
        if (ruleExpected != rulePresent) {
            return false;
        }

        if (rule != register.rule) {
            updateQueryBuilder.put(table.getRuleColumnIndex(), rule);
            somethingChanged = true;
        }

        if (!somethingChanged) {
            return true;
        }

        final DbUpdateQuery updateQuery = updateQueryBuilder
                .where(table.getIdColumnIndex(), agentId)
                .build();
        db.update(updateQuery);

        final ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableIntSet>> agentExecutionOrder = getAgentExecutionOrder(db);
        for (int thisAgentId : agentExecutionOrder.left) {
            final ImmutableIntSet dependencies = agentExecutionOrder.right.get(thisAgentId);
            if (thisAgentId == agentId || dependencies.anyMatch(touchedBunches::contains)) {
                final boolean sourceAgentChangedText = thisAgentId != agentId && correlationChanged;
                touchedBunches.addAll(rerunAgent(db, alphabetIdSetter, thisAgentId, null, sourceAgentChangedText));
            }
        }

        final LangbookDbSchema.QuizDefinitionsTable quizzesTable = LangbookDbSchema.Tables.quizDefinitions;
        final DbQuery quizDefQuery = new DbQuery.Builder(quizzesTable)
                .select(quizzesTable.getIdColumnIndex(), quizzesTable.getBunchColumnIndex());

        final MutableIntArraySet quizzesToUpdate = MutableIntArraySet.empty();
        try (DbResult quizResult = db.select(quizDefQuery)) {
            while (quizResult.hasNext()) {
                final List<DbValue> row = quizResult.next();
                final int bunch = row.get(1).toInt();
                if (touchedBunches.contains(bunch)) {
                    quizzesToUpdate.add(row.get(0).toInt());
                }
            }
        }

        final LangbookDbSchema.KnowledgeTable knowledgeTable = LangbookDbSchema.Tables.knowledge;
        for (int quizId : quizzesToUpdate) {
            final QuizDetails<AlphabetId> details = getQuizDetails(db, alphabetIdSetter, quizId);
            final ImmutableIntSet accs = readAllPossibleAcceptations(db, details.bunch, details.fields.toSet());
            final ImmutableIntSet accsInKnowledge = getCurrentKnowledge(db, quizId).keySet();
            for (int acc : accsInKnowledge.filterNot(accs::contains)) {
                final DbDeleteQuery deleteQuery = new DbDeleteQuery.Builder(knowledgeTable)
                        .where(knowledgeTable.getQuizDefinitionColumnIndex(), quizId)
                        .where(knowledgeTable.getAcceptationColumnIndex(), acc)
                        .build();

                if (!db.delete(deleteQuery)) {
                    throw new AssertionError();
                }
            }

            for (int acc : accs.filterNot(accsInKnowledge::contains)) {
                final DbInsertQuery insertQuery = new DbInsertQuery.Builder(knowledgeTable)
                        .put(knowledgeTable.getQuizDefinitionColumnIndex(), quizId)
                        .put(knowledgeTable.getAcceptationColumnIndex(), acc)
                        .put(knowledgeTable.getScoreColumnIndex(), NO_SCORE)
                        .build();

                db.insert(insertQuery);
            }
        }

        return true;
    }

    static <AlphabetId extends AlphabetIdInterface> void removeAgent(Database db, IntSetter<AlphabetId> alphabetIdManager, int agentId) {
        // This implementation has lot of holes.
        // 1. It is assuming that there is no chained agents
        // 2. It is assuming that agents sets only contains a single agent.
        // TODO: Improve this logic once it is centralised and better defined

        deleteBunchAcceptationsByAgent(db, agentId);
        final AgentRegister agentRegister = getAgentRegister(db, agentId);
        final ImmutableIntSet targetBunches = getBunchSet(db, agentRegister.targetBunchSetId);

        final ImmutableIntSet ruledAcceptations = getAllRuledAcceptationsForAgent(db, agentId);
        for (int ruleAcceptation : ruledAcceptations) {
            if (!deleteStringQueriesForDynamicAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }

            if (!deleteRuledAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }

            final int correlationArray = LangbookReadableDatabase.correlationArrayFromAcceptation(db, ruleAcceptation);
            if (!deleteAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }

            deleteSpansByDynamicAcceptation(db, ruleAcceptation);
            removeCorrelationArrayIfUnused(db, correlationArray);
        }

        if (!LangbookDeleter.deleteAgent(db, agentId)) {
            throw new AssertionError();
        }

        if (agentRegister.targetBunchSetId != 0 &&
                !isBunchSetInUse(db, agentRegister.targetBunchSetId) &&
                !deleteBunchSet(db, agentRegister.targetBunchSetId)) {
            throw new AssertionError();
        }

        if (agentRegister.sourceBunchSetId != 0 &&
                !isBunchSetInUse(db, agentRegister.sourceBunchSetId) &&
                !deleteBunchSet(db, agentRegister.sourceBunchSetId)) {
            throw new AssertionError();
        }

        if (agentRegister.diffBunchSetId != 0 &&
                !isBunchSetInUse(db, agentRegister.diffBunchSetId) &&
                !deleteBunchSet(db, agentRegister.diffBunchSetId)) {
            throw new AssertionError();
        }

        ImmutableIntSet updatedBunches = targetBunches;
        while (!updatedBunches.isEmpty()) {
            ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
            for (int bunch : updatedBunches) {
                for (IntKeyMap.Entry<ImmutableIntSet> entry : findAffectedAgentsByItsSourceWithTarget(db, bunch).entries()) {
                    rerunAgent(db, alphabetIdManager, entry.key(), null, false);
                    for (int b : entry.value()) {
                        builder.add(b);
                    }
                }
            }
            updatedBunches = builder.build();
        }
    }

    static <AlphabetId extends AlphabetIdInterface> Integer obtainQuiz(Database db, IntSetter<AlphabetId> alphabetIntSetter, int bunch, ImmutableList<QuestionFieldDetails<AlphabetId>> fields) {
        final Integer existingSetId = findQuestionFieldSet(db, alphabetIntSetter, fields);
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

    private static <AlphabetId extends AlphabetIdInterface> void recheckPossibleQuestions(Database db, IntSetter<AlphabetId> alphabetIntSetter, int quizId) {
        final QuizDetails<AlphabetId> quiz = getQuizDetails(db, alphabetIntSetter, quizId);
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

    private static <AlphabetId extends AlphabetIdInterface> Conversion<AlphabetId> updateJustConversion(Database db, Conversion<AlphabetId> newConversion) {
        final Conversion<AlphabetId> oldConversion = getConversion(db, newConversion.getAlphabets());

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

    private static <AlphabetId extends AlphabetIdInterface> void updateStringQueryTableDueToConversionUpdate(Database db, Conversion<AlphabetId> newConversion) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;

        final int offset = table.columns().size();
        final DbQuery.Builder queryBuilder = new DbQuery.Builder(table)
                .join(table, table.getDynamicAcceptationColumnIndex(), table.getDynamicAcceptationColumnIndex());
        newConversion.getTargetAlphabet().where(table.getStringAlphabetColumnIndex(), queryBuilder);
        newConversion.getSourceAlphabet().where(offset + table.getStringAlphabetColumnIndex(), queryBuilder);
        final DbQuery query = queryBuilder.select(
                table.getDynamicAcceptationColumnIndex(),
                table.getStringColumnIndex(),
                offset + table.getStringColumnIndex());

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
            final DbUpdateQuery.Builder updateQueryBuilder = new DbUpdateQuery.Builder(table);
            newConversion.getTargetAlphabet().where(table.getStringAlphabetColumnIndex(), updateQueryBuilder);
            final DbUpdateQuery upQuery = updateQueryBuilder
                    .where(table.getDynamicAcceptationColumnIndex(), updateMap.keyAt(i))
                    .put(table.getStringColumnIndex(), updateMap.valueAt(i))
                    .build();

            if (!db.update(upQuery)) {
                throw new AssertionError();
            }
        }
    }

    static <AlphabetId extends AlphabetIdInterface> boolean removeAlphabet(Database db, IntSetter<AlphabetId> alphabetIdManager, AlphabetId alphabet) {
        // There must be at least another alphabet in the same language to avoid leaving the language without alphabets
        if (alphabetsWithinLanguage(db, alphabetIdManager, alphabet).size() < 2) {
            return false;
        }

        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap(db, alphabetIdManager);
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
    public static <LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface> LanguageCreationResult<LanguageId, AlphabetId> addLanguage(Database db, IntSetter<LanguageId> languageIdSetter, IntSetter<AlphabetId> alphabetIntSetter, String code) {
        if (LangbookReadableDatabase.findLanguageByCode(db, languageIdSetter, code) != null) {
            return null;
        }

        final int lastConcept = getMaxConcept(db) + 1;
        final LanguageId language = languageIdSetter.getKeyFromInt(lastConcept + 1);
        final AlphabetId alphabet = alphabetIntSetter.getKeyFromInt(lastConcept + 2);
        LangbookDbInserter.insertLanguage(db, language, code, alphabet);
        insertAlphabet(db, alphabet, language);

        return new LanguageCreationResult<>(language, alphabet);
    }

    static <LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface> boolean removeLanguage(Database db, IntSetter<LanguageId> languageIdSetter, IntSetter<AlphabetId> alphabetIdSetter, LanguageId language) {
        // For now, if there is a bunch whose concept is only linked to acceptations of the language to be removed,
        // the removal is rejected, as there will not be any way to access that bunch any more in an AcceptationsDetailsActivity.
        // Only exception to the previous rule is the case where all acceptations within the bunch belongs to the language that is about to be removed.
        final ImmutableIntSet linkedBunches = findBunchConceptsLinkedToJustThisLanguage(db, languageIdSetter, language);
        if (!linkedBunches.isEmpty()) {
            if (linkedBunches.anyMatch(bunch -> {
                final ImmutableSet<LanguageId> languages = findIncludedAcceptationLanguages(db, languageIdSetter, bunch);
                return languages.size() > 1 || languages.size() == 1 && !languages.valueAt(0).equals(language);
            })) {
                return false;
            }
        }

        // For now, if there is a super type whose concept is only linked to acceptations of the language to be removed,
        // the removal is rejected, as there will not be any way to access that supertype any more in an AcceptationsDetailsActivity
        if (!findSuperTypesLinkedToJustThisLanguage(db, languageIdSetter, language).isEmpty()) {
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

        final ImmutableSet<AlphabetId> alphabets = findAlphabetsByLanguage(db, alphabetIdSetter, language);
        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap(db, alphabetIdSetter);
        final int size = conversionMap.size();
        for (int i = 0; i < size; i++) {
            final AlphabetId sourceAlphabet = conversionMap.valueAt(i);
            if (alphabets.contains(sourceAlphabet)) {
                final AlphabetId targetAlphabet = conversionMap.keyAt(i);
                if (!replaceConversion(db, languageIdSetter, new Conversion<>(sourceAlphabet, targetAlphabet, ImmutableHashMap.empty()))) {
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
    static <LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface> boolean addAlphabetCopyingFromOther(Database db, IntSetter<LanguageId> languageIdSetter, IntSetter<AlphabetId> alphabetIdSetter, AlphabetId alphabet, AlphabetId sourceAlphabet) {
        if (LangbookReadableDatabase.isAlphabetPresent(db, alphabet)) {
            return false;
        }

        final LanguageId language = getLanguageFromAlphabet(db, languageIdSetter, sourceAlphabet);
        if (language == null) {
            return false;
        }

        if (LangbookReadableDatabase.getConversionsMap(db, alphabetIdSetter).keySet().contains(sourceAlphabet)) {
            return false;
        }

        insertAlphabet(db, alphabet, language);

        final ImmutableIntPairMap correlations = LangbookReadableDatabase.findCorrelationsAndSymbolArrayForAlphabet(db, sourceAlphabet);
        final int correlationCount = correlations.size();
        for (int i = 0; i < correlationCount; i++) {
            LangbookDbInserter.insertCorrelationEntry(db, correlations.keyAt(i), alphabet, correlations.valueAt(i));
        }

        // Some kind of query for duplicating rows should be valuable. The following logic will be broken if a new column is added or removed for the table.
        //TODO: Change this logic
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery.Builder queryBuilder = new DbQuery.Builder(table);
        sourceAlphabet.where(table.getStringAlphabetColumnIndex(), queryBuilder);
        final DbQuery query = queryBuilder.select(
                table.getMainAcceptationColumnIndex(),
                table.getDynamicAcceptationColumnIndex(),
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

    static void addDefinition(DbImporter.Database db, int baseConcept, int concept, ImmutableIntSet complements) {
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
            if (foundSymbolArrayId != oldSymbolArrayId && oldSymbolArrayOnlyUsedHere && !deleteSymbolArray(db, oldSymbolArrayId)) {
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

    private static <AlphabetId extends AlphabetIdInterface> void applyConversion(DbImporter.Database db, Conversion<AlphabetId> conversion) {
        final AlphabetId sourceAlphabet = conversion.getSourceAlphabet();

        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery.Builder queryBuilder = new DbQuery.Builder(table);
        sourceAlphabet.where(table.getStringAlphabetColumnIndex(), queryBuilder);
        final DbQuery query = queryBuilder.select(
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

    private static <AlphabetId extends AlphabetIdInterface> void unapplyConversion(Deleter db, Conversion<AlphabetId> conversion) {
        final AlphabetId targetAlphabet = conversion.getTargetAlphabet();

        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery.Builder queryBuilder = new DbDeleteQuery.Builder(table);
        targetAlphabet.where(table.getStringAlphabetColumnIndex(), queryBuilder);
        final DbDeleteQuery query = queryBuilder.build();
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

    private static MutableIntKeyMap<MutableIntSet> getAcceptationsInBunchGroupedByAgent(Database db, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        DbQuery oldConceptQuery = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .select(table.getAgentColumnIndex(), table.getAcceptationColumnIndex());
        final MutableIntKeyMap<MutableIntSet> map = MutableIntKeyMap.empty();
        final SyncCacheIntKeyNonNullValueMap<MutableIntSet> syncCache = new SyncCacheIntKeyNonNullValueMap<>(map, k -> MutableIntArraySet.empty());
        try (DbResult dbResult = db.select(oldConceptQuery)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                syncCache.get(row.get(0).toInt()).add(row.get(1).toInt());
            }
        }

        return map;
    }

    private static void updateBunchAcceptationConcepts(Database db, int oldConcept, int newConcept) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final MutableIntKeyMap<MutableIntSet> oldBunchAcceptations = getAcceptationsInBunchGroupedByAgent(db, oldConcept);
        if (oldBunchAcceptations.isEmpty()) {
            return;
        }

        final MutableIntKeyMap<MutableIntSet> newBunchAcceptations = getAcceptationsInBunchGroupedByAgent(db, newConcept);
        final ImmutableIntSet involvedAgents = oldBunchAcceptations.keySet().toImmutable().addAll(newBunchAcceptations.keySet());

        final ImmutableIntKeyMap<IntSet> duplicated = involvedAgents.assign(agent -> {
            MutableIntSet rawNewAccSet = newBunchAcceptations.get(agent, null);
            final MutableIntSet newAccSet = (rawNewAccSet == null)? MutableIntArraySet.empty() : rawNewAccSet;
            MutableIntSet rawOldAccSet = oldBunchAcceptations.get(agent, null);
            final MutableIntSet oldAccSet = (rawOldAccSet == null)? MutableIntArraySet.empty() : rawOldAccSet;
            return newAccSet.filter(oldAccSet::contains);
        }).filterNot(IntSet::isEmpty);

        for (int agent : duplicated.keySet()) {
            for (int acc : duplicated.get(agent)) {
                if (!deleteBunchAcceptation(db, oldConcept, acc, agent)) {
                    throw new AssertionError();
                }
            }
        }

        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
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

    private static void updateBunchSetsInAgents(Database db, int oldBunchSetId, int newBunchSetId) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getTargetBunchSetColumnIndex(), oldBunchSetId)
                .put(table.getTargetBunchSetColumnIndex(), newBunchSetId)
                .build();
        db.update(query);

        query = new DbUpdateQuery.Builder(table)
                .where(table.getSourceBunchSetColumnIndex(), oldBunchSetId)
                .put(table.getSourceBunchSetColumnIndex(), newBunchSetId)
                .build();
        db.update(query);

        query = new DbUpdateQuery.Builder(table)
                .where(table.getDiffBunchSetColumnIndex(), oldBunchSetId)
                .put(table.getDiffBunchSetColumnIndex(), newBunchSetId)
                .build();
        db.update(query);
    }

    private static void updateBunchSetBunches(Database db, int oldBunch, int newBunch) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final IntKeyMap<MutableIntSet> oldBunchSets = readBunchSetsWhereBunchIsIncluded(db, oldBunch);
        if (oldBunchSets.isEmpty()) {
            return;
        }

        final MutableIntKeyMap<MutableIntSet> newBunchSets = readBunchSetsWhereBunchIsIncluded(db, newBunch);
        if (!newBunchSets.isEmpty()) {
            for (int index : oldBunchSets.indexes()) {
                final int oldSetId = oldBunchSets.keyAt(index);
                final MutableIntSet oldSet = oldBunchSets.valueAt(index);

                final boolean hasBoth = oldSet.contains(newBunch);
                if (hasBoth) {
                    final ImmutableIntSet desiredBunch = oldSet.toImmutable().remove(oldBunch);
                    final int reusableBunchIndex = newBunchSets.indexWhere(desiredBunch::equalSet);
                    if (reusableBunchIndex >= 0) {
                        updateBunchSetsInAgents(db, oldSetId, newBunchSets.keyAt(reusableBunchIndex));
                        deleteBunchSet(db, oldSetId);
                    }
                    else {
                        if (!deleteBunchSetBunch(db, oldSetId, oldBunch)) {
                            throw new AssertionError();
                        }
                    }
                }
                else {
                    final ImmutableIntSet set = oldSet.filterNot(v -> v == oldBunch).toImmutable().add(newBunch);
                    final int foundIndex = newBunchSets.indexWhere(set::equalSet);
                    if (foundIndex >= 0) {
                        if (!deleteBunchSet(db, oldSetId)) {
                            throw new AssertionError();
                        }

                        updateBunchSetsInAgents(db, oldSetId, newBunchSets.keyAt(foundIndex));
                    }
                }
            }
        }

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

    private static void updateRuledConceptsConcept(Database db, int oldConcept, int newConcept) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getConceptColumnIndex(), oldConcept)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        db.update(query);
    }

    private static void updateRuledConceptsRule(Database db, int ruledConcept, int newRule) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getIdColumnIndex(), ruledConcept)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        db.update(query);
    }

    private static IntPairMap getAcceptationsByConcept(Database db, int concept) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery readQuery = new DbQuery.Builder(table)
                .where(table.getConceptColumnIndex(), concept)
                .select(table.getIdColumnIndex(), table.getCorrelationArrayColumnIndex());

        final MutableIntPairMap map = MutableIntPairMap.empty();
        try (DbResult dbResult = db.select(readQuery)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                map.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return map;
    }

    static void updateAcceptationConcept(Database db, int acceptation, int newConcept) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getIdColumnIndex(), acceptation)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        db.update(query);
    }

    private static void updateAcceptationConcepts(Database db, int oldConcept, int newConcept) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbUpdateQuery query = new DbUpdateQuery.Builder(table)
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
        return mergeConcepts(db, conceptFromAcceptation(db, linkedAcceptation), oldConcept);
    }

    private static boolean mergeConcepts(Database db, int linkedConcept, int oldConcept) {
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

        final IntPairMap oldAcceptations = getAcceptationsByConcept(db, oldConcept);
        if (!oldAcceptations.isEmpty()) {
            final IntPairMap newAcceptations = getAcceptationsByConcept(db, linkedConcept);
            final IntPairMap repeatedAcceptations = oldAcceptations.filter(newAcceptations::contains);
            for (int oldAcc : repeatedAcceptations.keySet()) {
                if (!deleteAcceptation(db, oldAcc) || !deleteStringQueriesForDynamicAcceptation(db, oldAcc)) {
                    throw new AssertionError();
                }

                final int correlationArray = oldAcceptations.get(oldAcc);
                final int newAcc = newAcceptations.keyAt(newAcceptations.indexOf(correlationArray));

                final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
                DbQuery oldConceptQuery = new DbQuery.Builder(table)
                        .where(table.getAcceptationColumnIndex(), oldAcc)
                        .select(table.getAgentColumnIndex(), table.getBunchColumnIndex());
                final MutableIntKeyMap<MutableIntSet> map = MutableIntKeyMap.empty();
                final SyncCacheIntKeyNonNullValueMap<MutableIntSet> syncCache = new SyncCacheIntKeyNonNullValueMap<>(map, k -> MutableIntArraySet.empty());
                try (DbResult dbResult = db.select(oldConceptQuery)) {
                    while (dbResult.hasNext()) {
                        final List<DbValue> row = dbResult.next();
                        syncCache.get(row.get(0).toInt()).add(row.get(1).toInt());
                    }
                }

                for (int agent : map.keySet()) {
                    for (int bunch : map.get(agent)) {
                        insertBunchAcceptation(db, bunch, newAcc, agent);
                    }
                }
                deleteBunchAcceptationsByAcceptation(db, oldAcc);
            }

            updateAcceptationConcepts(db, oldConcept, linkedConcept);
        }

        final MutableIntPairMap oldRuledConcepts = findRuledConceptsByConceptInvertedMap(db, oldConcept);
        if (!oldRuledConcepts.isEmpty()) {
            final MutableIntPairMap newRuledConcepts = findRuledConceptsByConceptInvertedMap(db, linkedConcept);
            final ImmutableIntSet newRuledConceptsRules = newRuledConcepts.keySet().toImmutable();
            for (int oldRuledConceptIndex : oldRuledConcepts.indexes()) {
                final int rule = oldRuledConcepts.keyAt(oldRuledConceptIndex);
                final int oldRuledConcept = oldRuledConcepts.valueAt(oldRuledConceptIndex);
                final boolean isCommonRule = newRuledConceptsRules.contains(rule);
                if (isCommonRule) {
                    final int newRuledConcept = newRuledConcepts.get(rule);
                    if (!deleteRuledConcept(db, oldRuledConcept)) {
                        throw new AssertionError();
                    }

                    mergeConcepts(db, newRuledConcept, oldRuledConcept);
                }
                else {
                    updateRuledConceptsConcept(db, oldConcept, linkedConcept);
                }
            }
        }

        final ImmutableIntPairMap oldRuledConceptsMap = findRuledConceptsByRuleInvertedMap(db, oldConcept);
        final int oldRuledConceptsMapSize = oldRuledConceptsMap.size();
        if (oldRuledConceptsMapSize > 0) {
            final ImmutableIntPairMap newRuledConceptsMap = findRuledConceptsByRuleInvertedMap(db, linkedConcept);
            final ImmutableIntSet newRuledConceptsMapKeys = newRuledConceptsMap.keySet();
            for (int i = 0; i < oldRuledConceptsMapSize; i++) {
                final int baseConcept = oldRuledConceptsMap.keyAt(i);
                if (newRuledConceptsMapKeys.contains(baseConcept)) {
                    mergeConcepts(db, newRuledConceptsMap.get(baseConcept), oldRuledConceptsMap.valueAt(i));
                }
                else {
                    updateRuledConceptsRule(db, oldRuledConceptsMap.valueAt(i), linkedConcept);
                }
            }
        }

        return true;
    }

    /**
     * Extract the correlation array assigned to the given linkedAcceptation and
     * creates a new acceptation with the same correlation array but with the given concept.
     * @param db Database where the new acceptation has to be inserted.
     * @param linkedAcceptation Acceptation from where the correlation array reference has to be copied.
     * @param concept Concept to be applied to the new acceptation created.
     */
    static <AlphabetId extends AlphabetIdInterface> void duplicateAcceptationWithThisConcept(Database db, IntSetter<AlphabetId> alphabetIdManager, int linkedAcceptation, int concept) {
        if (concept == 0) {
            throw new AssertionError();
        }

        final int correlationArray = correlationArrayFromAcceptation(db, linkedAcceptation);
        addAcceptation(db, alphabetIdManager, concept, correlationArray);
    }

    /**
     * Add a new alphabet and a new conversion at once, being the resulting alphabet the target of the given conversion.
     * @param db Database where the alphabet and conversion will be included.
     * @param conversion Conversion to be evaluated and stored if no conflicts are found.
     * @return Whether the action was completed successfully, and so the database state content has changed.
     */
    static <LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface> boolean addAlphabetAsConversionTarget(Database db, IntSetter<LanguageId> languageIdSetter, Conversion<AlphabetId> conversion) {
        final LanguageId language = getLanguageFromAlphabet(db, languageIdSetter, conversion.getSourceAlphabet());
        if (language == null) {
            return false;
        }

        if (isAlphabetPresent(db, conversion.getTargetAlphabet())) {
            return false;
        }

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
    static <LanguageId, AlphabetId extends AlphabetIdInterface> boolean replaceConversion(Database db, IntSetter<LanguageId> languageIdSetter, Conversion<AlphabetId> conversion) {
        final AlphabetId sourceAlphabet = conversion.getSourceAlphabet();
        final AlphabetId targetAlphabet = conversion.getTargetAlphabet();
        final LanguageId languageObj = getLanguageFromAlphabet(db, languageIdSetter, sourceAlphabet);
        if (languageObj == null) {
            return false;
        }

        final LanguageId languageObj2 = getLanguageFromAlphabet(db, languageIdSetter, targetAlphabet);
        if (languageObj2 == null || !languageObj2.equals(languageObj)) {
            return false;
        }

        if (conversion.getMap().isEmpty()) {
            final Conversion<AlphabetId> oldConversion = getConversion(db, conversion.getAlphabets());
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
            final Conversion<AlphabetId> oldConversion = updateJustConversion(db, conversion);
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
    private static <AlphabetId extends AlphabetIdInterface> Integer obtainCorrelation(DbImporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, IntValueMap<AlphabetId> correlation) {
        final Integer foundId = findCorrelation(db, alphabetIntSetter, correlation);
        if (foundId != null) {
            return foundId;
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
    private static <AlphabetId extends AlphabetIdInterface> Integer obtainCorrelation(DbImporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, Correlation<AlphabetId> correlation) {
        if (correlation.anyMatch(str -> findSymbolArray(db, str) == null)) {
            final int newCorrelationId = getMaxCorrelationId(db) + 1;
            LangbookDbInserter.insertCorrelation(db, newCorrelationId, correlation.mapToInt(str -> obtainSymbolArray(db, str)));
            return newCorrelationId;
        }

        return obtainCorrelation(db, alphabetIntSetter, correlation.mapToInt(str -> obtainSymbolArray(db, str)));
    }

    /**
     * Add a new correlation array to the database.
     *
     * Correlations composing a correlation array:
     * <ul>
     *   <li>must contain alphabets from the same language in relation with other correlations.</li>
     *   <li>must not include alphabets that are target of a conversion.</li>
     * </ul>
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
    private static <AlphabetId extends AlphabetIdInterface> Integer obtainCorrelationArray(DbImporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, IntList correlations) {
        final Integer foundId = findCorrelationArray(db, correlations);
        if (foundId != null) {
            return foundId;
        }

        if (correlations.isEmpty()) {
            return null;
        }

        final List<ImmutableCorrelation<AlphabetId>> array = correlations.map(id -> getCorrelationWithText(db, alphabetIntSetter, id));
        if (array.anyMatch(ImmutableCorrelation::isEmpty)) {
            return null;
        }

        final ImmutableSet<AlphabetId> alphabets = array.map(ImmutableCorrelation::keySet).reduce(ImmutableSet::addAll);

        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap(db, alphabetIntSetter);
        if (alphabets.anyMatch(conversionMap.keySet()::contains)) {
            return null;
        }

        for (AlphabetId alphabet : alphabets) {
            final int index = conversionMap.indexOf(alphabet);
            if (index >= 0) {
                final AlphabetId targetAlphabet = conversionMap.keyAt(index);
                final Conversion<AlphabetId> conversion = getConversion(db, new ImmutablePair<>(alphabet, targetAlphabet));
                final String sourceText = array.map(c -> c.get(alphabet, "")).reduce((a, b) -> a + b);
                final String targetText = conversion.convert(sourceText);
                if (targetText == null) {
                    return null;
                }
            }
        }

        final int maxArrayId = getMaxCorrelationArrayId(db);
        final int newArrayId = maxArrayId + ((maxArrayId + 1 != NULL_CORRELATION_ARRAY_ID)? 1 : 2);
        LangbookDbInserter.insertCorrelationArray(db, newArrayId, correlations);
        return newArrayId;
    }

    /**
     * Shortcut for {@link #obtainCorrelationArray(DbImporter.Database, IntSetter, IntList)}.
     */
    private static <AlphabetId extends AlphabetIdInterface> Integer obtainSimpleCorrelationArray(DbImporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, int correlationId) {
        return obtainCorrelationArray(db, alphabetIntSetter, new ImmutableIntList.Builder().append(correlationId).build());
    }
}
