package sword.langbook3.android.db;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.List;
import sword.collections.Map;
import sword.collections.MutableHashMap;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableMap;
import sword.collections.MutableSet;
import sword.collections.Set;
import sword.database.Database;
import sword.database.DbDeleteQuery;
import sword.database.DbInsertQuery;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbStringValue;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
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
import static sword.langbook3.android.db.LangbookDbSchema.EMPTY_CORRELATION_ARRAY_ID;
import static sword.langbook3.android.db.LangbookDbSchema.MAX_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.MIN_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;
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

public class LangbookDatabaseManager<ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, SymbolArrayId extends SymbolArrayIdInterface, CorrelationId extends CorrelationIdInterface, CorrelationArrayId extends CorrelationArrayIdInterface, AcceptationId extends AcceptationIdInterface, BunchId extends BunchIdInterface<ConceptId>, RuleId extends RuleIdInterface<ConceptId>> extends LangbookDatabaseChecker<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, RuleId> implements LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, RuleId> {

    public LangbookDatabaseManager(Database db, ConceptSetter<ConceptId> conceptIdManager, ConceptualizableSetter<ConceptId, LanguageId> languageIdManager, ConceptualizableSetter<ConceptId, AlphabetId> alphabetIdManager, IntSetter<SymbolArrayId> symbolArrayIdManager, IntSetter<CorrelationId> correlationIdSetter, IntSetter<CorrelationArrayId> correlationArrayIdSetter, IntSetter<AcceptationId> acceptationIdSetter, ConceptualizableSetter<ConceptId, BunchId> bunchIdSetter, ConceptualizableSetter<ConceptId, RuleId> ruleIdSetter) {
        super(db, conceptIdManager, languageIdManager, alphabetIdManager, symbolArrayIdManager, correlationIdSetter, correlationArrayIdSetter, acceptationIdSetter, bunchIdSetter, ruleIdSetter);
    }

    private boolean applyMatchersAddersAndConversions(
            MutableCorrelation<AlphabetId> correlation,
            AgentDetails<AlphabetId, BunchId, RuleId> details, ImmutableMap<AlphabetId, AlphabetId> conversionMap,
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

    private SymbolArrayId obtainSymbolArray(String str) {
        SymbolArrayId id = insertSymbolArray(_db, _symbolArrayIdSetter, str);
        if (id != null) {
            return id;
        }

        id = findSymbolArray(str);
        if (id == null) {
            throw new AssertionError("Unable to insert, and not present");
        }

        return id;
    }

    private ConceptId insertRuledConcept(RuleId rule, ConceptId concept) {
        final ConceptId ruledConcept = getNextAvailableConceptId();
        LangbookDbInserter.insertRuledConcept(_db, ruledConcept, rule, concept);
        return ruledConcept;
    }

    private ConceptId obtainRuledConcept(RuleId rule, ConceptId concept) {
        final ConceptId id = findRuledConcept(rule, concept);
        return (id != null)? id : insertRuledConcept(rule, concept);
    }

    private ImmutableSet<AcceptationId> findMatchingAcceptations(
            ImmutableSet<BunchId> sourceBunches, ImmutableSet<BunchId> diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> endMatcher) {

        final ImmutableSet.Builder<AcceptationId> diffAccBuilder = new ImmutableHashSet.Builder<>();
        for (BunchId bunch : diffBunches) {
            final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
            final DbQuery query = new DbQueryBuilder(table)
                    .where(table.getBunchColumnIndex(), bunch)
                    .select(table.getAcceptationColumnIndex());
            try (DbResult result = _db.select(query)) {
                while (result.hasNext()) {
                    diffAccBuilder.add(_acceptationIdSetter.getKeyFromDbValue(result.next().get(0)));
                }
            }
        }
        final ImmutableSet<AcceptationId> diffAcceptations = diffAccBuilder.build();

        ImmutableSet<AcceptationId> matchingAcceptations = null;
        if (!sourceBunches.isEmpty()) {
            final ImmutableSet.Builder<AcceptationId> builder = new ImmutableHashSet.Builder<>();
            for (BunchId bunch : sourceBunches) {
                final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
                final DbQuery query = new DbQueryBuilder(table)
                        .where(table.getBunchColumnIndex(), bunch)
                        .select(table.getAcceptationColumnIndex());
                try (DbResult result = _db.select(query)) {
                    while (result.hasNext()) {
                        final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(result.next().get(0));
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
            final DbQuery matchQuery = new DbQueryBuilder(strTable)
                    .where(strTable.getStringAlphabetColumnIndex(), alphabet)
                    .where(strTable.getStringColumnIndex(), new DbQuery.Restriction(
                            new DbStringValue(queryValue), restrictionType))
                    .select(strTable.getDynamicAcceptationColumnIndex());
            final ImmutableSet.Builder<AcceptationId> builder = new ImmutableHashSet.Builder<>();
            try (DbResult result = _db.select(matchQuery)) {
                while (result.hasNext()) {
                    final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(result.next().get(0));
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

    /**
     * Add a new correlation to the database.
     *
     * This method will fail if the keys within the correlation map does not match valid alphabets,
     * alphabets are not from the same language, or any of the symbol array reference is wrong.
     *
     * @param correlation IntPairMap whose keys are alphabets and values are symbol arrays identifiers.
     * @return An identifier for the new correlation included, or null in case of error.
     */
    private CorrelationId obtainCorrelation(Map<AlphabetId, SymbolArrayId> correlation) {
        final CorrelationId foundId = findCorrelation(correlation);
        if (foundId != null) {
            return foundId;
        }

        if (correlation.anyMatch(strId -> !isSymbolArrayPresent(strId))) {
            return null;
        }

        final int rawMaxCorrelationId = getMaxCorrelationId();
        final CorrelationId newCorrelationId = _correlationIdSetter.getKeyFromInt(rawMaxCorrelationId + 1);
        LangbookDbInserter.insertCorrelation(_db, newCorrelationId, correlation);
        return newCorrelationId;
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
     * @param correlations list of correlations to be entered.
     * @return An identifier for the correlation array, or null if it cannot be inserted into the database.
     */
    private CorrelationArrayId obtainCorrelationArray(List<CorrelationId> correlations) {
        final CorrelationArrayId foundId = findCorrelationArray(correlations);
        if (foundId != null) {
            return foundId;
        }

        if (correlations.isEmpty()) {
            return null;
        }

        final List<ImmutableCorrelation<AlphabetId>> array = correlations.map(this::getCorrelationWithText);
        if (array.anyMatch(ImmutableCorrelation::isEmpty)) {
            return null;
        }

        final ImmutableSet<AlphabetId> alphabets = array.map(ImmutableCorrelation::keySet).reduce(ImmutableSet::addAll);

        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap();
        if (alphabets.anyMatch(conversionMap.keySet()::contains)) {
            return null;
        }

        for (AlphabetId alphabet : alphabets) {
            final int index = conversionMap.indexOf(alphabet);
            if (index >= 0) {
                final AlphabetId targetAlphabet = conversionMap.keyAt(index);
                final Conversion<AlphabetId> conversion = getConversion(new ImmutablePair<>(alphabet, targetAlphabet));
                final String sourceText = array.map(c -> c.get(alphabet, "")).reduce((a, b) -> a + b);
                final String targetText = conversion.convert(sourceText);
                if (targetText == null) {
                    return null;
                }
            }
        }

        final int maxArrayId = getMaxCorrelationArrayId();
        final int newRawArrayId = maxArrayId + ((maxArrayId + 1 != EMPTY_CORRELATION_ARRAY_ID)? 1 : 2);
        final CorrelationArrayId newArrayId = _correlationArrayIdSetter.getKeyFromInt(newRawArrayId);
        LangbookDbInserter.insertCorrelationArray(_db, newArrayId, correlations);
        return newArrayId;
    }

    /**
     * Shortcut for {@link #obtainCorrelationArray(List)}.
     */
    private CorrelationArrayId obtainSimpleCorrelationArray(CorrelationId correlationId) {
        return obtainCorrelationArray(new ImmutableList.Builder<CorrelationId>().append(correlationId).build());
    }

    private void runAgent(int agentId, AgentDetails<AlphabetId, BunchId, RuleId> details) {
        final ImmutableSet<AcceptationId> matchingAcceptations = findMatchingAcceptations(details.sourceBunches, details.diffBunches, details.startMatcher, details.endMatcher);
        final ImmutableSet<AcceptationId> processedAcceptations;
        if (!details.modifyCorrelations()) {
            processedAcceptations = matchingAcceptations;
        }
        else {
            final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap();
            final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions = new SyncCacheMap<>(this::getConversion);

            final SyncCacheMap<AlphabetId, AlphabetId> mainAlphabets = new SyncCacheMap<>(this::readMainAlphabetFromAlphabet);
            final ImmutableSet.Builder<AcceptationId> processedAccBuilder = new ImmutableHashSet.Builder<>();

            for (AcceptationId acc : matchingAcceptations) {
                final ImmutablePair<ImmutableCorrelation<AlphabetId>, AcceptationId> textsAndMain = readAcceptationTextsAndMain(acc);
                final MutableCorrelation<AlphabetId> correlation = textsAndMain.left.mutate();

                final boolean validConversion = applyMatchersAddersAndConversions(correlation, details, conversionMap, conversions);
                if (validConversion) {
                    final ImmutableSet<AlphabetId> conversionTargets = conversionMap.keySet();
                    final ImmutableMap.Builder<AlphabetId, SymbolArrayId> corrBuilder = new ImmutableHashMap.Builder<>();
                    for (ImmutableMap.Entry<AlphabetId, String> entry : correlation.entries()) {
                        if (!conversionTargets.contains(entry.key())) {
                            corrBuilder.put(entry.key(), obtainSymbolArray(entry.value()));
                        }
                    }

                    final CorrelationId correlationId = obtainCorrelation(corrBuilder.build());
                    final CorrelationArrayId correlationArrayId = obtainSimpleCorrelationArray(correlationId);

                    final ConceptId baseConcept = conceptFromAcceptation(acc);
                    final ConceptId ruledConcept = obtainRuledConcept(details.rule, baseConcept);
                    final AcceptationId newAcc = insertAcceptation(_db, _acceptationIdSetter, ruledConcept, correlationArrayId);
                    insertRuledAcceptation(_db, newAcc, agentId, acc);

                    for (Map.Entry<AlphabetId, String> entry : correlation.entries()) {
                        final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                        insertStringQuery(_db, entry.value(), mainText, textsAndMain.right, newAcc, entry.key());
                    }
                    processedAccBuilder.add(newAcc);
                }
            }
            processedAcceptations = processedAccBuilder.build();
        }

        for (BunchId targetBunch : details.targetBunches) {
            for (AcceptationId acc : processedAcceptations) {
                insertBunchAcceptation(_db, targetBunch, acc, agentId);
            }
        }
    }

    private int insertBunchSet(Set<BunchId> bunchSet) {
        if (bunchSet.isEmpty()) {
            return 0;
        }

        final int setId = getMaxBunchSetId() + 1;
        LangbookDbInserter.insertBunchSet(_db, setId, bunchSet);
        return setId;
    }

    private int obtainBunchSet(Set<BunchId> bunchSet) {
        final Integer id = findBunchSet(bunchSet);
        return (id != null)? id : insertBunchSet(bunchSet);
    }

    private void updateAcceptationConcept(AcceptationId acceptation, ConceptIdInterface newConcept) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getIdColumnIndex(), acceptation)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        _db.update(query);
    }

    /**
     * Run again the specified agent.
     * @param sourceAgentChangedText If a ruled is applied, forces the recheck of the resulting texts.
     *                               This should be true at least if one source agent has changed its adders,
     *                               resulting in a different source acceptation to be ruled here.
     * @return A bunch set containing all target bunches that changed, or empty if there is no change.
     */
    private ImmutableSet<BunchId> rerunAgent(int agentId, MutableSet<AcceptationId> deletedDynamicAcceptations, boolean sourceAgentChangedText) {
        final AgentDetails<AlphabetId, BunchId, RuleId> agentDetails = getAgentDetails(agentId);
        final ImmutableSet<AcceptationId> matchingAcceptations = findMatchingAcceptations(
                agentDetails.sourceBunches, agentDetails.diffBunches,
                agentDetails.startMatcher, agentDetails.endMatcher);

        boolean targetChanged = false;
        final boolean ruleApplied = agentDetails.modifyCorrelations();
        final ImmutableMap<AcceptationId, AcceptationId> processedAcceptationsMap;
        if (!ruleApplied) {
            final ImmutableMap<BunchId, ImmutableSet<AcceptationId>> alreadyProcessedAcceptations;
            // TODO: Ruled concept should also be removed if they are not used by other agent
            if (deleteRuledAcceptationByAgent(_db, agentId)) {
                deleteBunchAcceptationsByAgent(_db, agentId);
                targetChanged = true;
                alreadyProcessedAcceptations = ImmutableHashMap.empty();
            }
            else {
                alreadyProcessedAcceptations = agentDetails.targetBunches.assign(targetBunch -> getAcceptationsInBunchByBunchAndAgent(targetBunch, agentId));
            }

            for (BunchId targetBunch : alreadyProcessedAcceptations.keySet()) {
                for (AcceptationId acc : alreadyProcessedAcceptations.get(targetBunch)) {
                    if (!matchingAcceptations.contains(acc)) {
                        if (!deleteBunchAcceptation(_db, targetBunch, acc, agentId)) {
                            throw new AssertionError();
                        }
                        targetChanged = true;
                    }
                }
            }

            final ImmutableSet<AcceptationId> allAlreadyProcessedAcceptations = alreadyProcessedAcceptations
                    .reduce((a, b) -> a.filter(b::contains), ImmutableHashSet.empty());
            final ImmutableSet<AcceptationId> processedAcceptations = matchingAcceptations.filterNot(allAlreadyProcessedAcceptations::contains);
            processedAcceptationsMap = processedAcceptations.assign(key -> key);
        }
        else {
            final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap();
            final ImmutableSet<AlphabetId> conversionTargets = conversionMap.keySet();
            final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions = new SyncCacheMap<>(this::getConversion);
            final SyncCacheMap<AlphabetId, AlphabetId> mainAlphabets = new SyncCacheMap<>(this::readMainAlphabetFromAlphabet);

            // This is assuming that matcher, adder and flags did not change from last run,
            // only its source and diff bunches and its contents
            final ImmutableMap<AcceptationId, AcceptationId> oldProcessedMap = getAgentProcessedMap(agentId);
            final ImmutableSet<AcceptationId> toBeProcessed;
            final ImmutableSet<AcceptationId> alreadyProcessedAcceptations = oldProcessedMap.keySet();
            final boolean noRuleBefore = alreadyProcessedAcceptations.isEmpty() && isBunchAcceptationPresentByAgent(agentId);
            if (noRuleBefore && !deleteBunchAcceptationsByAgent(_db, agentId)) {
                throw new AssertionError();
            }

            final AcceptationId sampleStaticAcc = matchingAcceptations.findFirst(alreadyProcessedAcceptations::contains, null);
            final boolean hasSameRule;
            final boolean canReuseOldRuledConcept;
            final boolean mustChangeResultingText;
            if (sampleStaticAcc != null) {
                final AcceptationId sampleDynAcc = oldProcessedMap.get(sampleStaticAcc);
                final ConceptId sampleRuledConcept = conceptFromAcceptation(sampleDynAcc);
                final RuleId sampleRule = getRuleByRuledConcept(sampleRuledConcept);
                hasSameRule = equal(sampleRule, agentDetails.rule);
                canReuseOldRuledConcept = hasSameRule || findAgentsByRule(sampleRule).isEmpty();

                if (sourceAgentChangedText) {
                    mustChangeResultingText = true;
                }
                else {
                    final MutableCorrelation<AlphabetId> accText = getAcceptationTexts(sampleStaticAcc).mutate();
                    final ImmutableCorrelation<AlphabetId> sampleDynAccText = getAcceptationTexts(sampleDynAcc);
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
                final ImmutableMap<ConceptId, ConceptId> ruledConceptsInvertedMap = findRuledConceptsByRuleInvertedMap(agentDetails.rule);
                for (AcceptationId staticAcc : matchingAcceptations.filter(alreadyProcessedAcceptations::contains)) {
                    final AcceptationId dynAcc = oldProcessedMap.get(staticAcc);
                    final ConceptId baseConcept = conceptFromAcceptation(staticAcc);
                    final ConceptId foundRuledConcept = ruledConceptsInvertedMap.get(baseConcept, null);

                    if (canReuseOldRuledConcept) {
                        final ConceptId ruledConcept = conceptFromAcceptation(dynAcc);
                        if (foundRuledConcept != null) {
                            updateAcceptationConcept(dynAcc, foundRuledConcept);
                            if (!deleteRuledConcept(_db, ruledConcept)) {
                                throw new AssertionError();
                            }
                        }
                        else {
                            final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
                            final DbUpdateQuery updateQuery = new DbUpdateQueryBuilder(table)
                                    .put(table.getRuleColumnIndex(), agentDetails.rule)
                                    .where(table.getIdColumnIndex(), ruledConcept)
                                    .build();
                            if (!_db.update(updateQuery)) {
                                throw new AssertionError();
                            }
                        }
                    }
                    else {
                        final ConceptId newRuledConcept = (foundRuledConcept != null)? foundRuledConcept : insertRuledConcept(agentDetails.rule, baseConcept);
                        updateAcceptationConcept(dynAcc, newRuledConcept);
                    }
                }
            }

            if (mustChangeResultingText) {
                for (AcceptationId staticAcc : matchingAcceptations.filter(alreadyProcessedAcceptations::contains)) {
                    final MutableCorrelation<AlphabetId> correlation = getAcceptationTexts(staticAcc).mutate();
                    final boolean validConversion = applyMatchersAddersAndConversions(correlation, agentDetails,
                            conversionMap, conversions);
                    if (validConversion) {
                        final ImmutableMap.Builder<AlphabetId, SymbolArrayId> corrBuilder = new ImmutableHashMap.Builder<>();
                        for (ImmutableMap.Entry<AlphabetId, String> entry : correlation.entries()) {
                            if (!conversionTargets.contains(entry.key())) {
                                corrBuilder.put(entry.key(), obtainSymbolArray(entry.value()));
                            }
                        }

                        final CorrelationId correlationId = obtainCorrelation(corrBuilder.build());
                        final CorrelationArrayId correlationArrayId = obtainSimpleCorrelationArray(correlationId);
                        final AcceptationId dynAcc = oldProcessedMap.get(staticAcc);

                        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
                        DbUpdateQuery updateQuery = new DbUpdateQueryBuilder(table)
                                .put(table.getCorrelationArrayColumnIndex(), correlationArrayId)
                                .where(table.getIdColumnIndex(), dynAcc)
                                .build();
                        if (!_db.update(updateQuery)) {
                            throw new AssertionError();
                        }
                        targetChanged = true;

                        for (Map.Entry<AlphabetId, String> entry : correlation.entries()) {
                            final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
                            final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                            updateQuery = new DbUpdateQueryBuilder(strings)
                                    .put(strings.getStringColumnIndex(), entry.value())
                                    .put(strings.getMainStringColumnIndex(), mainText)
                                    .where(strings.getDynamicAcceptationColumnIndex(), dynAcc)
                                    .where(strings.getStringAlphabetColumnIndex(), entry.key())
                                    .build();
                            if (!_db.update(updateQuery)) {
                                throw new AssertionError();
                            }
                        }
                    }
                }
            }

            toBeProcessed = matchingAcceptations.filterNot(alreadyProcessedAcceptations::contains);

            final MutableMap<AcceptationId, AcceptationId> resultProcessedMap = oldProcessedMap.mutate();
            for (Map.Entry<AcceptationId, AcceptationId> accPair : oldProcessedMap.entries()) {
                if (!matchingAcceptations.contains(accPair.key())) {
                    final AcceptationId acc = accPair.value();
                    deleteKnowledge(_db, acc);
                    for (BunchId targetBunch : agentDetails.targetBunches) {
                        deleteBunchAcceptation(_db, targetBunch, acc, agentId);
                    }
                    deleteStringQueriesForDynamicAcceptation(_db, acc);
                    deleteSpansByDynamicAcceptation(_db, acc);
                    if (!deleteAcceptation(_db, acc) | !deleteRuledAcceptation(_db, acc)) {
                        throw new AssertionError();
                    }

                    if (deletedDynamicAcceptations != null) {
                        deletedDynamicAcceptations.add(acc);
                    }

                    targetChanged = true;
                    resultProcessedMap.remove(accPair.key());
                }
            }

            final ImmutableMap.Builder<AcceptationId, AcceptationId> processedAccMapBuilder = new ImmutableHashMap.Builder<>();
            for (AcceptationId acc : matchingAcceptations) {
                if (toBeProcessed.contains(acc)) {
                    final ImmutablePair<ImmutableCorrelation<AlphabetId>, AcceptationId> textsAndMain = readAcceptationTextsAndMain(acc);
                    final MutableCorrelation<AlphabetId> correlation = textsAndMain.left.mutate();

                    final boolean validConversion = applyMatchersAddersAndConversions(correlation, agentDetails, conversionMap, conversions);
                    if (validConversion) {
                        final ImmutableMap.Builder<AlphabetId, SymbolArrayId> corrBuilder = new ImmutableHashMap.Builder<>();
                        for (ImmutableMap.Entry<AlphabetId, String> entry : correlation.entries()) {
                            if (!conversionTargets.contains(entry.key())) {
                                corrBuilder.put(entry.key(), obtainSymbolArray(entry.value()));
                            }
                        }

                        final CorrelationId correlationId = obtainCorrelation(corrBuilder.build());
                        final CorrelationArrayId correlationArrayId = obtainSimpleCorrelationArray(correlationId);
                        final ConceptId baseConcept = conceptFromAcceptation(acc);
                        final ConceptId ruledConcept = obtainRuledConcept(agentDetails.rule, baseConcept);
                        final AcceptationId newAcc = insertAcceptation(_db, _acceptationIdSetter, ruledConcept, correlationArrayId);
                        insertRuledAcceptation(_db, newAcc, agentId, acc);

                        for (Map.Entry<AlphabetId, String> entry : correlation.entries()) {
                            final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                            insertStringQuery(_db, entry.value(), mainText, textsAndMain.right, newAcc, entry.key());
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

        for (BunchId targetBunch : agentDetails.targetBunches) {
            final ImmutableSet<AcceptationId> alreadyIncludedAcceptations = getAcceptationsInBunchByBunchAndAgent(targetBunch, agentId);
            for (AcceptationId acc : processedAcceptationsMap.filterNot(alreadyIncludedAcceptations::contains)) {
                insertBunchAcceptation(_db, targetBunch, acc, agentId);
                targetChanged = true;
            }
        }

        return targetChanged? agentDetails.targetBunches : ImmutableHashSet.empty();
    }

    @Override
    public Integer addAgent(
            ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches, ImmutableSet<BunchId> diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> startAdder,
            ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> endAdder, RuleId rule) {
        if (sourceBunches.anyMatch(diffBunches::contains)) {
            return null;
        }

        final int targetBunchSetId = obtainBunchSet(targetBunches);
        final int sourceBunchSetId = obtainBunchSet(sourceBunches);
        final int diffBunchSetId = obtainBunchSet(diffBunches);

        final SyncCacheMap<ImmutableCorrelation<AlphabetId>, CorrelationId> cachedCorrelationIds =
                new SyncCacheMap<>(corr -> obtainCorrelation(corr.map(this::obtainSymbolArray)));
        final CorrelationId startMatcherId = cachedCorrelationIds.get(startMatcher);
        final CorrelationId startAdderId = cachedCorrelationIds.get(startAdder);
        final CorrelationId endMatcherId = cachedCorrelationIds.get(endMatcher);
        final CorrelationId endAdderId = cachedCorrelationIds.get(endAdder);

        final boolean ruleExpected = startMatcherId != startAdderId || endMatcherId != endAdderId;
        final boolean rulePresent = rule != null;
        if (ruleExpected != rulePresent) {
            return null;
        }

        final AgentRegister<CorrelationId, RuleId> register;
        try {
            register = new AgentRegister<>(targetBunchSetId, sourceBunchSetId,
                    diffBunchSetId, startMatcherId, startAdderId, endMatcherId, endAdderId, rule);
        }
        catch (IllegalArgumentException e) {
            return null;
        }

        final Integer agentId = LangbookDbInserter.insertAgent(_db, register);
        if (agentId != null) {
            final AgentDetails<AlphabetId, BunchId, RuleId> details = new AgentDetails<>(targetBunches, sourceBunches, diffBunches,
                    startMatcher, startAdder, endMatcher, endAdder, rule);
            runAgent(agentId, details);
        }

        ImmutableSet<BunchId> updatedBunches = targetBunches.isEmpty()? new ImmutableHashSet.Builder<BunchId>().build() : targetBunches;
        while (!updatedBunches.isEmpty()) {
            final ImmutableSet.Builder<BunchId> builder = new ImmutableHashSet.Builder<>();
            for (BunchId bunch : updatedBunches) {
                for (IntKeyMap.Entry<ImmutableSet<BunchId>> entry : findAffectedAgentsByItsSourceWithTarget(bunch).entries()) {
                    rerunAgent(entry.key(), null, false);
                    for (BunchId b : entry.value()) {
                        builder.add(b);
                    }
                }
            }
            updatedBunches = builder.build();
        }

        return agentId;
    }

    /**
     * Add a new correlation to the database.
     *
     * This method will fail if the keys within the correlation map does not match valid alphabets,
     * or alphabets are not from the same language.
     *
     * @param correlation IntKeyMap whose keys are alphabets and values are symbol arrays to be included as well.
     * @return An identifier for the new correlation included, or null in case of error.
     */
    private CorrelationId obtainCorrelation(Correlation<AlphabetId> correlation) {
        if (correlation.anyMatch(str -> findSymbolArray(str) == null)) {
            final int maxCorrelationId = getMaxCorrelationId() + 1;
            final CorrelationId newCorrelationId = _correlationIdSetter.getKeyFromInt(maxCorrelationId + 1);
            LangbookDbInserter.insertCorrelation(_db, newCorrelationId, correlation.map(this::obtainSymbolArray));
            return newCorrelationId;
        }

        return obtainCorrelation(correlation.map(this::obtainSymbolArray));
    }

    private ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableSet<BunchId>>> getAgentExecutionOrder() {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getTargetBunchSetColumnIndex(), table.getSourceBunchSetColumnIndex(), table.getDiffBunchSetColumnIndex());

        final MutableIntKeyMap<ImmutableSet<BunchId>> agentDependencies = MutableIntKeyMap.empty();
        final MutableIntKeyMap<ImmutableSet<BunchId>> agentDependenciesWithNull = MutableIntKeyMap.empty();

        final MutableIntSet agentsWithoutSource = MutableIntArraySet.empty();
        final MutableIntKeyMap<ImmutableSet<BunchId>> agentTargets = MutableIntKeyMap.empty();
        final SyncCacheIntKeyNonNullValueMap<ImmutableSet<BunchId>> bunchSets = new SyncCacheIntKeyNonNullValueMap<>(this::getBunchSet);
        try (DbResult dbResult = _db.select(query)) {
            final ImmutableSet<BunchId> justNullDependency = ImmutableHashSet.<BunchId>empty().add(null);
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int id = row.get(0).toInt();

                agentTargets.put(id, bunchSets.get(row.get(1).toInt()));

                final ImmutableSet<BunchId> sourceBunches = bunchSets.get(row.get(2).toInt());
                final ImmutableSet<BunchId> diffBunches = bunchSets.get(row.get(3).toInt());
                agentDependencies.put(id, sourceBunches.addAll(diffBunches));

                final ImmutableSet<BunchId> sourceBunchesWithNull = sourceBunches.isEmpty()? justNullDependency : sourceBunches;
                agentDependenciesWithNull.put(id, sourceBunchesWithNull.addAll(diffBunches));

                if (sourceBunches.isEmpty()) {
                    agentsWithoutSource.add(id);
                }
            }
        }

        final int agentCount = agentDependencies.size();
        final int[] agentList = new int[agentCount];
        for (int i = 0; i < agentCount; i++) {
            final int agentId = agentDependencies.keyAt(i);
            final ImmutableSet<BunchId> targets = agentTargets.get(agentId);
            boolean inserted = false;
            for (int j = 0; j < i; j++) {
                if (agentDependencies.get(agentList[j]).anyMatch(targets::contains)) {
                    for (int k = i; k > j; k--) {
                        agentList[k] = agentList[k - 1];
                    }
                    agentList[j] = agentId;
                    inserted = true;
                    break;
                }
            }

            if (!inserted) {
                agentList[i] = agentId;
            }
        }

        final ImmutableIntList sortedIdentifiers = ImmutableIntList.from(agentList);
        return new ImmutablePair<>(sortedIdentifiers, agentDependenciesWithNull.toImmutable());
    }

    @Override
    public boolean updateAgent(
            int agentId, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches, ImmutableSet<BunchId> diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> startAdder,
            ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> endAdder, RuleId rule) {
        if (sourceBunches.anyMatch(diffBunches::contains)) {
            return false;
        }

        final AgentRegister<CorrelationId, RuleId> register = getAgentRegister(agentId);
        if (register == null) {
            return false;
        }

        final MutableSet<BunchId> touchedBunches = MutableHashSet.empty();
        final ImmutableSet<BunchId> currentTargetBunches = getBunchSet(register.targetBunchSetId);
        for (BunchId targetBunch : currentTargetBunches.filterNot(targetBunches::contains)) {
            if (deleteBunchAcceptationsByAgentAndBunch(_db, agentId, targetBunch)) {
                touchedBunches.add(targetBunch);
            }
        }

        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbUpdateQueryBuilder updateQueryBuilder = new DbUpdateQueryBuilder(table);

        boolean somethingChanged = false;
        boolean correlationChanged = false;
        final int targetBunchSetId = obtainBunchSet(targetBunches);
        if (targetBunchSetId != register.targetBunchSetId) {
            // TODO: old bunch set should be removed if not used by any other agent, in order to keep clean the database
            updateQueryBuilder.put(table.getTargetBunchSetColumnIndex(), targetBunchSetId);
            somethingChanged = true;
        }

        final int sourceBunchSetId = obtainBunchSet(sourceBunches);
        if (sourceBunchSetId != register.sourceBunchSetId) {
            // TODO: old bunch set should be removed if not used by any other agent, in order to keep clean the database
            updateQueryBuilder.put(table.getSourceBunchSetColumnIndex(), sourceBunchSetId);
            somethingChanged = true;
        }

        final int diffBunchSetId = obtainBunchSet(diffBunches);
        if (diffBunchSetId != register.diffBunchSetId) {
            // TODO: old bunch set should be removed if not used by any other agent, in order to keep clean the database
            updateQueryBuilder.put(table.getDiffBunchSetColumnIndex(), diffBunchSetId);
            somethingChanged = true;
        }

        final CorrelationId startMatcherId = obtainCorrelation(startMatcher);
        if (!startMatcherId.equals(register.startMatcherId)) {
            // TODO: old correlation should be removed if not used by any other acceptation or agent, in order to keep clean the database
            updateQueryBuilder.put(table.getStartMatcherColumnIndex(), startMatcherId);
            correlationChanged = true;
            somethingChanged = true;
        }

        final CorrelationId startAdderId = obtainCorrelation(startAdder);
        if (!startAdderId.equals(register.startAdderId)) {
            // TODO: old correlation should be removed if not used by any other acceptation or agent, in order to keep clean the database
            updateQueryBuilder.put(table.getStartAdderColumnIndex(), startAdderId);
            correlationChanged = true;
            somethingChanged = true;
        }

        final CorrelationId endMatcherId = obtainCorrelation(endMatcher);
        if (!endMatcherId.equals(register.endMatcherId)) {
            // TODO: old correlation should be removed if not used by any other acceptation or agent, in order to keep clean the database
            updateQueryBuilder.put(table.getEndMatcherColumnIndex(), endMatcherId);
            correlationChanged = true;
            somethingChanged = true;
        }

        final CorrelationId endAdderId = obtainCorrelation(endAdder);
        if (!endAdderId.equals(register.endAdderId)) {
            // TODO: old correlation should be removed if not used by any other acceptation or agent, in order to keep clean the database
            updateQueryBuilder.put(table.getEndAdderColumnIndex(), endAdderId);
            correlationChanged = true;
            somethingChanged = true;
        }

        final boolean ruleExpected = !equal(startMatcherId, startAdderId) || !equal(endMatcherId, endAdderId);
        final boolean rulePresent = rule != null;
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
        _db.update(updateQuery);

        final ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableSet<BunchId>>> agentExecutionOrder = getAgentExecutionOrder();
        for (int thisAgentId : agentExecutionOrder.left) {
            final ImmutableSet<BunchId> dependencies = agentExecutionOrder.right.get(thisAgentId);
            if (thisAgentId == agentId || dependencies.anyMatch(touchedBunches::contains)) {
                final boolean sourceAgentChangedText = thisAgentId != agentId && correlationChanged;
                touchedBunches.addAll(rerunAgent(thisAgentId, null, sourceAgentChangedText));
            }
        }

        final LangbookDbSchema.QuizDefinitionsTable quizzesTable = LangbookDbSchema.Tables.quizDefinitions;
        final DbQuery quizDefQuery = new DbQuery.Builder(quizzesTable)
                .select(quizzesTable.getIdColumnIndex(), quizzesTable.getBunchColumnIndex());

        final MutableIntArraySet quizzesToUpdate = MutableIntArraySet.empty();
        try (DbResult quizResult = _db.select(quizDefQuery)) {
            while (quizResult.hasNext()) {
                final List<DbValue> row = quizResult.next();
                final BunchId bunch = _bunchIdSetter.getKeyFromDbValue(row.get(1));
                if (touchedBunches.contains(bunch)) {
                    quizzesToUpdate.add(row.get(0).toInt());
                }
            }
        }

        final LangbookDbSchema.KnowledgeTable knowledgeTable = LangbookDbSchema.Tables.knowledge;
        for (int quizId : quizzesToUpdate) {
            final QuizDetails<AlphabetId, BunchId, RuleId> details = getQuizDetails(quizId);
            final ImmutableSet<AcceptationId> accs = readAllPossibleAcceptations(details.bunch, details.fields.toSet());
            final ImmutableSet<AcceptationId> accsInKnowledge = getCurrentKnowledge(quizId).keySet();
            for (AcceptationId acc : accsInKnowledge.filterNot(accs::contains)) {
                final DbDeleteQuery deleteQuery = new DbDeleteQueryBuilder(knowledgeTable)
                        .where(knowledgeTable.getQuizDefinitionColumnIndex(), quizId)
                        .where(knowledgeTable.getAcceptationColumnIndex(), acc)
                        .build();

                if (!_db.delete(deleteQuery)) {
                    throw new AssertionError();
                }
            }

            for (AcceptationId acc : accs.filterNot(accsInKnowledge::contains)) {
                final DbInsertQuery insertQuery = new DbInsertQueryBuilder(knowledgeTable)
                        .put(knowledgeTable.getQuizDefinitionColumnIndex(), quizId)
                        .put(knowledgeTable.getAcceptationColumnIndex(), acc)
                        .put(knowledgeTable.getScoreColumnIndex(), NO_SCORE)
                        .build();

                _db.insert(insertQuery);
            }
        }

        return true;
    }

    private void removeCorrelationArrayIfUnused(CorrelationArrayId correlationArray) {
        if (!isCorrelationArrayInUse(correlationArray)) {
            final List<CorrelationId> correlationIds = getCorrelationArray(correlationArray);
            if (!deleteCorrelationArray(_db, correlationArray)) {
                throw new AssertionError();
            }

            for (CorrelationId correlationId : correlationIds) {
                if (!isCorrelationInUse(correlationId)) {
                    final ImmutableSet<SymbolArrayId> symbolArrayIds = getCorrelationSymbolArrayIds(correlationId);

                    if (!deleteCorrelation(_db, correlationId)) {
                        throw new AssertionError();
                    }

                    for (SymbolArrayId symbolArrayId : symbolArrayIds) {
                        if (!isSymbolArrayInUse(symbolArrayId) && !deleteSymbolArray(_db, symbolArrayId)) {
                            throw new AssertionError();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void removeAgent(int agentId) {
        // This implementation has lot of holes.
        // 1. It is assuming that there is no chained agents
        // 2. It is assuming that agents sets only contains a single agent.
        // TODO: Improve this logic once it is centralised and better defined

        deleteBunchAcceptationsByAgent(_db, agentId);
        final AgentRegister<CorrelationId, RuleId> agentRegister = getAgentRegister(agentId);
        final ImmutableSet<BunchId> targetBunches = getBunchSet(agentRegister.targetBunchSetId);

        final ImmutableSet<AcceptationId> ruledAcceptations = getAllRuledAcceptationsForAgent(agentId);
        for (AcceptationId ruleAcceptation : ruledAcceptations) {
            if (!deleteStringQueriesForDynamicAcceptation(_db, ruleAcceptation)) {
                throw new AssertionError();
            }

            if (!deleteRuledAcceptation(_db, ruleAcceptation)) {
                throw new AssertionError();
            }

            final CorrelationArrayId correlationArray = correlationArrayFromAcceptation(ruleAcceptation);
            if (!deleteAcceptation(_db, ruleAcceptation)) {
                throw new AssertionError();
            }

            deleteSpansByDynamicAcceptation(_db, ruleAcceptation);
            removeCorrelationArrayIfUnused(correlationArray);
        }

        if (!LangbookDeleter.deleteAgent(_db, agentId)) {
            throw new AssertionError();
        }

        if (agentRegister.targetBunchSetId != 0 &&
                !isBunchSetInUse(agentRegister.targetBunchSetId) &&
                !deleteBunchSet(_db, agentRegister.targetBunchSetId)) {
            throw new AssertionError();
        }

        if (agentRegister.sourceBunchSetId != 0 &&
                !isBunchSetInUse(agentRegister.sourceBunchSetId) &&
                !deleteBunchSet(_db, agentRegister.sourceBunchSetId)) {
            throw new AssertionError();
        }

        if (agentRegister.diffBunchSetId != 0 &&
                !isBunchSetInUse(agentRegister.diffBunchSetId) &&
                !deleteBunchSet(_db, agentRegister.diffBunchSetId)) {
            throw new AssertionError();
        }

        ImmutableSet<BunchId> updatedBunches = targetBunches;
        while (!updatedBunches.isEmpty()) {
            ImmutableSet.Builder<BunchId> builder = new ImmutableHashSet.Builder<>();
            for (BunchId bunch : updatedBunches) {
                for (IntKeyMap.Entry<ImmutableSet<BunchId>> entry : findAffectedAgentsByItsSourceWithTarget(bunch).entries()) {
                    rerunAgent(entry.key(), null, false);
                    for (BunchId b : entry.value()) {
                        builder.add(b);
                    }
                }
            }
            updatedBunches = builder.build();
        }
    }

    private ImmutableSet<AcceptationId> findMatchingAcceptationsAmongGiven(
            Set<AcceptationId> acceptations, ImmutableSet<BunchId> sourceBunches,
            ImmutableSet<BunchId> diffBunches, ImmutableCorrelation<AlphabetId> startMatcher,
            ImmutableCorrelation<AlphabetId> endMatcher) {

        final MutableSet<AcceptationId> filteredAcceptations = acceptations.mutate();
        for (BunchId bunch : diffBunches) {
            final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
            final DbQuery query = new DbQueryBuilder(table)
                    .where(table.getBunchColumnIndex(), bunch)
                    .select(table.getAcceptationColumnIndex());
            try (DbResult result = _db.select(query)) {
                while (result.hasNext()) {
                    filteredAcceptations.remove(_acceptationIdSetter.getKeyFromDbValue(result.next().get(0)));
                }
            }
        }

        final ImmutableSet.Builder<AcceptationId> builder = new ImmutableHashSet.Builder<>();
        if (!sourceBunches.isEmpty()) {
            for (BunchId bunch : sourceBunches) {
                final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
                final DbQuery query = new DbQueryBuilder(table)
                        .where(table.getBunchColumnIndex(), bunch)
                        .select(table.getAcceptationColumnIndex());
                try (DbResult result = _db.select(query)) {
                    while (result.hasNext()) {
                        final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(result.next().get(0));
                        if (filteredAcceptations.contains(acc)) {
                            builder.add(acc);
                        }
                    }
                }
            }
        }
        final ImmutableSet<AcceptationId> matchingAcceptations = builder.build();

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
            final ImmutableCorrelation<AlphabetId> texts = getAcceptationTexts(acc);
            return matchingAlphabets.anyMatch(alphabet -> {
                final String text = texts.get(alphabet, null);
                return text == null || !text.startsWith(sMatcher.get(alphabet)) || !text.endsWith(eMatcher.get(alphabet));
            });
        });
    }

    private void removeFromBunches(AcceptationIdInterface acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .build();

        _db.delete(query);
    }

    private void removeFromStringQueryTable(AcceptationIdInterface acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .build();

        _db.delete(query);
    }

    private void removeFromSentenceSpans(AcceptationIdInterface acceptation) {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .build();

        _db.delete(query);
    }

    private boolean removeAcceptationInternal(AcceptationId acceptation) {
        if (findRuledAcceptationByBaseAcceptation(acceptation).anyMatch(acc -> !removeAcceptationInternal(acc))) {
            throw new AssertionError();
        }

        LangbookDeleter.deleteKnowledge(_db, acceptation);
        removeFromBunches(acceptation);
        removeFromStringQueryTable(acceptation);
        removeFromSentenceSpans(acceptation);

        final ConceptId concept = conceptFromAcceptation(acceptation);
        final CorrelationArrayId correlationArray = correlationArrayFromAcceptation(acceptation);
        final boolean removed = LangbookDeleter.deleteAcceptation(_db, acceptation);
        deleteSearchHistoryForAcceptation(_db, acceptation);

        if (removed) {
            final boolean withoutSynonymsOrTranslations = findAcceptationsByConcept(concept).size() <= 1;
            if (withoutSynonymsOrTranslations) {
                deleteBunch(_db, _bunchIdSetter.getKeyFromConceptId(concept));
            }

            deleteRuledAcceptation(_db, acceptation);
            removeCorrelationArrayIfUnused(correlationArray);
        }

        return removed;
    }

    private ImmutableSet<BunchId> rerunAgentWhenAcceptationIncludedInBunch(int agentId, MutableSet<AcceptationId> addedAcceptations) {
        final AgentDetails<AlphabetId, BunchId, RuleId> agentDetails = getAgentDetails(agentId);
        final ImmutableSet<AcceptationId> matchingAcceptations = findMatchingAcceptationsAmongGiven(addedAcceptations, agentDetails.sourceBunches,
                agentDetails.diffBunches, agentDetails.startMatcher, agentDetails.endMatcher);

        boolean targetChanged = false;
        final boolean ruleApplied = agentDetails.modifyCorrelations();
        if (!ruleApplied) {
            final ImmutableSet<AcceptationId> acceptationAlreadyInTarget = getAcceptationsInBunchByBunchAndAgent(agentDetails.targetBunches.valueAt(0), agentId)
                    .filter(addedAcceptations::contains);

            for (AcceptationId acc : addedAcceptations) {
                final boolean alreadyInTarget = acceptationAlreadyInTarget.contains(acc);
                final boolean isMatching = matchingAcceptations.contains(acc);
                if (isMatching && !alreadyInTarget) {
                    for (BunchId target : agentDetails.targetBunches) {
                        insertBunchAcceptation(_db, target, acc, agentId);
                    }

                    targetChanged = true;
                }
                else if (!isMatching && alreadyInTarget) {
                    if (!deleteBunchAcceptationsByAgentAndAcceptation(_db, agentId, acc)) {
                        throw new AssertionError();
                    }

                    targetChanged = true;
                }
            }
        }
        else {
            final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap();
            final ImmutableSet<AlphabetId> conversionTargets = conversionMap.keySet();
            final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions = new SyncCacheMap<>(this::getConversion);
            final SyncCacheMap<AlphabetId, AlphabetId> mainAlphabets = new SyncCacheMap<>(this::readMainAlphabetFromAlphabet);

            // This is assuming that matcher, adder, rule and flags did not change from last run,
            // only its source and diff bunches and its contents
            final ImmutableMap<AcceptationId, AcceptationId> oldProcessedMap = getFilteredAgentProcessedMap(agentId, addedAcceptations);
            for (AcceptationId acc : addedAcceptations.toImmutable()) {
                final boolean isMatching = matchingAcceptations.contains(acc);
                final AcceptationId dynAcc = oldProcessedMap.get(acc, null);
                if (!isMatching && dynAcc != null) {
                    targetChanged |= !agentDetails.targetBunches.isEmpty();
                    removeAcceptationInternal(dynAcc);
                }
                else if (isMatching && dynAcc == null) {
                    final ImmutablePair<ImmutableCorrelation<AlphabetId>, AcceptationId> textsAndMain = readAcceptationTextsAndMain(acc);
                    final MutableCorrelation<AlphabetId> correlation = textsAndMain.left.mutate();

                    final boolean validConversion = applyMatchersAddersAndConversions(correlation, agentDetails,
                            conversionMap, conversions);
                    if (validConversion) {
                        final ImmutableMap.Builder<AlphabetId, SymbolArrayId> corrBuilder = new ImmutableHashMap.Builder<>();
                        for (ImmutableMap.Entry<AlphabetId, String> entry : correlation.entries()) {
                            if (!conversionTargets.contains(entry.key())) {
                                corrBuilder.put(entry.key(), obtainSymbolArray(entry.value()));
                            }
                        }

                        final CorrelationId correlationId = obtainCorrelation(corrBuilder.build());
                        final CorrelationArrayId correlationArrayId = obtainSimpleCorrelationArray(correlationId);
                        final ConceptId baseConcept = conceptFromAcceptation(acc);
                        final ConceptId ruledConcept = obtainRuledConcept(agentDetails.rule, baseConcept);
                        final AcceptationId newAcc = insertAcceptation(_db, _acceptationIdSetter, ruledConcept, correlationArrayId);
                        insertRuledAcceptation(_db, newAcc, agentId, acc);
                        addedAcceptations.add(newAcc);

                        for (Map.Entry<AlphabetId, String> entry : correlation.entries()) {
                            final String mainText = correlation.get(mainAlphabets.get(entry.key()), entry.value());
                            insertStringQuery(_db, entry.value(), mainText, textsAndMain.right, newAcc, entry.key());
                        }

                        for (BunchId targetBunch : agentDetails.targetBunches) {
                            insertBunchAcceptation(_db, targetBunch, newAcc, agentId);
                            targetChanged = true;
                        }
                    }
                }
            }
        }

        return targetChanged? agentDetails.targetBunches : ImmutableHashSet.empty();
    }

    private void recheckPossibleQuestions(int quizId) {
        final QuizDetails<AlphabetId, BunchId, RuleId> quiz = getQuizDetails(quizId);
        final ImmutableSet<AcceptationId> possibleAcceptations = readAllPossibleAcceptations(quiz.bunch, quiz.fields.toSet());
        final ImmutableSet<AcceptationId> registeredAcceptations = getCurrentKnowledge(quizId).keySet();

        for (AcceptationId acceptation : registeredAcceptations.filterNot(possibleAcceptations::contains)) {
            if (!deleteKnowledge(_db, quizId, acceptation)) {
                throw new AssertionError();
            }
        }

        insertAllPossibilities(_db, quizId, possibleAcceptations.filterNot(registeredAcceptations::contains));
    }

    private void recheckQuizzes(ImmutableSet<BunchId> updatedBunches) {
        final ImmutableIntSet.Builder affectedQuizzesBuilder = new ImmutableIntSetCreator();
        for (BunchId b : updatedBunches) {
            for (int quizId : findQuizzesByBunch(b)) {
                affectedQuizzesBuilder.add(quizId);
            }
        }

        for (int quizId : affectedQuizzesBuilder.build()) {
            recheckPossibleQuestions(quizId);
        }
    }

    @Override
    public boolean addAcceptationInBunch(BunchId bunch, AcceptationId acceptation) {
        if (isAcceptationStaticallyInBunch(bunch, acceptation)) {
            return false;
        }

        LangbookDbInserter.insertBunchAcceptation(_db, bunch, acceptation, 0);

        final ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableSet<BunchId>>> agentSortingResult = getAgentExecutionOrder();
        final MutableSet<BunchId> updatedBunches = MutableHashSet.empty();
        updatedBunches.add(bunch);

        MutableSet<AcceptationId> addedAcceptations = MutableHashSet.empty();
        addedAcceptations.add(acceptation);

        for (int agentId : agentSortingResult.left) {
            if (agentSortingResult.right.get(agentId).anyMatch(updatedBunches::contains)) {
                updatedBunches.addAll(rerunAgentWhenAcceptationIncludedInBunch(agentId, addedAcceptations));
            }
        }

        recheckQuizzes(updatedBunches.toImmutable());
        return true;
    }

    @Override
    public boolean removeAcceptationFromBunch(BunchId bunch, AcceptationId acceptation) {
        if (LangbookDeleter.deleteBunchAcceptation(_db, bunch, acceptation, 0)) {
            final ImmutableSet.Builder<BunchId> allUpdatedBunchesBuilder = new ImmutableHashSet.Builder<>();
            ImmutableSet<BunchId> updatedBunches = new ImmutableHashSet.Builder<BunchId>().add(bunch).build();
            MutableSet<AcceptationId> removedDynamicAcceptations = MutableHashSet.empty();
            while (!updatedBunches.isEmpty()) {
                final ImmutableSet.Builder<BunchId> builder = new ImmutableHashSet.Builder<>();
                for (BunchId b : updatedBunches) {
                    allUpdatedBunchesBuilder.add(b);
                    for (IntKeyMap.Entry<ImmutableSet<BunchId>> entry : findAffectedAgentsByItsSourceWithTarget(b).entries()) {
                        rerunAgent(entry.key(), removedDynamicAcceptations, false);
                        for (BunchId bb : entry.value()) {
                            builder.add(bb);
                        }
                    }

                    for (IntKeyMap.Entry<ImmutableSet<BunchId>> entry : findAffectedAgentsByItsDiffWithTarget(b).entries()) {
                        rerunAgent(entry.key(), removedDynamicAcceptations, false);
                        for (BunchId bb : entry.value()) {
                            builder.add(bb);
                        }
                    }
                }
                updatedBunches = builder.build();
            }

            for (AcceptationId dynAcc : removedDynamicAcceptations) {
                deleteSpansByDynamicAcceptation(_db, dynAcc);
            }

            recheckQuizzes(allUpdatedBunchesBuilder.build());
            return true;
        }

        return false;
    }

    @Override
    public LanguageCreationResult<LanguageId, AlphabetId> addLanguage(String code) {
        if (findLanguageByCode(code) != null) {
            return null;
        }

        final int lastConcept = getMaxConcept() + 1;
        final LanguageId language = _languageIdSetter.getKeyFromInt(lastConcept + 1);
        final AlphabetId alphabet = _alphabetIdSetter.getKeyFromInt(lastConcept + 2);
        LangbookDbInserter.insertLanguage(_db, language, code, alphabet);
        insertAlphabet(_db, alphabet, language);

        return new LanguageCreationResult<>(language, alphabet);
    }

    @Override
    public boolean removeLanguage(LanguageId language) {
        // For now, if there is a bunch whose concept is only linked to acceptations of the language to be removed,
        // the removal is rejected, as there will not be any way to access that bunch any more in an AcceptationsDetailsActivity.
        // Only exception to the previous rule is the case where all acceptations within the bunch belongs to the language that is about to be removed.
        final ImmutableIntSet linkedBunches = findBunchConceptsLinkedToJustThisLanguage(language);
        if (!linkedBunches.isEmpty()) {
            if (linkedBunches.anyMatch(bunch -> {
                final ImmutableSet<LanguageId> languages = findIncludedAcceptationLanguages(bunch);
                return languages.size() > 1 || languages.size() == 1 && !languages.valueAt(0).equals(language);
            })) {
                return false;
            }
        }

        // For now, if there is a super type whose concept is only linked to acceptations of the language to be removed,
        // the removal is rejected, as there will not be any way to access that supertype any more in an AcceptationsDetailsActivity
        if (!findSuperTypesLinkedToJustThisLanguage(language).isEmpty()) {
            return false;
        }

        final ImmutableIntSet correlationIds = findCorrelationsByLanguage(language);
        final ImmutableIntSet correlationUsedInAgents = findCorrelationsUsedInAgents();

        // For now, if there are agents using affected correlations. This rejects to remove the language
        if (!correlationIds.filter(correlationUsedInAgents::contains).isEmpty()) {
            return false;
        }

        final ImmutableSet<AcceptationId> acceptationIds = findAcceptationsByLanguage(language);
        for (AcceptationId acceptation : acceptationIds) {
            if (!removeAcceptation(acceptation)) {
                throw new AssertionError();
            }
        }

        final ImmutableSet<AlphabetId> alphabets = findAlphabetsByLanguage(language);
        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap();
        final int size = conversionMap.size();
        for (int i = 0; i < size; i++) {
            final AlphabetId sourceAlphabet = conversionMap.valueAt(i);
            if (alphabets.contains(sourceAlphabet)) {
                final AlphabetId targetAlphabet = conversionMap.keyAt(i);
                if (!replaceConversion(new Conversion<>(sourceAlphabet, targetAlphabet, ImmutableHashMap.empty()))) {
                    throw new AssertionError();
                }
            }
        }

        if (!LangbookDeleter.deleteAlphabetsForLanguage(_db, language) || !LangbookDeleter.deleteLanguage(_db, language)) {
            throw new AssertionError();
        }

        return true;
    }

    private static final class StringQueryTableRow<AcceptationId> {
        final AcceptationId mainAcceptation;
        final AcceptationId dynamicAcceptation;
        final String text;
        final String mainText;

        StringQueryTableRow(AcceptationId mainAcceptation, AcceptationId dynamicAcceptation, String text, String mainText) {
            this.mainAcceptation = mainAcceptation;
            this.dynamicAcceptation = dynamicAcceptation;
            this.text = text;
            this.mainText = mainText;
        }
    }

    @Override
    public boolean addAlphabetCopyingFromOther(AlphabetId alphabet, AlphabetId sourceAlphabet) {
        if (isAlphabetPresent(alphabet)) {
            return false;
        }

        final LanguageId language = getLanguageFromAlphabet(sourceAlphabet);
        if (language == null) {
            return false;
        }

        if (getConversionsMap().keySet().contains(sourceAlphabet)) {
            return false;
        }

        insertAlphabet(_db, alphabet, language);

        final ImmutableMap<CorrelationId, SymbolArrayId> correlations = findCorrelationsAndSymbolArrayForAlphabet(sourceAlphabet);
        final int correlationCount = correlations.size();
        for (int i = 0; i < correlationCount; i++) {
            LangbookDbInserter.insertCorrelationEntry(_db, correlations.keyAt(i), alphabet, correlations.valueAt(i));
        }

        // Some kind of query for duplicating rows should be valuable. The following logic will be broken if a new column is added or removed for the table.
        //TODO: Change this logic
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getStringAlphabetColumnIndex(), sourceAlphabet)
                .select(
                        table.getMainAcceptationColumnIndex(),
                        table.getDynamicAcceptationColumnIndex(),
                        table.getStringColumnIndex(), table.getMainStringColumnIndex());

        final List<StringQueryTableRow<AcceptationId>> rows = _db.select(query).map(row -> {
            final AcceptationId mainAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
            final AcceptationId dynAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
            return new StringQueryTableRow<>(mainAcc, dynAcc, row.get(2).toText(), row.get(3).toText());
        }).toList();
        for (StringQueryTableRow<AcceptationId> row : rows) {
            LangbookDbInserter.insertStringQuery(_db, row.text, row.mainText, row.mainAcceptation, row.dynamicAcceptation, alphabet);
        }

        return true;
    }

    private Conversion<AlphabetId> updateJustConversion(Conversion<AlphabetId> newConversion) {
        final Conversion<AlphabetId> oldConversion = getConversion(newConversion.getAlphabets());

        final ImmutableSet<Map.Entry<String, String>> oldPairs = oldConversion.getMap().entries();
        final ImmutableSet<Map.Entry<String, String>> newPairs = newConversion.getMap().entries();

        final ImmutableSet<Map.Entry<String, String>> pairsToRemove = oldPairs.filterNot(newPairs::contains);
        final ImmutableSet<Map.Entry<String, String>> pairsToInclude = newPairs.filterNot(oldPairs::contains);

        for (Map.Entry<String, String> pair : pairsToRemove) {
            // TODO: SymbolArrays should also be removed if not used by any other, just to avoid dirty databases
            final SymbolArrayId sourceId = findSymbolArray(pair.key());
            final SymbolArrayId targetId = findSymbolArray(pair.value());
            if (!LangbookDeleter.deleteConversionRegister(_db, newConversion.getAlphabets(), sourceId, targetId)) {
                throw new AssertionError();
            }
        }

        for (Map.Entry<String, String> pair : pairsToInclude) {
            final SymbolArrayId sourceId = obtainSymbolArray(pair.key());
            final SymbolArrayId targetId = obtainSymbolArray(pair.value());
            LangbookDbInserter.insertConversion(_db, newConversion.getSourceAlphabet(), newConversion.getTargetAlphabet(), sourceId, targetId);
        }

        return oldConversion;
    }

    private void applyConversion(Conversion<AlphabetId> conversion) {
        final AlphabetId sourceAlphabet = conversion.getSourceAlphabet();

        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getStringAlphabetColumnIndex(), sourceAlphabet)
                .select(
                        table.getStringColumnIndex(),
                        table.getMainStringColumnIndex(),
                        table.getMainAcceptationColumnIndex(),
                        table.getDynamicAcceptationColumnIndex());

        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final String str = conversion.convert(row.get(0).toText());
                if (str == null) {
                    throw new AssertionError("Unable to convert word " + row.get(0).toText());
                }

                final String mainStr = row.get(1).toText();
                final AcceptationId mainAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(2));
                final AcceptationId dynAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(3));

                insertStringQuery(_db, str, mainStr, mainAcc, dynAcc, conversion.getTargetAlphabet());
            }
        }
    }

    @Override
    public boolean addAlphabetAsConversionTarget(Conversion<AlphabetId> conversion) {
        final LanguageId language = getLanguageFromAlphabet(conversion.getSourceAlphabet());
        if (language == null) {
            return false;
        }

        if (isAlphabetPresent(conversion.getTargetAlphabet())) {
            return false;
        }

        if (!checkConversionConflicts(conversion)) {
            return false;
        }

        insertAlphabet(_db, conversion.getTargetAlphabet(), language);

        if (!updateJustConversion(conversion).getMap().isEmpty()) {
            throw new AssertionError();
        }

        applyConversion(conversion);
        return true;
    }

    @Override
    public boolean removeAlphabet(AlphabetId alphabet) {
        // There must be at least another alphabet in the same language to avoid leaving the language without alphabets
        if (alphabetsWithinLanguage(alphabet).size() < 2) {
            return false;
        }

        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap();
        if (conversionMap.contains(alphabet)) {
            return false;
        }

        if (isAlphabetUsedInQuestions(alphabet)) {
            return false;
        }

        boolean changed = false;
        if (conversionMap.keySet().contains(alphabet)) {
            if (!deleteConversion(_db, conversionMap.get(alphabet), alphabet)) {
                throw new AssertionError();
            }
            changed = true;
        }

        changed |= deleteAlphabetFromStringQueries(_db, alphabet);
        changed |= deleteAlphabetFromCorrelations(_db, alphabet);
        changed |= deleteAlphabet(_db, alphabet);
        return changed;
    }

    private AcceptationId addAcceptation(ConceptId concept, CorrelationArrayId correlationArrayId) {
        final Correlation<AlphabetId> texts = readCorrelationArrayTextAndItsAppliedConversions(correlationArrayId);
        if (texts == null) {
            return null;
        }

        final String mainStr = texts.valueAt(0);
        final AcceptationId acceptation = insertAcceptation(_db, _acceptationIdSetter, concept, correlationArrayId);
        for (Map.Entry<AlphabetId, String> entry : texts.entries()) {
            final AlphabetId alphabet = entry.key();
            final String str = entry.value();
            insertStringQuery(_db, str, mainStr, acceptation, acceptation, alphabet);
        }

        final ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableSet<BunchId>>> sortedAgents = getAgentExecutionOrder();
        final ImmutableIntKeyMap<ImmutableSet<BunchId>> agentDependencies = sortedAgents.right;
        final MutableSet<BunchId> touchedBunches = MutableHashSet.empty();
        touchedBunches.add(null);

        for (int agentId : sortedAgents.left) {
            if (!agentDependencies.get(agentId).filter(touchedBunches::contains).isEmpty()) {
                touchedBunches.addAll(rerunAgent(agentId, null, false));
            }
        }

        return acceptation;
    }

    @Override
    public AcceptationId addAcceptation(ConceptId concept, ImmutableCorrelationArray<AlphabetId> correlationArray) {
        final CorrelationArrayId correlationArrayId = obtainCorrelationArray(correlationArray.map(this::obtainCorrelation));
        return addAcceptation(concept, correlationArrayId);
    }

    @Override
    public boolean updateAcceptationCorrelationArray(AcceptationId acceptation, ImmutableCorrelationArray<AlphabetId> correlationArray) {
        final CorrelationArrayId newCorrelationArrayId = obtainCorrelationArray(correlationArray.map(this::obtainCorrelation));
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getIdColumnIndex(), acceptation)
                .put(table.getCorrelationArrayColumnIndex(), newCorrelationArrayId)
                .build();

        final boolean changed = _db.update(query);
        if (changed) {
            final Correlation<AlphabetId> texts = readCorrelationArrayTextAndItsAppliedConversions(newCorrelationArrayId);
            if (texts == null) {
                throw new AssertionError();
            }

            final String mainStr = texts.valueAt(0);
            for (Map.Entry<AlphabetId, String> entry : texts.entries()) {
                final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
                final DbUpdateQuery updateQuery = new DbUpdateQueryBuilder(strings)
                        .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                        .where(strings.getStringAlphabetColumnIndex(), entry.key())
                        .put(strings.getMainStringColumnIndex(), mainStr)
                        .put(strings.getStringColumnIndex(), entry.value())
                        .build();

                if (!_db.update(updateQuery)) {
                    throw new AssertionError();
                }
            }

            final MutableSet<BunchId> touchedBunches = MutableHashSet.empty();

            final ImmutableIntSet.Builder affectedAgentsBuilder = new ImmutableIntSetCreator();
            for (int agentId : findAgentsWithoutSourceBunches()) {
                affectedAgentsBuilder.add(agentId);
            }

            for (int agentId : findAffectedAgentsByAcceptationCorrelationModification(acceptation)) {
                affectedAgentsBuilder.add(agentId);
            }

            final ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableSet<BunchId>>> agentExecutionOrder = getAgentExecutionOrder();
            final ImmutableIntSet affectedAgents = affectedAgentsBuilder.build();
            for (int thisAgentId : agentExecutionOrder.left) {
                final ImmutableSet<BunchId> dependencies = agentExecutionOrder.right.get(thisAgentId);
                if (affectedAgents.contains(thisAgentId) || dependencies.anyMatch(touchedBunches::contains)) {
                    touchedBunches.addAll(rerunAgent(thisAgentId, null, false));
                }
            }

            final ImmutableIntSet.Builder quizIdsBuilder = new ImmutableIntSetCreator();
            final LangbookDbSchema.QuizDefinitionsTable quizzes = LangbookDbSchema.Tables.quizDefinitions;
            final DbQuery quizQuery = new DbQuery.Builder(quizzes)
                    .select(quizzes.getIdColumnIndex(), quizzes.getBunchColumnIndex());
            try (DbResult result = _db.select(quizQuery)) {
                while (result.hasNext()) {
                    final List<DbValue> row = result.next();
                    final BunchId quizBunch = _bunchIdSetter.getKeyFromDbValue(row.get(1));
                    if (quizBunch == null || touchedBunches.contains(quizBunch)) {
                        quizIdsBuilder.add(row.get(0).toInt());
                    }
                }
            }

            for (int quizId : quizIdsBuilder.build()) {
                recheckPossibleQuestions(quizId);
            }
        }

        return changed;
    }

    private boolean canAcceptationBeRemoved(AcceptationId acceptation) {
        final ConceptId concept = conceptFromAcceptation(acceptation);
        final boolean withSynonymsOrTranslations = !findAcceptationsByConcept(concept).remove(acceptation).isEmpty();
        return (withSynonymsOrTranslations || !hasAgentsRequiringAcceptation(concept)) &&
                !findRuledAcceptationByBaseAcceptation(acceptation).anyMatch(acc -> !canAcceptationBeRemoved(acc));
    }

    @Override
    public boolean removeAcceptation(AcceptationId acceptation) {
        if (!canAcceptationBeRemoved(acceptation)) {
            return false;
        }

        if (!removeAcceptationInternal(acceptation)) {
            throw new AssertionError();
        }

        return true;
    }

    private void updateConceptsInComplementedConcepts(ConceptId oldConcept, ConceptId newConcept) {
        final LangbookDbSchema.ComplementedConceptsTable table = LangbookDbSchema.Tables.complementedConcepts;

        DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getBaseColumnIndex(), oldConcept)
                .put(table.getBaseColumnIndex(), newConcept)
                .build();
        _db.update(query);

        query = new DbUpdateQueryBuilder(table)
                .where(table.getIdColumnIndex(), oldConcept)
                .put(table.getIdColumnIndex(), newConcept)
                .build();
        _db.update(query);

        query = new DbUpdateQueryBuilder(table)
                .where(table.getComplementColumnIndex(), oldConcept)
                .put(table.getComplementColumnIndex(), newConcept)
                .build();
        _db.update(query);
    }

    private MutableIntKeyMap<MutableSet<AcceptationId>> getAcceptationsInBunchGroupedByAgent(BunchId bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        DbQuery oldConceptQuery = new DbQueryBuilder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .select(table.getAgentColumnIndex(), table.getAcceptationColumnIndex());
        final MutableIntKeyMap<MutableSet<AcceptationId>> map = MutableIntKeyMap.empty();
        final SyncCacheIntKeyNonNullValueMap<MutableSet<AcceptationId>> syncCache = new SyncCacheIntKeyNonNullValueMap<>(map, k -> MutableHashSet.empty());
        try (DbResult dbResult = _db.select(oldConceptQuery)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                syncCache.get(row.get(0).toInt()).add(_acceptationIdSetter.getKeyFromDbValue(row.get(1)));
            }
        }

        return map;
    }

    private void updateBunchAcceptationConcepts(ConceptId oldConcept, ConceptId newConcept) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final MutableIntKeyMap<MutableSet<AcceptationId>> oldBunchAcceptations = getAcceptationsInBunchGroupedByAgent(_bunchIdSetter.getKeyFromConceptId(oldConcept));
        if (oldBunchAcceptations.isEmpty()) {
            return;
        }

        final MutableIntKeyMap<MutableSet<AcceptationId>> newBunchAcceptations = getAcceptationsInBunchGroupedByAgent(_bunchIdSetter.getKeyFromConceptId(newConcept));
        final ImmutableIntSet involvedAgents = oldBunchAcceptations.keySet().toImmutable().addAll(newBunchAcceptations.keySet());

        final ImmutableIntKeyMap<Set<AcceptationId>> duplicated = involvedAgents.assign(agent -> {
            MutableSet<AcceptationId> rawNewAccSet = newBunchAcceptations.get(agent, null);
            final MutableSet<AcceptationId> newAccSet = (rawNewAccSet == null)? MutableHashSet.empty() : rawNewAccSet;
            MutableSet<AcceptationId> rawOldAccSet = oldBunchAcceptations.get(agent, null);
            final MutableSet<AcceptationId> oldAccSet = (rawOldAccSet == null)? MutableHashSet.empty() : rawOldAccSet;
            return newAccSet.filter(oldAccSet::contains);
        }).filterNot(Set::isEmpty);

        for (int agent : duplicated.keySet()) {
            for (AcceptationId acc : duplicated.get(agent)) {
                if (!deleteBunchAcceptation(_db, _bunchIdSetter.getKeyFromConceptId(oldConcept), acc, agent)) {
                    throw new AssertionError();
                }
            }
        }

        final DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getBunchColumnIndex(), oldConcept)
                .put(table.getBunchColumnIndex(), newConcept)
                .build();
        _db.update(query);
    }

    private void updateQuestionRules(RuleId oldRule, RuleId newRule) {
        final LangbookDbSchema.QuestionFieldSets table = LangbookDbSchema.Tables.questionFieldSets;
        DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getRuleColumnIndex(), oldRule)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        _db.update(query);
    }

    private void updateQuizBunches(BunchId oldBunch, BunchId newBunch) {
        final LangbookDbSchema.QuizDefinitionsTable table = LangbookDbSchema.Tables.quizDefinitions;
        DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getBunchColumnIndex(), oldBunch)
                .put(table.getBunchColumnIndex(), newBunch)
                .build();
        _db.update(query);
    }

    private void updateBunchSetsInAgents(int oldBunchSetId, int newBunchSetId) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getTargetBunchSetColumnIndex(), oldBunchSetId)
                .put(table.getTargetBunchSetColumnIndex(), newBunchSetId)
                .build();
        _db.update(query);

        query = new DbUpdateQuery.Builder(table)
                .where(table.getSourceBunchSetColumnIndex(), oldBunchSetId)
                .put(table.getSourceBunchSetColumnIndex(), newBunchSetId)
                .build();
        _db.update(query);

        query = new DbUpdateQuery.Builder(table)
                .where(table.getDiffBunchSetColumnIndex(), oldBunchSetId)
                .put(table.getDiffBunchSetColumnIndex(), newBunchSetId)
                .build();
        _db.update(query);
    }

    private void updateBunchSetBunches(BunchId oldBunch, BunchId newBunch) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final IntKeyMap<MutableSet<BunchId>> oldBunchSets = readBunchSetsWhereBunchIsIncluded(oldBunch);
        if (oldBunchSets.isEmpty()) {
            return;
        }

        final MutableIntKeyMap<MutableSet<BunchId>> newBunchSets = readBunchSetsWhereBunchIsIncluded(newBunch);
        if (!newBunchSets.isEmpty()) {
            for (int index : oldBunchSets.indexes()) {
                final int oldSetId = oldBunchSets.keyAt(index);
                final MutableSet<BunchId> oldSet = oldBunchSets.valueAt(index);

                final boolean hasBoth = oldSet.contains(newBunch);
                if (hasBoth) {
                    final ImmutableSet<BunchId> desiredBunch = oldSet.toImmutable().remove(oldBunch);
                    final int reusableBunchIndex = newBunchSets.indexWhere(desiredBunch::equalSet);
                    if (reusableBunchIndex >= 0) {
                        updateBunchSetsInAgents(oldSetId, newBunchSets.keyAt(reusableBunchIndex));
                        deleteBunchSet(_db, oldSetId);
                    }
                    else {
                        if (!deleteBunchSetBunch(_db, oldSetId, oldBunch)) {
                            throw new AssertionError();
                        }
                    }
                }
                else {
                    final ImmutableSet<BunchId> set = oldSet.filterNot(v -> equal(v, oldBunch)).toImmutable().add(newBunch);
                    final int foundIndex = newBunchSets.indexWhere(set::equalSet);
                    if (foundIndex >= 0) {
                        if (!deleteBunchSet(_db, oldSetId)) {
                            throw new AssertionError();
                        }

                        updateBunchSetsInAgents(oldSetId, newBunchSets.keyAt(foundIndex));
                    }
                }
            }
        }

        DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getBunchColumnIndex(), oldBunch)
                .put(table.getBunchColumnIndex(), newBunch)
                .build();
        _db.update(query);
    }

    private void updateAgentRules(RuleId oldRule, RuleId newRule) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getRuleColumnIndex(), oldRule)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        _db.update(query);
    }

    private Map<AcceptationId, CorrelationArrayId> getAcceptationsByConcept(ConceptId concept) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery readQuery = new DbQueryBuilder(table)
                .where(table.getConceptColumnIndex(), concept)
                .select(table.getIdColumnIndex(), table.getCorrelationArrayColumnIndex());

        final MutableMap<AcceptationId, CorrelationArrayId> map = MutableHashMap.empty();
        try (DbResult dbResult = _db.select(readQuery)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                map.put(_acceptationIdSetter.getKeyFromDbValue(row.get(0)), _correlationArrayIdSetter.getKeyFromDbValue(row.get(1)));
            }
        }

        return map;
    }

    private void updateAcceptationConcepts(ConceptId oldConcept, ConceptId newConcept) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getConceptColumnIndex(), oldConcept)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        _db.update(query);
    }

    private void updateRuledConceptsConcept(ConceptId oldConcept, ConceptId newConcept) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getConceptColumnIndex(), oldConcept)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        _db.update(query);
    }

    private void updateRuledConceptsRule(ConceptId ruledConcept, RuleId newRule) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getIdColumnIndex(), ruledConcept)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        _db.update(query);
    }

    private boolean mergeConcepts(ConceptId linkedConcept, ConceptId oldConcept) {
        if (equal(oldConcept, linkedConcept)) {
            return false;
        }

        final ImmutableSet<ConceptId> nonLinkableConcepts = getAlphabetAndLanguageConcepts();
        if (nonLinkableConcepts.contains(linkedConcept)) {
            return false;
        }

        if (oldConcept == null || linkedConcept == null) {
            throw new AssertionError();
        }

        final BunchId oldConceptAsBunch = _bunchIdSetter.getKeyFromConceptId(oldConcept);
        final BunchId linkedConceptAsBunch = _bunchIdSetter.getKeyFromConceptId(linkedConcept);
        final RuleId oldConceptAsRule = _ruleIdSetter.getKeyFromConceptId(oldConcept);
        final RuleId linkedConceptAsRule = _ruleIdSetter.getKeyFromConceptId(linkedConcept);

        updateConceptsInComplementedConcepts(oldConcept, linkedConcept);
        updateBunchAcceptationConcepts(oldConcept, linkedConcept);
        updateQuestionRules(oldConceptAsRule, linkedConceptAsRule);
        updateQuizBunches(oldConceptAsBunch, linkedConceptAsBunch);
        updateBunchSetBunches(oldConceptAsBunch, linkedConceptAsBunch);
        updateAgentRules(oldConceptAsRule, linkedConceptAsRule);

        final Map<AcceptationId, CorrelationArrayId> oldAcceptations = getAcceptationsByConcept(oldConcept);
        if (!oldAcceptations.isEmpty()) {
            final Map<AcceptationId, CorrelationArrayId> newAcceptations = getAcceptationsByConcept(linkedConcept);
            final Map<AcceptationId, CorrelationArrayId> repeatedAcceptations = oldAcceptations.filter(newAcceptations::contains);
            for (AcceptationId oldAcc : repeatedAcceptations.keySet()) {
                if (!deleteAcceptation(_db, oldAcc) || !deleteStringQueriesForDynamicAcceptation(_db, oldAcc)) {
                    throw new AssertionError();
                }

                final CorrelationArrayId correlationArray = oldAcceptations.get(oldAcc);
                final AcceptationId newAcc = newAcceptations.keyAt(newAcceptations.indexOf(correlationArray));

                final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
                DbQuery oldConceptQuery = new DbQueryBuilder(table)
                        .where(table.getAcceptationColumnIndex(), oldAcc)
                        .select(table.getAgentColumnIndex(), table.getBunchColumnIndex());
                final MutableIntKeyMap<MutableSet<BunchId>> map = MutableIntKeyMap.empty();
                final SyncCacheIntKeyNonNullValueMap<MutableSet<BunchId>> syncCache = new SyncCacheIntKeyNonNullValueMap<>(map, k -> MutableHashSet.empty());
                try (DbResult dbResult = _db.select(oldConceptQuery)) {
                    while (dbResult.hasNext()) {
                        final List<DbValue> row = dbResult.next();
                        syncCache.get(row.get(0).toInt()).add(_bunchIdSetter.getKeyFromDbValue(row.get(1)));
                    }
                }

                for (int agent : map.keySet()) {
                    for (BunchId bunch : map.get(agent)) {
                        insertBunchAcceptation(_db, bunch, newAcc, agent);
                    }
                }
                deleteBunchAcceptationsByAcceptation(_db, oldAcc);
            }

            updateAcceptationConcepts(oldConcept, linkedConcept);
        }

        final MutableMap<RuleId, ConceptId> oldRuledConcepts = findRuledConceptsByConceptInvertedMap(oldConcept);
        if (!oldRuledConcepts.isEmpty()) {
            final MutableMap<RuleId, ConceptId> newRuledConcepts = findRuledConceptsByConceptInvertedMap(linkedConcept);
            final ImmutableSet<RuleId> newRuledConceptsRules = newRuledConcepts.keySet().toImmutable();
            for (int oldRuledConceptIndex : oldRuledConcepts.indexes()) {
                final RuleId rule = oldRuledConcepts.keyAt(oldRuledConceptIndex);
                final ConceptId oldRuledConcept = oldRuledConcepts.valueAt(oldRuledConceptIndex);
                final boolean isCommonRule = newRuledConceptsRules.contains(rule);
                if (isCommonRule) {
                    final ConceptId newRuledConcept = newRuledConcepts.get(rule);
                    if (!deleteRuledConcept(_db, oldRuledConcept)) {
                        throw new AssertionError();
                    }

                    mergeConcepts(newRuledConcept, oldRuledConcept);
                }
                else {
                    updateRuledConceptsConcept(oldConcept, linkedConcept);
                }
            }
        }

        final ImmutableMap<ConceptId, ConceptId> oldRuledConceptsMap = findRuledConceptsByRuleInvertedMap(oldConceptAsRule);
        final int oldRuledConceptsMapSize = oldRuledConceptsMap.size();
        if (oldRuledConceptsMapSize > 0) {
            final ImmutableMap<ConceptId, ConceptId> newRuledConceptsMap = findRuledConceptsByRuleInvertedMap(linkedConceptAsRule);
            final ImmutableSet<ConceptId> newRuledConceptsMapKeys = newRuledConceptsMap.keySet();
            for (int i = 0; i < oldRuledConceptsMapSize; i++) {
                final ConceptId baseConcept = oldRuledConceptsMap.keyAt(i);
                if (newRuledConceptsMapKeys.contains(baseConcept)) {
                    mergeConcepts(newRuledConceptsMap.get(baseConcept), oldRuledConceptsMap.valueAt(i));
                }
                else {
                    updateRuledConceptsRule(oldRuledConceptsMap.valueAt(i), linkedConceptAsRule);
                }
            }
        }

        return true;
    }

    @Override
    public boolean shareConcept(AcceptationId linkedAcceptation, ConceptId oldConcept) {
        return mergeConcepts(conceptFromAcceptation(linkedAcceptation), oldConcept);
    }

    @Override
    public void duplicateAcceptationWithThisConcept(AcceptationId linkedAcceptation, ConceptId concept) {
        if (concept == null) {
            throw new AssertionError();
        }

        final CorrelationArrayId correlationArray = correlationArrayFromAcceptation(linkedAcceptation);
        addAcceptation(concept, correlationArray);
    }

    private void unapplyConversion(Conversion<AlphabetId> conversion) {
        final AlphabetId targetAlphabet = conversion.getTargetAlphabet();

        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getStringAlphabetColumnIndex(), targetAlphabet)
                .build();
        _db.delete(query);
    }

    private void updateStringQueryTableDueToConversionUpdate(Conversion<AlphabetId> newConversion) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;

        final int offset = table.columns().size();
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getDynamicAcceptationColumnIndex(), table.getDynamicAcceptationColumnIndex())
                .where(table.getStringAlphabetColumnIndex(), newConversion.getTargetAlphabet())
                .where(offset + table.getStringAlphabetColumnIndex(), newConversion.getSourceAlphabet())
                .select(
                        table.getDynamicAcceptationColumnIndex(),
                        table.getStringColumnIndex(),
                        offset + table.getStringColumnIndex());

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        final DbResult dbResult = _db.select(query);
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
            final DbUpdateQuery upQuery = new DbUpdateQueryBuilder(table)
                    .where(table.getStringAlphabetColumnIndex(), newConversion.getTargetAlphabet())
                    .where(table.getDynamicAcceptationColumnIndex(), updateMap.keyAt(i))
                    .put(table.getStringColumnIndex(), updateMap.valueAt(i))
                    .build();

            if (!_db.update(upQuery)) {
                throw new AssertionError();
            }
        }
    }

    @Override
    public boolean replaceConversion(Conversion<AlphabetId> conversion) {
        final AlphabetId sourceAlphabet = conversion.getSourceAlphabet();
        final AlphabetId targetAlphabet = conversion.getTargetAlphabet();
        final LanguageId languageObj = getLanguageFromAlphabet(sourceAlphabet);
        if (languageObj == null) {
            return false;
        }

        final LanguageId languageObj2 = getLanguageFromAlphabet(targetAlphabet);
        if (languageObj2 == null || !languageObj2.equals(languageObj)) {
            return false;
        }

        if (conversion.getMap().isEmpty()) {
            final Conversion<AlphabetId> oldConversion = getConversion(conversion.getAlphabets());
            if (oldConversion.getMap().isEmpty()) {
                return false;
            }
            else {
                unapplyConversion(oldConversion);
                updateJustConversion(conversion);
                return true;
            }
        }

        if (checkConversionConflicts(conversion)) {
            final Conversion<AlphabetId> oldConversion = updateJustConversion(conversion);
            if (oldConversion.getMap().isEmpty()) {
                applyConversion(conversion);
            }
            else {
                updateStringQueryTableDueToConversionUpdate(conversion);
            }
            return true;
        }

        return false;
    }

    private int insertQuestionFieldSet(Iterable<QuestionFieldDetails<AlphabetId, RuleId>> fields) {
        if (!fields.iterator().hasNext()) {
            return 0;
        }

        final int setId = getMaxQuestionFieldSetId() + 1;
        LangbookDbInserter.insertQuestionFieldSet(_db, setId, fields);
        return setId;
    }

    @Override
    public Integer obtainQuiz(BunchId bunch, ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields) {
        final Integer existingSetId = findQuestionFieldSet(fields);
        final Integer existingQuizId = (existingSetId != null)? findQuizDefinition(bunch, existingSetId) : null;

        final Integer quizId;
        if (existingQuizId == null) {
            final ImmutableSet<AcceptationId> acceptations = readAllPossibleAcceptations(bunch, fields.toSet());
            final int setId = (existingSetId != null) ? existingSetId : insertQuestionFieldSet(fields);
            quizId = insertQuizDefinition(_db, bunch, setId);
            insertAllPossibilities(_db, quizId, acceptations);
        }
        else {
            quizId = existingQuizId;
        }

        return quizId;
    }

    @Override
    public void removeQuiz(int quizId) {
        deleteKnowledgeForQuiz(_db, quizId);
        deleteQuiz(_db, quizId);
    }

    @Override
    public void updateScore(int quizId, AcceptationId acceptation, int score) {
        if (score < MIN_ALLOWED_SCORE || score > MAX_ALLOWED_SCORE) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.KnowledgeTable table = LangbookDbSchema.Tables.knowledge;
        final DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getQuizDefinitionColumnIndex(), quizId)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .put(table.getScoreColumnIndex(), score)
                .build();
        _db.update(query);
    }

    private ConceptId obtainConceptComposition(ImmutableSet<ConceptId> concepts) {
        final ConceptId compositionConcept = findConceptComposition(concepts);
        if (compositionConcept == null) {
            ConceptId newCompositionConcept = getNextAvailableConceptId();
            for (ConceptId concept : concepts) {
                newCompositionConcept = _conceptIdSetter.recheckAvailability(newCompositionConcept, concept);
            }

            for (ConceptId item : concepts) {
                insertConceptCompositionEntry(_db, newCompositionConcept, item);
            }

            return newCompositionConcept;
        }

        return compositionConcept;
    }

    @Override
    public void addDefinition(ConceptId baseConcept, ConceptId concept, ImmutableSet<ConceptId> complements) {
        LangbookDbInserter.insertComplementedConcept(_db, baseConcept, concept, obtainConceptComposition(complements));
    }

    @Override
    public boolean removeDefinition(ConceptId complementedConcept) {
        // TODO: This method should remove any orphan concept composition to avoid rubbish
        return deleteComplementedConcept(_db, complementedConcept);
    }

    @Override
    public void updateSearchHistory(AcceptationId dynamicAcceptation) {
        deleteSearchHistoryForAcceptation(_db, dynamicAcceptation);
        insertSearchHistoryEntry(_db, dynamicAcceptation);
    }

    @Override
    public boolean removeSentence(int sentenceId) {
        final SymbolArrayId symbolArrayId = getSentenceSymbolArray(sentenceId);
        if (symbolArrayId == null || !deleteSentence(_db, sentenceId)) {
            return false;
        }

        deleteSpansBySentenceId(_db, sentenceId);
        if (isSymbolArrayMerelyASentence(symbolArrayId) &&
                findSentencesBySymbolArrayId(symbolArrayId).isEmpty() &&
                !deleteSymbolArray(_db, symbolArrayId)) {
            throw new AssertionError();
        }

        return true;
    }

    private boolean checkValidTextAndSpans(String text, Set<SentenceSpan<AcceptationId>> spans) {
        if (text == null || text.length() == 0) {
            return false;
        }
        final int textLength = text.length();

        if (spans == null) {
            spans = ImmutableHashSet.empty();
        }

        for (SentenceSpan<AcceptationId> span : spans) {
            if (span == null || span.range.min() < 0 || span.range.max() >= textLength) {
                return false;
            }
        }

        final Set<SentenceSpan<AcceptationId>> sortedSpans = spans.sort((a, b) -> a.range.min() < b.range.min());
        final int spanCount = sortedSpans.size();

        for (int i = 1; i < spanCount; i++) {
            if (spans.valueAt(i - 1).range.max() >= spans.valueAt(i).range.min()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Integer addSentence(ConceptId concept, String text, Set<SentenceSpan<AcceptationId>> spans) {
        if (!checkValidTextAndSpans(text, spans)) {
            return null;
        }

        final SymbolArrayId symbolArray = obtainSymbolArray(text);
        final int sentenceId = insertSentence(_db, concept, symbolArray);
        for (SentenceSpan<AcceptationId> span : spans) {
            insertSpan(_db, sentenceId, span.range, span.acceptation);
        }
        return sentenceId;
    }

    private boolean updateSymbolArray(SymbolArrayIdInterface symbolArrayId, String text) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getIdColumnIndex(), symbolArrayId)
                .put(table.getStrColumnIndex(), text)
                .build();
        return _db.update(query);
    }

    private boolean updateSentenceSymbolArrayId(int sentenceId, SymbolArrayIdInterface newSymbolArrayId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbUpdateQuery query = new DbUpdateQueryBuilder(table)
                .where(table.getIdColumnIndex(), sentenceId)
                .put(table.getSymbolArrayColumnIndex(), newSymbolArrayId)
                .build();
        return _db.update(query);
    }

    @Override
    public boolean updateSentenceTextAndSpans(int sentenceId, String newText, Set<SentenceSpan<AcceptationId>> newSpans) {
        final SymbolArrayId oldSymbolArrayId = getSentenceSymbolArray(sentenceId);
        if (oldSymbolArrayId == null || !checkValidTextAndSpans(newText, newSpans)) {
            return false;
        }

        final ImmutableIntValueMap<SentenceSpan<AcceptationId>> oldSpanMap = getSentenceSpansWithIds(sentenceId);

        final SymbolArrayId foundSymbolArrayId = findSymbolArray(newText);
        final boolean oldSymbolArrayOnlyUsedHere = isSymbolArrayMerelyASentence(oldSymbolArrayId) && findSentencesBySymbolArrayId(oldSymbolArrayId).size() == 1;
        final SymbolArrayId newSymbolArrayId;
        if (foundSymbolArrayId == null) {
            if (oldSymbolArrayOnlyUsedHere) {
                if (!updateSymbolArray(oldSymbolArrayId, newText)) {
                    throw new AssertionError();
                }
                newSymbolArrayId = oldSymbolArrayId;
            }
            else {
                newSymbolArrayId = insertSymbolArray(_db, _symbolArrayIdSetter, newText);
            }
        }
        else {
            if (!equal(foundSymbolArrayId, oldSymbolArrayId) && oldSymbolArrayOnlyUsedHere && !deleteSymbolArray(_db, oldSymbolArrayId)) {
                throw new AssertionError();
            }
            newSymbolArrayId = foundSymbolArrayId;
        }

        if (newSymbolArrayId != oldSymbolArrayId && !updateSentenceSymbolArrayId(sentenceId, newSymbolArrayId)) {
            throw new AssertionError();
        }

        final ImmutableSet<SentenceSpan<AcceptationId>> oldSpans = oldSpanMap.keySet();
        for (SentenceSpan<AcceptationId> span : oldSpans.filterNot(newSpans::contains)) {
            if (!deleteSpan(_db, oldSpanMap.get(span))) {
                throw new AssertionError();
            }
        }

        for (SentenceSpan<AcceptationId> span : newSpans.filterNot(oldSpans::contains)) {
            insertSpan(_db, sentenceId, span.range, span.acceptation);
        }

        return true;
    }
}
