package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.List;
import sword.collections.MutableHashMap;
import sword.collections.MutableMap;
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbValue;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.MorphologyResult;
import sword.langbook3.android.models.SearchResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SetTestUtils.assertEqualSet;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.collections.EqualUtils.equal;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.obtainNewAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.obtainNewConcept;
import static sword.langbook3.android.db.AcceptationsManagerTest.updateAcceptationSimpleCorrelationArray;
import static sword.langbook3.android.db.BunchesManagerTest.addSpanishSingAcceptation;

/**
 * Include all test related to all responsibilities of a AgentsManager.
 *
 * AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> responsibilities include all responsibilities from BunchesManager, and include the following ones:
 * <li>Bunch sets</li>
 * <li>Rules</li>
 * <li>Ruled concepts</li>
 * <li>Ruled acceptations</li>
 * <li>Agents</li>
 */
interface AgentsManagerTest<ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId extends AgentIdInterface> extends BunchesManagerTest<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> {

    @Override
    AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> createManager(MemoryDatabase db);
    ConceptSetter<ConceptId> getConceptIdManager();
    ConceptualizableSetter<ConceptId, AlphabetId> getAlphabetIdManager();
    IntSetter<AcceptationId> getAcceptationIdManager();
    RuleId conceptAsRuleId(ConceptId conceptId);

    static <T> ImmutableSet<T> setOf() {
        return ImmutableHashSet.empty();
    }

    static <T> ImmutableSet<T> setOf(T a) {
        return new ImmutableHashSet.Builder<T>().add(a).build();
    }

    static <T> ImmutableSet<T> setOf(T a, T b) {
        return new ImmutableHashSet.Builder<T>().add(a).add(b).build();
    }

    static <T> ImmutableSet<T> setOf(T a, T b, T c) {
        return new ImmutableHashSet.Builder<T>().add(a).add(b).add(c).build();
    }

    static <T> ImmutableSet<T> setOf(T a, T b, T c, T d) {
        return new ImmutableHashSet.Builder<T>().add(a).add(b).add(c).add(d).build();
    }

    static <AcceptationId> ImmutableMap<AcceptationId, AcceptationId> findRuledAcceptationsByAgent(DbExporter.Database db, IntSetter<AcceptationId> acceptationIdSetter, AgentIdInterface agent) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQueryBuilder(ruledAccs)
                .where(ruledAccs.getAgentColumnIndex(), agent)
                .select(ruledAccs.getIdColumnIndex(), ruledAccs.getAcceptationColumnIndex());

        final MutableMap<AcceptationId, AcceptationId> map = MutableHashMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                map.put(acceptationIdSetter.getKeyFromDbValue(row.get(0)), acceptationIdSetter.getKeyFromDbValue(row.get(1)));
            }
        }

        return map.toImmutable();
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> AgentId addSingleAlphabetAgent(AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
            ImmutableSet<BunchId> diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, RuleId rule) {
        final ImmutableCorrelation<AlphabetId> startMatcher = (startMatcherText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, startMatcherText).build();

        final ImmutableCorrelation<AlphabetId> startAdder = (startAdderText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, startAdderText).build();

        final ImmutableCorrelation<AlphabetId> endMatcher = (endMatcherText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, endMatcherText).build();

        final ImmutableCorrelation<AlphabetId> endAdder = (endAdderText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, endAdderText).build();

        return manager.addAgent(targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> boolean updateSingleAlphabetAgent(AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, AgentId agentId, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
            ImmutableSet<BunchId> diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, RuleId rule) {
        final ImmutableCorrelation<AlphabetId> startMatcher = (startMatcherText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, startMatcherText).build();

        final ImmutableCorrelation<AlphabetId> startAdder = (startAdderText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, startAdderText).build();

        final ImmutableCorrelation<AlphabetId> endMatcher = (endMatcherText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, endMatcherText).build();

        final ImmutableCorrelation<AlphabetId> endAdder = (endAdderText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, endAdderText).build();

        return manager.updateAgent(agentId, targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> void assertOnlyOneMorphology(AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, AcceptationId staticAcceptation, AlphabetId preferredAlphabet, String expectedText, RuleId expectedRule) {
        final MorphologyResult<AcceptationId, RuleId> morphology = getSingleValue(manager.readMorphologiesFromAcceptation(staticAcceptation, preferredAlphabet).morphologies);
        assertEquals(expectedText, morphology.text);
        assertContainsOnly(expectedRule, morphology.rules);
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> void assertNoRuledAcceptationsPresentForChainedAgents(AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, Add3ChainedAgentsResult<AgentId> result) {
        assertEmpty(manager.getAgentProcessedMap(result.agent1Id));
        assertEmpty(manager.getAgentProcessedMap(result.agent2Id));
        assertEmpty(manager.getAgentProcessedMap(result.agent3Id));
    }

    default BunchId obtainNewBunch(BunchesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager, AlphabetId alphabet, String text) {
        return conceptAsBunchId(obtainNewConcept(manager, alphabet, text));
    }

    default RuleId obtainNewRule(AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, AlphabetId alphabet, String text) {
        return conceptAsRuleId(obtainNewConcept(manager, alphabet, text));
    }

    @Test
    default void testAddAgentWhenApplyingRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final BunchId verbBunch = obtainNewBunch(manager, alphabet, "Verbo");
        assertTrue(manager.addAcceptationInBunch(verbBunch, acceptation));

        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerundio");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final ConceptId ruledConcept = manager.findRuledConcept(gerundRule, concept);
        final AcceptationId ruledAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, acceptation);
        assertEquals(ruledConcept, manager.conceptFromAcceptation(ruledAcceptation));

        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(ruledAcceptation));
        assertEquals(acceptation, manager.getStaticAcceptationFromDynamic(ruledAcceptation));
        assertEquals("cantando", manager.readAcceptationMainText(ruledAcceptation));
    }

    @Test
    default void testAddAgentWhenComposingBunch() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId verbBunch = obtainNewBunch(manager, alphabet, "Verbo");
        assertTrue(manager.addAcceptationInBunch(verbBunch, singAcceptation));

        final AcceptationId coughtAcceptation = obtainNewAcceptation(manager, alphabet, "toser");
        assertTrue(manager.addAcceptationInBunch(verbBunch, coughtAcceptation));

        final ConceptId arVerbConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbos acabados en ar");

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ar", null);

        assertContainsOnly(singAcceptation, coughtAcceptation, manager.getAcceptationsInBunch(verbBunch));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(arVerbBunch, agentId));
    }

    @Test
    default void testAddAgentWhenCopyingToTwoBunches() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId verbBunch = obtainNewBunch(manager, alphabet, "Verbos");
        assertTrue(manager.addAcceptationInBunch(verbBunch, singAcceptation));

        final AcceptationId coughtAcceptation = obtainNewAcceptation(manager, alphabet, "toser");
        assertTrue(manager.addAcceptationInBunch(verbBunch, coughtAcceptation));

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "Verbo acabado en ar");
        final BunchId actionBunch = obtainNewBunch(manager, alphabet, "Acción");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch, actionBunch), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ar", null);

        assertContainsOnly(singAcceptation, coughtAcceptation, manager.getAcceptationsInBunch(verbBunch));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(arVerbBunch, agentId));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(actionBunch, agentId));
    }

    default void checkAdd2ChainedAgents(boolean reversedAdditionOrder, boolean addExtraMiddleTargetBunch) {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final ConceptId singConcept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final BunchId verbBunch = obtainNewBunch(manager, alphabet, "Verbo");
        assertTrue(manager.addAcceptationInBunch(verbBunch, acceptation));

        final ImmutableCorrelation<AlphabetId> nullCorrelation = new ImmutableCorrelation.Builder<AlphabetId>().build();
        final ImmutableCorrelation<AlphabetId> matcher = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ar")
                .build();
        final ImmutableCorrelation<AlphabetId> adder = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ando")
                .build();

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "Verbo acabado en ar");
        final BunchId extraBunch = obtainNewBunch(manager, alphabet, "concepto auxiliar");
        final ImmutableSet<BunchId> arVerbBunchSet = setOf(arVerbBunch);
        final ImmutableSet<BunchId> verbBunchSet = setOf(verbBunch);
        final ImmutableSet<BunchId> diffBunches = setOf();
        final ImmutableSet<BunchId> firstTargetBunches = addExtraMiddleTargetBunch? arVerbBunchSet.add(extraBunch) :
                arVerbBunchSet;

        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerundio");
        final AgentId agent2Id;
        if (reversedAdditionOrder) {
            agent2Id = manager.addAgent(setOf(), arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerundRule);
            manager.addAgent(firstTargetBunches, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, null);
        }
        else {
            manager.addAgent(firstTargetBunches, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, null);
            agent2Id = manager.addAgent(setOf(), arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerundRule);
        }

        final ConceptId ruledConcept = manager.findRuledConcept(gerundRule, singConcept);
        final AcceptationId ruledAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, acceptation);
        assertEquals(ruledConcept, manager.conceptFromAcceptation(ruledAcceptation));

        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(ruledAcceptation));
        assertEquals("cantando", manager.readAcceptationMainText(ruledAcceptation));
        assertEquals(acceptation, manager.getStaticAcceptationFromDynamic(acceptation));

        if (addExtraMiddleTargetBunch) {
            assertContainsOnly(acceptation, manager.getAcceptationsInBunch(extraBunch));
        }
        else {
            assertEmpty(manager.getAcceptationsInBunch(extraBunch));
        }
    }

    @Test
    default void testAddAgentWhenAdding2ChainedAgents() {
        checkAdd2ChainedAgents(false, false);
    }

    @Test
    default void testAddAgentWhenAdding2ChainedAgentsReversedAdditionOrder() {
        checkAdd2ChainedAgents(true, false);
    }

    @Test
    default void testAddAgentWhenAdding2ChainedAgentsWithExtraMiddleTargetBunch() {
        checkAdd2ChainedAgents(false, true);
    }

    @Test
    default void testAddAgentWhenAdding2ChainedAgentsReversedAdditionOrderWithExtraMiddleTargetBunch() {
        checkAdd2ChainedAgents(true, true);
    }

    @Test
    default void testAddAcceptationInBunchWhenAddingAcceptationInFirstAgentSourceBunchForChainedAgents() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final ConceptId maleStudentConcept = manager.getNextAvailableConceptId();
        final AcceptationId maleStudentAcc = addSimpleAcceptation(manager, alphabet, maleStudentConcept, "alumno");

        final BunchId pluralableWordsBunch = obtainNewBunch(manager, alphabet, "pluralizable");
        final BunchId feminableWordsBunch = obtainNewBunch(manager, alphabet, "feminizable");
        final RuleId femenineRule = obtainNewRule(manager, alphabet, "femenino");
        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(pluralableWordsBunch), setOf(feminableWordsBunch), setOf(), alphabet, null, null, "o", "a", femenineRule);

        final RuleId pluralRule = obtainNewRule(manager, alphabet, "plural");
        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(pluralableWordsBunch), setOf(), alphabet, null, null, null, "s", pluralRule);

        manager.addAcceptationInBunch(feminableWordsBunch, maleStudentAcc);

        final ConceptId femaleStudentConcept = manager.findRuledConcept(femenineRule, maleStudentConcept);
        final AcceptationId femaleStudentAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, maleStudentAcc);
        assertEquals(femaleStudentConcept, manager.conceptFromAcceptation(femaleStudentAcc));
        assertSinglePair(alphabet, "alumna", manager.getAcceptationTexts(femaleStudentAcc));

        final ConceptId pluralFemaleStudentConcept = manager.findRuledConcept(pluralRule, femaleStudentConcept);
        final AcceptationId pluralFemaleStudentAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, femaleStudentAcc);
        assertEquals(pluralFemaleStudentConcept, manager.conceptFromAcceptation(pluralFemaleStudentAcc));
        assertSinglePair(alphabet, "alumnas", manager.getAcceptationTexts(pluralFemaleStudentAcc));
    }

    default void checkAdd2ChainedAgentsFirstWithoutSource(boolean reversedAdditionOrder, boolean acceptationBeforeAgents) {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        AcceptationId acceptation = null;
        if (acceptationBeforeAgents) {
            final ConceptId songConcept = manager.getNextAvailableConceptId();
            acceptation = addSimpleAcceptation(manager, alphabet, songConcept, "canción");
        }

        final ImmutableCorrelation<AlphabetId> nullCorrelation = new ImmutableCorrelation.Builder<AlphabetId>().build();
        final ImmutableCorrelation<AlphabetId> matcher = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ón")
                .build();
        final ImmutableCorrelation<AlphabetId> adder = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ones")
                .build();

        final BunchId bunchBunch = obtainNewBunch(manager, alphabet, "pluralizable sustituyendo ón por ones");
        final ImmutableSet<BunchId> middleBunchSet = setOf(bunchBunch);
        final ImmutableSet<BunchId> noBunchSet = setOf();

        final RuleId pluralRule;
        if (reversedAdditionOrder) {
            pluralRule = obtainNewRule(manager, alphabet, "plural");
            assertNotNull(manager.addAgent(setOf(), middleBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, adder, pluralRule));
            assertNotNull(manager.addAgent(middleBunchSet, noBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, matcher, null));
        }
        else {
            assertNotNull(manager.addAgent(middleBunchSet, noBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, matcher, null));
            pluralRule = obtainNewRule(manager, alphabet, "plural");
            assertNotNull(manager.addAgent(setOf(), middleBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, adder, pluralRule));
        }

        if (!acceptationBeforeAgents) {
            final ConceptId songConcept = manager.getNextAvailableConceptId();
            acceptation = addSimpleAcceptation(manager, alphabet, songConcept, "canción");
        }

        assertOnlyOneMorphology(manager, acceptation, alphabet, "canciones", pluralRule);
    }

    @Test
    default void testAddAcceptationWhen2ChainedAgentsFirstWithoutSourceBeforeMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(false, false);
    }

    @Test
    default void testAddAcceptationWhen2ChainedAgentsFirstWithoutSourceReversedAdditionOrderBeforeMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(true, false);
    }

    @Test
    default void testAddAgentWhenAdding2ChainedAgentsFirstWithoutSourceAfterMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(false, true);
    }

    @Test
    default void testAddAgentWhenAdding2ChainedAgentsFirstWithoutSourceReversedAdditionOrderAfterMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(true, true);
    }

    default void checkAddAgentWithDiffBunch(boolean addAgentBeforeAcceptations) {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final BunchId arEndingNounBunch = obtainNewBunch(manager, alphabet, "arNoun");
        final ImmutableSet<BunchId> sourceBunches = setOf();
        final ImmutableSet<BunchId> diffBunches = setOf(arEndingNounBunch);

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId palateAcceptation;
        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "arVerb");
        final AgentId agentId;
        if (addAgentBeforeAcceptations) {
            agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", null);
            palateAcceptation = obtainNewAcceptation(manager, alphabet, "paladar");
            manager.addAcceptationInBunch(arEndingNounBunch, palateAcceptation);
        }
        else {
            palateAcceptation = obtainNewAcceptation(manager, alphabet, "paladar");
            manager.addAcceptationInBunch(arEndingNounBunch, palateAcceptation);
            agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", null);
        }

        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunch(arVerbBunch));
        assertContainsOnly(agentId, manager.findAllAgentsThatIncludedAcceptationInBunch(arVerbBunch, singAcceptation));
    }

    @Test
    default void testAddAgentWhenAddingAcceptationBeforeAgentWithDiffBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final BunchId arEndingNounBunch = obtainNewBunch(manager, alphabet, "arNoun");
        final ImmutableSet<BunchId> sourceBunches = setOf();
        final ImmutableSet<BunchId> diffBunches = setOf(arEndingNounBunch);

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "arVerb");

        final AcceptationId palateAcceptation = obtainNewAcceptation(manager, alphabet, "paladar");
        manager.addAcceptationInBunch(arEndingNounBunch, palateAcceptation);
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", null);

        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunch(arVerbBunch));
        assertContainsOnly(agentId, manager.findAllAgentsThatIncludedAcceptationInBunch(arVerbBunch, singAcceptation));
    }

    @Test
    default void testAddAcceptationInBunchWhenAddingAcceptationAfterAgentWithDiffBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final BunchId arEndingNounBunch = obtainNewBunch(manager, alphabet, "arNoun");
        final ImmutableSet<BunchId> sourceBunches = setOf();
        final ImmutableSet<BunchId> diffBunches = setOf(arEndingNounBunch);

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "arVerb");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", null);
        final AcceptationId palateAcceptation = obtainNewAcceptation(manager, alphabet, "paladar");
        assertTrue(manager.addAcceptationInBunch(arEndingNounBunch, palateAcceptation));

        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunch(arVerbBunch));
        assertContainsOnly(agentId, manager.findAllAgentsThatIncludedAcceptationInBunch(arVerbBunch, singAcceptation));
    }

    class Add3ChainedAgentsResult<AgentId> {
        final AgentId agent1Id;
        final AgentId agent2Id;
        final AgentId agent3Id;

        Add3ChainedAgentsResult(AgentId agent1Id, AgentId agent2Id, AgentId agent3Id) {
            this.agent1Id = agent1Id;
            this.agent2Id = agent2Id;
            this.agent3Id = agent3Id;
        }
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> Add3ChainedAgentsResult<AgentId> add3ChainedAgents(
            AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager,
            AlphabetId alphabet, ImmutableSet<BunchId> sourceBunchSet, BunchId arVerbBunch, BunchId actionBunch,
            RuleId nominalizationRule, RuleId pluralRule) {

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agent3Id = addSingleAlphabetAgent(manager, setOf(), setOf(actionBunch), noBunches, alphabet, null, null, null, "s", pluralRule);
        final AgentId agent2Id = addSingleAlphabetAgent(manager, setOf(actionBunch), setOf(arVerbBunch), noBunches, alphabet, null, null, "ar", "o", nominalizationRule);
        final AgentId agent1Id = addSingleAlphabetAgent(manager, setOf(arVerbBunch), sourceBunchSet, noBunches, alphabet, null, null, "ar", "ar", null);

        return new Add3ChainedAgentsResult<>(agent1Id, agent2Id, agent3Id);
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> Add3ChainedAgentsResult<AgentId> add3ChainedAgents(AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager,
            AlphabetId alphabet, BunchId arVerbBunch, BunchId actionBunch,
            RuleId nominalizationRule, RuleId pluralRule) {

        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        return add3ChainedAgents(manager, alphabet, noBunches, arVerbBunch, actionBunch, nominalizationRule, pluralRule);
    }

    @Test
    default void testAddAgentWhenAdding3ChainedAgents() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId singConcept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "arVerb");
        final BunchId actionBunch = obtainNewBunch(manager, alphabet, "action");
        final RuleId nominalizationRule = obtainNewRule(manager, alphabet, "nominalization");
        final RuleId pluralRule = obtainNewRule(manager, alphabet, "plural");
        final Add3ChainedAgentsResult<AgentId> addAgentsResult = add3ChainedAgents(manager, alphabet,
                arVerbBunch, actionBunch, nominalizationRule, pluralRule);

        final ImmutableMap<ConceptId, ConceptId> nominalizationRuledConcepts = manager.findRuledConceptsByRule(nominalizationRule);
        assertContainsOnly(singConcept, nominalizationRuledConcepts);
        final ConceptId nounRuledConcept = nominalizationRuledConcepts.keyAt(0);

        final ImmutableMap<ConceptId, ConceptId> pluralRuledConcepts = manager.findRuledConceptsByRule(pluralRule);
        assertContainsOnly(nounRuledConcept, pluralRuledConcepts);
        final ConceptId pluralRuledConcept = pluralRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = manager.getAgentProcessedMap(addAgentsResult.agent2Id);
        assertContainsOnly(acceptation, processedMap.keySet());
        final AcceptationId nounRuledAcceptation = processedMap.valueAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> pluralProcessedMap = manager.getAgentProcessedMap(addAgentsResult.agent3Id);
        assertContainsOnly(nounRuledAcceptation, pluralProcessedMap.keySet());
        final AcceptationId pluralRuledAcceptation = pluralProcessedMap.valueAt(0);

        assertEquals(nounRuledConcept, manager.conceptFromAcceptation(nounRuledAcceptation));
        assertEquals(pluralRuledConcept, manager.conceptFromAcceptation(pluralRuledAcceptation));

        assertSinglePair(alphabet, "canto", manager.getAcceptationTexts(nounRuledAcceptation));
        assertEquals("canto", manager.readAcceptationMainText(nounRuledAcceptation));
        assertEquals(acceptation, manager.getStaticAcceptationFromDynamic(nounRuledAcceptation));

        assertSinglePair(alphabet, "cantos", manager.getAcceptationTexts(pluralRuledAcceptation));
        assertEquals("cantos", manager.readAcceptationMainText(pluralRuledAcceptation));
        assertEquals(acceptation, manager.getStaticAcceptationFromDynamic(pluralRuledAcceptation));
    }

    @Test
    default void testRemoveAgentRemovesDynamicAcceptations() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AcceptationId studentAcceptation = obtainNewAcceptation(manager, alphabet, "alumno");

        final RuleId femenineRule = obtainNewRule(manager, alphabet, "femenino");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(), setOf(), setOf(), alphabet, null, null, "o", "a", femenineRule);

        final AcceptationId femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
        final ImmutableList<CorrelationId> correlationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        manager.removeAgent(agentId);

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(femaleStudentAcceptation));
        assertNull(manager.conceptFromAcceptation(femaleStudentAcceptation));
        for (CorrelationId correlationId : correlationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }
    }

    @Test
    default void testRemoveAcceptationRemovesDynamicAcceptationsWhenAcceptationFromSourceBunch() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AcceptationId studentAcceptation = obtainNewAcceptation(manager, alphabet, "alumno");

        final BunchId sourceBunch = obtainNewBunch(manager, alphabet, "mis palabras");
        manager.addAcceptationInBunch(sourceBunch, studentAcceptation);

        final RuleId femenineRule = obtainNewRule(manager, alphabet, "femenino");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(), setOf(sourceBunch), setOf(), alphabet, null, null, "o", "a", femenineRule);

        final AcceptationId femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
        final ImmutableList<CorrelationId> studentCorrelationArray = manager.getAcceptationCorrelationArray(studentAcceptation);
        final ImmutableList<CorrelationId> femaleStudentCorrelationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        assertTrue(manager.removeAcceptation(studentAcceptation));

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(femaleStudentAcceptation));
        assertNull(manager.conceptFromAcceptation(studentAcceptation));
        assertNull(manager.conceptFromAcceptation(femaleStudentAcceptation));
        for (CorrelationId correlationId : studentCorrelationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }

        assertSize(2, femaleStudentCorrelationArray);
        assertEmpty(manager.getCorrelationWithText(femaleStudentCorrelationArray.valueAt(0)));
        assertFalse(manager.getCorrelationWithText(femaleStudentCorrelationArray.valueAt(1)).isEmpty());
    }

    @Test
    default void testRemoveAgentRemovesUnusedBunchSets() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AcceptationId studentAcceptation = obtainNewAcceptation(manager, alphabet, "alumno");
        final RuleId femenineRule = obtainNewRule(manager, alphabet, "femenino");
        final BunchId targetBunch = obtainNewBunch(manager, alphabet, "destino");
        final BunchId sourceBunch = obtainNewBunch(manager, alphabet, "origen");
        manager.addAcceptationInBunch(sourceBunch, studentAcceptation);

        final BunchId diffBunch = obtainNewBunch(manager, alphabet, "diferencial");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(targetBunch), setOf(sourceBunch), setOf(diffBunch), alphabet, null, null, "o", "a", femenineRule);

        final AcceptationId femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
        final ImmutableList<CorrelationId> correlationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        final AgentRegister<CorrelationId, BunchSetId, RuleId> agentRegister = manager.getAgentRegister(agentId);
        manager.removeAgent(agentId);

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(femaleStudentAcceptation));
        assertNull(manager.conceptFromAcceptation(femaleStudentAcceptation));
        for (CorrelationId correlationId : correlationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }

        assertEmpty(manager.getBunchSet(agentRegister.targetBunchSetId));
        assertEmpty(manager.getBunchSet(agentRegister.sourceBunchSetId));
        assertEmpty(manager.getBunchSet(agentRegister.diffBunchSetId));
    }

    @Test
    default void testRemoveAgentWhenRemovingChainedAgent() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId singConcept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "arVerb");
        final BunchId actionBunch = obtainNewBunch(manager, alphabet, "action");
        final RuleId nominalizationRule = obtainNewRule(manager, alphabet, "nominalization");
        final RuleId pluralRule = obtainNewRule(manager, alphabet, "plural");
        final Add3ChainedAgentsResult<AgentId> addAgentsResult = add3ChainedAgents(manager, alphabet,
                arVerbBunch, actionBunch, nominalizationRule, pluralRule);

        final AcceptationId nounAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent2Id, acceptation);
        final AcceptationId pluralAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent3Id, nounAcceptation);
        assertNotEquals(0, pluralAcceptation);

        manager.removeAgent(addAgentsResult.agent1Id);
        assertNoRuledAcceptationsPresentForChainedAgents(manager, addAgentsResult);

        assertEquals(singConcept, manager.conceptFromAcceptation(acceptation));
        assertNull(manager.conceptFromAcceptation(nounAcceptation));
        assertNull(manager.conceptFromAcceptation(pluralAcceptation));

        assertEmpty(manager.getAcceptationsInBunch(arVerbBunch));
        assertEmpty(manager.getAcceptationsInBunch(actionBunch));
    }

    @Test
    default void testRemoveAcceptationWithChainedAgent() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId singConcept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "arVerb");
        final BunchId actionBunch = obtainNewBunch(manager, alphabet, "action");
        final RuleId nominalizationRule = obtainNewRule(manager, alphabet, "nominalization");
        final RuleId pluralRule = obtainNewRule(manager, alphabet, "plural");
        final Add3ChainedAgentsResult<AgentId> addAgentsResult = add3ChainedAgents(manager, alphabet, arVerbBunch, actionBunch, nominalizationRule, pluralRule);

        final AcceptationId nounAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent2Id, acceptation);
        final AcceptationId pluralAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent3Id, nounAcceptation);
        assertNotNull(pluralAcceptation);

        manager.removeAcceptation(acceptation);
        assertNoRuledAcceptationsPresentForChainedAgents(manager, addAgentsResult);

        assertNull(manager.conceptFromAcceptation(acceptation));
        assertNull(manager.conceptFromAcceptation(nounAcceptation));
        assertNull(manager.conceptFromAcceptation(pluralAcceptation));

        assertEmpty(manager.getAcceptationsInBunch(arVerbBunch));
        assertEmpty(manager.getAcceptationsInBunch(actionBunch));
    }

    @Test
    default void testRemoveAcceptationFromBunchWithBunchChainedAgent() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId singConcept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final BunchId verbBunch = obtainNewBunch(manager, alphabet, "verb");
        manager.addAcceptationInBunch(verbBunch, acceptation);

        final ImmutableSet<BunchId> sourceBunches = new ImmutableHashSet.Builder<BunchId>().add(verbBunch).build();
        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "arVerb");
        final BunchId actionBunch = obtainNewBunch(manager, alphabet, "action");
        final RuleId nominalizationRule = obtainNewRule(manager, alphabet, "nominalization");
        final RuleId pluralRule = obtainNewRule(manager, alphabet, "plural");
        final Add3ChainedAgentsResult<AgentId> addAgentsResult = add3ChainedAgents(manager, alphabet, sourceBunches, arVerbBunch, actionBunch, nominalizationRule, pluralRule);

        final AcceptationId nounAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent2Id, acceptation);
        final AcceptationId pluralAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent3Id, nounAcceptation);
        assertNotNull(pluralAcceptation);

        manager.removeAcceptationFromBunch(verbBunch, acceptation);
        assertNoRuledAcceptationsPresentForChainedAgents(manager, addAgentsResult);

        assertEquals(singConcept, manager.conceptFromAcceptation(acceptation));
        assertNull(manager.conceptFromAcceptation(nounAcceptation));
        assertNull(manager.conceptFromAcceptation(pluralAcceptation));

        assertEmpty(manager.getAcceptationsInBunch(verbBunch));
        assertEmpty(manager.getAcceptationsInBunch(arVerbBunch));
        assertEmpty(manager.getAcceptationsInBunch(actionBunch));
    }

    @Test
    default void testReadAllMatchingBunchesForSingleMatching() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final ImmutableSet<BunchId> diffBunches = setOf();
        final BunchId verbArBunch = obtainNewBunch(manager, alphabet, "verbo ar");
        final BunchId verbErBunch = obtainNewBunch(manager, alphabet, "verbo er");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");
        addSingleAlphabetAgent(manager, setOf(), setOf(verbArBunch), diffBunches, alphabet, null, null, "ar", "ando", gerundRule);

        addSingleAlphabetAgent(manager, setOf(), setOf(verbErBunch), diffBunches, alphabet, null, null, "er", "iendo", gerundRule);

        ImmutableCorrelation<AlphabetId> texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "provocar").build();
        assertSinglePair(verbArBunch, "verbo ar", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "comer").build();
        assertSinglePair(verbErBunch, "verbo er", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "dormir").build();
        assertEmpty(manager.readAllMatchingBunches(texts, alphabet));
    }

    @Test
    default void testReadAllMatchingBunchesForMultipleMatching() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final BunchId verbErBunch = obtainNewBunch(manager, alphabet, "verbo er");
        final BunchId sustantivableBunch = obtainNewBunch(manager, alphabet, "sustantivable");
        final BunchId nounBunch = obtainNewBunch(manager, alphabet, "noun");

        final ImmutableSet<BunchId> diffBunches = setOf();
        final BunchId verbArBunch = obtainNewBunch(manager, alphabet, "verbo ar");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");
        addSingleAlphabetAgent(manager, setOf(), setOf(verbArBunch), diffBunches, alphabet, null, null, "ar", "ando", gerundRule);
        addSingleAlphabetAgent(manager, setOf(), setOf(verbErBunch), diffBunches, alphabet, null, null, "er", "iendo", gerundRule);
        addSingleAlphabetAgent(manager, setOf(nounBunch), setOf(sustantivableBunch), diffBunches, alphabet, null, null, "ar", "ación", gerundRule);

        ImmutableCorrelation<AlphabetId> texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "provocar").build();
        final ImmutableMap<BunchId, String> bunches = manager.readAllMatchingBunches(texts, alphabet);
        assertContainsOnly(verbArBunch, sustantivableBunch, bunches.keySet());
        assertEquals("verbo ar", bunches.get(verbArBunch));
        assertEquals("sustantivable", bunches.get(sustantivableBunch));

        texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "comer").build();
        assertSinglePair(verbErBunch, "verbo er", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "dormir").build();
        assertEmpty(manager.readAllMatchingBunches(texts, alphabet));
    }

    final class SingleAlphabetCorrelationComposer<AlphabetId> {
        final AlphabetId alphabet;

        SingleAlphabetCorrelationComposer(AlphabetId alphabet) {
            this.alphabet = alphabet;
        }

        ImmutableCorrelation<AlphabetId> compose(String text) {
            return new ImmutableCorrelation.Builder<AlphabetId>()
                    .put(alphabet, text)
                    .build();
        }
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayForAcceptationWithRuleAgent() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptationId = addSimpleAcceptation(manager, alphabet, concept, "contar");

        final BunchId firstConjugationVerbBunch = obtainNewBunch(manager, alphabet, "firstConjugationVerb");
        manager.addAcceptationInBunch(firstConjugationVerbBunch, acceptationId);

        final SingleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new SingleAlphabetCorrelationComposer<>(alphabet);
        final ImmutableCorrelation<AlphabetId> arCorrelation = correlationComposer.compose("ar");
        final ImmutableCorrelation<AlphabetId> andoCorrelation = correlationComposer.compose("ando");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();

        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");
        manager.addAgent(setOf(), setOf(firstConjugationVerbBunch), setOf(), emptyCorrelation, emptyCorrelation, arCorrelation, andoCorrelation, gerundRule);

        updateAcceptationSimpleCorrelationArray(manager, alphabet, acceptationId, "cantar");

        final ConceptId ruledConcept = manager.findRuledConcept(gerundRule, concept);
        assertNotEquals(concept, ruledConcept);

        final AcceptationId ruledAcceptation = getSingleValue(manager.findAcceptationsByConcept(ruledConcept));
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(ruledAcceptation).toImmutable());
        assertEquals("cantando", manager.readAcceptationMainText(ruledAcceptation));
        assertEquals(acceptationId, manager.getStaticAcceptationFromDynamic(ruledAcceptation));

        final ImmutableCorrelation<AlphabetId> cantCorrelation = correlationComposer.compose("cant");
        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(ruledAcceptation);
        assertSize(2, correlationIds);
        assertEquals(cantCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(andoCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
    }

    @Test
    default void testRemoveAcceptationWhenUnableToRemoveAcceptationsDueToTheyAreUniqueAgentSourceOrTargetBunch() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final ConceptId verbConcept = manager.getNextAvailableConceptId();
        final AcceptationId verbAcc = addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");

        final ConceptId firstConjugationVerbConcept = manager.getNextAvailableConceptId();
        final AcceptationId firstConjugationVerbAcc = addSimpleAcceptation(manager, alphabet, firstConjugationVerbConcept, "verbo ar");
        final AcceptationId singAcc = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        assertTrue(manager.addAcceptationInBunch(verbBunch, singAcc));

        final BunchId firstConjugationVerbBunch = conceptAsBunchId(firstConjugationVerbConcept);
        assertNotEquals(null, addSingleAlphabetAgent(manager, setOf(firstConjugationVerbBunch), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ar", null));

        assertFalse(manager.removeAcceptation(verbAcc));
        assertFalse(manager.removeAcceptation(firstConjugationVerbAcc));

        assertEquals("verbo", manager.getAcceptationTexts(verbAcc).get(alphabet));
        assertEquals("verbo ar", manager.getAcceptationTexts(firstConjugationVerbAcc).get(alphabet));
    }

    @Test
    default void testAddAgentWhenMultipleAgentsTargetingSameBunch() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId verbAcc = obtainNewAcceptation(manager, alphabet, "desconfiar");

        final ImmutableSet<BunchId> emptyBunchSet = setOf();
        final BunchId myBunch = obtainNewBunch(manager, alphabet, "palabaras raras");
        final AgentId desAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, null);
        final AgentId arAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", null);

        assertContainsOnly(desAgent, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc));
    }

    @Test
    default void testAddAgentWhenAddingAcceptationInBunchBefore() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId bedAcc = obtainNewAcceptation(manager, alphabet, "cama");
        final AcceptationId verbAcc1 = obtainNewAcceptation(manager, alphabet, "confiar");
        final AcceptationId verbAcc2 = obtainNewAcceptation(manager, alphabet, "desconfiar");

        final BunchId myBunch = obtainNewBunch(manager, alphabet, "palabras raras");
        assertTrue(manager.addAcceptationInBunch(myBunch, bedAcc));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc1));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc2));

        final ImmutableSet<BunchId> emptyBunchSet = setOf();
        final AgentId desAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, null);
        final AgentId arAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", null);

        assertContainsOnly(null, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, bedAcc));
        assertContainsOnly(null, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc1));
        assertContainsOnly(null, desAgent, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc2));
    }

    @Test
    default void testAddAcceptationInBunchWhenAddingAgentBefore() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId bedAcc = obtainNewAcceptation(manager, alphabet, "cama");
        final AcceptationId verbAcc1 = obtainNewAcceptation(manager, alphabet, "confiar");
        final AcceptationId verbAcc2 = obtainNewAcceptation(manager, alphabet, "desconfiar");

        final ImmutableSet<BunchId> emptyBunchSet = setOf();
        final BunchId myBunch = obtainNewBunch(manager, alphabet, "palabras raras");
        final AgentId desAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, null);
        final AgentId arAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", null);

        assertTrue(manager.addAcceptationInBunch(myBunch, bedAcc));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc1));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc2));

        assertContainsOnly(null, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, bedAcc));
        assertContainsOnly(null, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc1));
        assertContainsOnly(null, desAgent, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc2));
    }

    @Test
    default void testUpdateAgentWhenUpdatingAgentTargetForNoChainedAgentWithoutRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "verbo de primera conjugación");
        final BunchId erVerbBunch = obtainNewBunch(manager, alphabet, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(arVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
    }

    @Test
    default void testUpdateAgentWhenIncludingExtraTargetForNoChainedAgentWithoutRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "verbo de primera conjugación");
        final BunchId erVerbBunch = obtainNewBunch(manager, alphabet, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch, erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(arVerbBunch, erVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
    }

    @Test
    default void testUpdateAgentWhenRemovingExtraTargetForNoChainedAgentWithoutRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "verbo de primera conjugación");
        final BunchId erVerbBunch = obtainNewBunch(manager, alphabet, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch, erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(arVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
    }

    @Test
    default void testUpdateAgentWhenIncludingExtraTargetForNoChainedAgentWithRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");
        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "verbo de primera conjugación");
        final BunchId erVerbBunch = obtainNewBunch(manager, alphabet, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch, erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));

        assertEmpty(manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
        assertContainsOnly(arVerbBunch, erVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(dynamicAcceptation));
    }

    @Test
    default void testUpdateAgentWhenRemovingExtraTargetForNoChainedAgentWithRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");
        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "verbo de primera conjugación");
        final BunchId erVerbBunch = obtainNewBunch(manager, alphabet, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch, erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));

        assertEmpty(manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
        assertContainsOnly(arVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(dynamicAcceptation));
    }

    @Test
    default void testUpdateAgentWhenUpdatingAgentTargetForChainedAgentWithoutRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "verbo de primera conjugación");
        final BunchId erVerbBunch = obtainNewBunch(manager, alphabet, "verbo de segunda conjugación");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerundio");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agent1Id = addSingleAlphabetAgent(manager, setOf(erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final AgentId agent2Id = addSingleAlphabetAgent(manager, setOf(), setOf(arVerbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule);
        assertTrue(updateSingleAlphabetAgent(manager, agent1Id, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));
    }

    @Test
    default void testUpdateAgentWhenRemovingAgentTargetFromSecondChainedAgent() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "verbo de primera conjugación");
        final BunchId recentWordsBunch = obtainNewBunch(manager, alphabet, "palabras recientes");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerundio");

        final ImmutableSet<BunchId> noBunches = setOf();
        addSingleAlphabetAgent(manager, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final AgentId agent2Id = addSingleAlphabetAgent(manager, setOf(recentWordsBunch), setOf(arVerbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule);
        assertTrue(updateSingleAlphabetAgent(manager, agent2Id, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));
        assertEmpty(manager.getAcceptationsInBunch(recentWordsBunch));
    }

    @Test
    default void testUpdateAgentWhenIncludingAgentTargetToSecondChainedAgent() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "verbo de primera conjugación");
        final BunchId recentWordsBunch = obtainNewBunch(manager, alphabet, "palabras recientes");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerundio");

        final ImmutableSet<BunchId> noBunches = setOf();
        addSingleAlphabetAgent(manager, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final AgentId agent2Id = addSingleAlphabetAgent(manager, setOf(), setOf(arVerbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agent2Id, setOf(recentWordsBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));
        assertContainsOnly(dynamicAcceptation, manager.getAcceptationsInBunch(recentWordsBunch));
    }

    @Test
    default void testUpdateAgentWhenIncludingAgentSourceBunches() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        obtainNewAcceptation(manager, alphabet, "tocar");

        final BunchId chapter1Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final BunchId allVocabularyBunch = obtainNewBunch(manager, alphabet, "vocabulario a repasar");
        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), setOf(chapter1Bunch), noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenRemovingAgentSourceBunches() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId touchAcceptation = obtainNewAcceptation(manager, alphabet, "tocar");

        final BunchId chapter1Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final BunchId allVocabularyBunch = obtainNewBunch(manager, alphabet, "vocabulario");
        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), setOf(chapter1Bunch), noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        assertContainsOnly(singAcceptation, touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenChangingAgentSourceBunches() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId touchAcceptation = obtainNewAcceptation(manager, alphabet, "tocar");

        final BunchId chapter1Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final BunchId chapter2Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final BunchId allVocabularyBunch = obtainNewBunch(manager, alphabet, "vocabulario a repasar");
        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), setOf(chapter1Bunch), noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), setOf(chapter2Bunch), noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenIncludingExtraSourceBunch() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId touchAcceptation = obtainNewAcceptation(manager, alphabet, "tocar");

        obtainNewAcceptation(manager, alphabet, "pasar");

        final BunchId chapter1Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final BunchId chapter2Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final BunchId allVocabularyBunch = obtainNewBunch(manager, alphabet, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), setOf(chapter1Bunch), noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), setOf(chapter1Bunch, chapter2Bunch), noBunches, alphabet, null, null, "ar", "ar", null));

        final ImmutableSet<AcceptationId> expectedAcceptations = setOf(singAcceptation, touchAcceptation);
        assertEqualSet(expectedAcceptations, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenRemovingOneSourceBunch() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId touchAcceptation = obtainNewAcceptation(manager, alphabet, "tocar");
        obtainNewAcceptation(manager, alphabet, "pasar");

        final BunchId chapter1Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final BunchId chapter2Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final BunchId allVocabularyBunch = obtainNewBunch(manager, alphabet, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), setOf(chapter1Bunch, chapter2Bunch), noBunches, alphabet, null, null, "ar", "ar", null);

        final ImmutableSet<BunchId> chapter1Only = new ImmutableHashSet.Builder<BunchId>().add(chapter1Bunch).build();
        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter1Only, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenIncludingAgentDiffBunchMatchingSource() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId touchAcceptation = obtainNewAcceptation(manager, alphabet, "tocar");

        final BunchId chapter1Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final BunchId chapter2Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, singAcceptation));
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final BunchId allVocabularyBunch = obtainNewBunch(manager, alphabet, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableSet<BunchId> chapter2Only = setOf(chapter2Bunch);

        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter2Only, setOf(chapter1Bunch), alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenRemovingAgentDiffBunchMatchingSource() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId touchAcceptation = obtainNewAcceptation(manager, alphabet, "tocar");

        final BunchId chapter1Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final BunchId chapter2Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, singAcceptation));
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final BunchId allVocabularyBunch = obtainNewBunch(manager, alphabet, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableSet<BunchId> chapter2Only = setOf(chapter2Bunch);
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), chapter2Only, setOf(chapter1Bunch), alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", null));

        final ImmutableSet<AcceptationId> expectedAcceptations = setOf(singAcceptation, touchAcceptation);
        assertEqualSet(expectedAcceptations, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenIncludingAgentDiffBunchNoMatchingSource() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId touchAcceptation = obtainNewAcceptation(manager, alphabet, "tocar");

        final BunchId chapter1Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final BunchId chapter2Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final BunchId allVocabularyBunch = obtainNewBunch(manager, alphabet, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableSet<BunchId> chapter2Only = setOf(chapter2Bunch);

        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter2Only, setOf(chapter1Bunch), alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenRemovingAgentDiffBunchNoMatchingSource() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId touchAcceptation = obtainNewAcceptation(manager, alphabet, "tocar");

        final BunchId chapter1Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final BunchId chapter2Bunch = obtainNewBunch(manager, alphabet, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableSet<BunchId> chapter1Only = setOf(chapter1Bunch);
        final ImmutableSet<BunchId> chapter2Only = setOf(chapter2Bunch);

        final BunchId allVocabularyBunch = obtainNewBunch(manager, alphabet, "vocabulario a repasar");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), chapter2Only, chapter1Only, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenChangingAgentEndMatcherAndAdder() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        obtainNewAcceptation(manager, alphabet, "comer");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId arVerbBunch = obtainNewBunch(manager, alphabet, "verbo de primera conjugación");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "er", "er", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(arVerbBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenChangingAgentStartMatcherAndAdder() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        obtainNewAcceptation(manager, alphabet, "confiar");
        final AcceptationId untrustAcceptation = obtainNewAcceptation(manager, alphabet, "desconfiar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId unVerbBunch = obtainNewBunch(manager, alphabet, "verbo que comienza por des");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(unVerbBunch), noBunches, noBunches, alphabet, "con", "con", null, null, null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(unVerbBunch), noBunches, noBunches, alphabet, "des", "des", null, null, null));
        assertContainsOnly(untrustAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(unVerbBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenChangingRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final RuleId pastRule = obtainNewRule(manager, alphabet, "pasado");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", pastRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testUpdateAgentWhenChangingAdder() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "aba", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testUpdateAgentWhenChangingAdderAffectingMultipleAcceptations() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final AcceptationId cryAcceptation = obtainNewAcceptation(manager, alphabet, "llorar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "aba", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
        assertOnlyOneMorphology(manager, cryAcceptation, alphabet, "llorando", gerundRule);
    }

    @Test
    default void testUpdateAgentWhenChangingAdderAndRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final RuleId pastRule = obtainNewRule(manager, alphabet, "pasado");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "aba", pastRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testUpdateAgentWhenAddingAdderAndRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final BunchId myTargetBunch = obtainNewBunch(manager, alphabet, "mi lista");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final MorphologyResult<AcceptationId, RuleId> morphology = getSingleValue(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertEquals("cantando", morphology.text);
        assertContainsOnly(gerundRule, morphology.rules);
        assertContainsOnly(morphology.dynamicAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
    }

    @Test
    default void testUpdateAgentWhenAddingAdderAndRuleForMultipleTargetBunches() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId myTargetBunch = obtainNewBunch(manager, alphabet, "mi lista");
        final BunchId myTargetBunch2 = obtainNewBunch(manager, alphabet, "mi otra lista");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final MorphologyResult<AcceptationId, RuleId> morphology = getSingleValue(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertEquals("cantando", morphology.text);
        assertContainsOnly(gerundRule, morphology.rules);
        assertContainsOnly(morphology.dynamicAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
        assertContainsOnly(morphology.dynamicAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch2, agentId));
    }

    @Test
    default void testUpdateAgentWhenRemovingAdderAndRule() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final BunchId myTargetBunch = obtainNewBunch(manager, alphabet, "mi lista");
        final BunchId myTargetBunch2 = obtainNewBunch(manager, alphabet, "mi otra lista");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch2, agentId));
    }

    @Test
    default void testUpdateAgentWhenRemovingAdderAndRuleForMultipleTargetBunches() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final BunchId myTargetBunch = obtainNewBunch(manager, alphabet, "mi lista");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayWhenAcceptationWasMatchingAgentBeforeAndNotAfter() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        assertNotNull(addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar (sin instrumentos)"));
        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayWhenAcceptationIsMatchingAgentAfterButNotBefore() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar (sin instrumentos)");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        assertNotNull(addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar"));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayWhenMatchingChainedAgentBefore() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");
        final BunchId verbBunch = obtainNewBunch(manager, alphabet, "verbo");

        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        assertNotNull(addSingleAlphabetAgent(manager, setOf(verbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        assertNotNull(addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar (sin instrumentos)"));
        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayWhenMatchingChainedAgentAfter() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar (sin instrumentos)");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerund");
        final BunchId verbBunch = obtainNewBunch(manager, alphabet, "verbo");

        final ImmutableSet<BunchId> noBunches = setOf();
        assertNotNull(addSingleAlphabetAgent(manager, setOf(verbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));
        assertNotNull(addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar"));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testAddAcceptationInBunchWhenIncludingMatchingAcceptationInAgentSourceBunchWithJustEndAdderForAcceptationFromOtherLanguage() {
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(new MemoryDatabase());
        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final AcceptationId singAcceptation = obtainNewAcceptation(manager, esAlphabet, "cantar");

        final BunchId myBunch = obtainNewBunch(manager, esAlphabet, "palabras");
        manager.addAcceptationInBunch(myBunch, singAcceptation);

        final RuleId verbalitationRule = obtainNewRule(manager, esAlphabet, "verbalización");

        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(), setOf(myBunch), setOf(), jaAlphabet, null, null, null, "する", verbalitationRule);
        assertEmpty(manager.getAgentProcessedMap(agentId));

        final AcceptationId studyAcceptation = obtainNewAcceptation(manager, jaAlphabet, "べんきょう");
        manager.addAcceptationInBunch(myBunch, studyAcceptation);
        assertContainsOnly(studyAcceptation, manager.getAgentProcessedMap(agentId).keySet());
    }

    @Test
    default void testShareConceptAvoidsDuplicatedBunchSets() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId guyConcept = manager.getNextAvailableConceptId();
        final AcceptationId guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");
        final ConceptId personConcept = obtainNewConcept(manager, alphabet, "persona");

        final BunchId targetBunch1 = obtainNewBunch(manager, alphabet, "mis palabras 1");
        final BunchId targetBunch2 = obtainNewBunch(manager, alphabet, "mis palabras 2");

        final BunchId guyBunch = conceptAsBunchId(guyConcept);
        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(targetBunch1), setOf(guyBunch), setOf(), alphabet, null, null, null, null, null);

        final BunchId personBunch = conceptAsBunchId(personConcept);
        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(targetBunch2), setOf(personBunch), setOf(), alphabet, null, null, null, null, null);

        final BunchSetId oldSetId = manager.getAgentRegister(agent2).sourceBunchSetId;
        assertTrue(manager.shareConcept(guyAcc, personConcept));

        final BunchSetId setId = manager.getAgentRegister(agent1).sourceBunchSetId;
        assertEquals(setId, manager.getAgentRegister(agent2).sourceBunchSetId);

        assertContainsOnly(guyBunch, manager.getBunchSet(setId));
        assertEmpty(manager.getBunchSet(oldSetId));
    }

    @Test
    default void testShareConceptReusesBunchSet() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId guyConcept = manager.getNextAvailableConceptId();
        final AcceptationId guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final ConceptId personConcept = obtainNewConcept(manager, alphabet, "persona");

        final BunchId targetBunch1 = obtainNewBunch(manager, alphabet, "mis palabras 1");
        final BunchId targetBunch2 = obtainNewBunch(manager, alphabet, "mis palabras 2");

        final BunchId guyBunch = conceptAsBunchId(guyConcept);
        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(targetBunch1), setOf(guyBunch), setOf(), alphabet, null, null, null, null, null);

        final BunchId personBunch = conceptAsBunchId(personConcept);
        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(targetBunch2), setOf(guyBunch, personBunch), setOf(), alphabet, null, null, null, null, null);

        final BunchSetId setId = manager.getAgentRegister(agent1).sourceBunchSetId;
        final BunchSetId oldAgent2SetId = manager.getAgentRegister(agent2).sourceBunchSetId;
        assertTrue(manager.shareConcept(guyAcc, personConcept));

        assertEquals(setId, manager.getAgentRegister(agent1).sourceBunchSetId);
        assertEquals(setId, manager.getAgentRegister(agent2).sourceBunchSetId);
        assertContainsOnly(guyBunch, manager.getBunchSet(setId));
        assertEmpty(manager.getBunchSet(oldAgent2SetId));
    }

    @Test
    default void testShareConceptAvoidsDuplicatedBunchInBunchSet() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId guyConcept = manager.getNextAvailableConceptId();
        final AcceptationId guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final ConceptId personConcept = obtainNewConcept(manager, alphabet, "persona");

        obtainNewAcceptation(manager, alphabet, "mis palabras 1");
        final BunchId targetBunch2 = obtainNewBunch(manager, alphabet, "mis palabras 2");

        final BunchId guyBunch = conceptAsBunchId(guyConcept);
        final BunchId personBunch = conceptAsBunchId(personConcept);
        final AgentId agent = addSingleAlphabetAgent(manager, setOf(targetBunch2), setOf(guyBunch, personBunch), setOf(), alphabet, null, null, null, null, null);

        final BunchSetId setId = manager.getAgentRegister(agent).sourceBunchSetId;
        assertTrue(manager.shareConcept(guyAcc, personConcept));

        assertEquals(setId, manager.getAgentRegister(agent).sourceBunchSetId);

        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .select(table.getBunchColumnIndex());
        final ConceptSetter<ConceptId> conceptIdManager = getConceptIdManager();
        assertContainsOnly(guyConcept, db.select(query).map(row -> conceptIdManager.getKeyFromDbValue(row.get(0))).toList());
    }

    @Test
    default void testShareConceptAvoidsDuplicatedRuledConceptsAndAcceptations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId jumpConcept = manager.getNextAvailableConceptId();
        final AcceptationId jumpAcc = addSimpleAcceptation(manager, alphabet, jumpConcept, "saltar");

        final ConceptId jumpConcept2 = manager.getNextAvailableConceptId();
        final AcceptationId jumpAcc2 = addSimpleAcceptation(manager, alphabet, jumpConcept2, "brincar");

        final BunchId bunchBunch = obtainNewBunch(manager, alphabet, "mi lista");
        final RuleId gerundRule = obtainNewRule(manager, alphabet, "gerundio");
        final RuleId continuousRule = obtainNewRule(manager, alphabet, "continuo");

        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(bunchBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(bunchBunch), setOf(), alphabet, null, "estoy ", null, null, continuousRule);

        final AcceptationId ruledJumpAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, jumpAcc);
        final AcceptationId ruledJumpAcc2 = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, jumpAcc2);
        assertNotEquals(ruledJumpAcc, ruledJumpAcc2);

        final AcceptationId ruled2JumpAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, ruledJumpAcc);
        final AcceptationId ruled2JumpAcc2 = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, ruledJumpAcc2);
        assertNotEquals(ruled2JumpAcc, ruled2JumpAcc2);

        final ConceptId ruledJumpConcept = manager.conceptFromAcceptation(ruledJumpAcc);
        final ConceptId ruledJumpConcept2 = manager.conceptFromAcceptation(ruledJumpAcc2);
        assertNotEquals(ruledJumpConcept, ruledJumpConcept2);

        final ConceptId ruled2JumpConcept = manager.conceptFromAcceptation(ruled2JumpAcc);
        final ConceptId ruled2JumpConcept2 = manager.conceptFromAcceptation(ruled2JumpAcc2);
        assertNotEquals(ruled2JumpConcept, ruled2JumpConcept2);

        assertTrue(manager.shareConcept(jumpAcc, jumpConcept2));
        assertSinglePair(ruledJumpConcept, jumpConcept, manager.findRuledConceptsByRule(gerundRule));
        final ImmutableMap<AcceptationId, AcceptationId> ruledAcceptations = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent1);
        assertSize(2, ruledAcceptations);
        assertEquals(jumpAcc, ruledAcceptations.get(ruledJumpAcc));
        assertEquals(jumpAcc2, ruledAcceptations.get(ruledJumpAcc2));
        assertEquals(ruledJumpConcept, manager.conceptFromAcceptation(ruledJumpAcc));
        assertEquals(ruledJumpConcept, manager.conceptFromAcceptation(ruledJumpAcc2));

        assertSinglePair(alphabet, "saltando", manager.getAcceptationTexts(ruledJumpAcc));
        assertSinglePair(alphabet, "brincando", manager.getAcceptationTexts(ruledJumpAcc2));

        assertSinglePair(ruled2JumpConcept, ruledJumpConcept, manager.findRuledConceptsByRule(continuousRule));
        final ImmutableMap<AcceptationId, AcceptationId> ruled2Acceptations = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent2);
        assertSize(2, ruled2Acceptations);
        assertEquals(ruledJumpAcc, ruled2Acceptations.get(ruled2JumpAcc));
        assertEquals(ruledJumpAcc2, ruled2Acceptations.get(ruled2JumpAcc2));
        assertEquals(ruled2JumpConcept, manager.conceptFromAcceptation(ruled2JumpAcc));
        assertEquals(ruled2JumpConcept, manager.conceptFromAcceptation(ruled2JumpAcc2));

        assertSinglePair(alphabet, "estoy saltando", manager.getAcceptationTexts(ruled2JumpAcc));
        assertSinglePair(alphabet, "estoy brincando", manager.getAcceptationTexts(ruled2JumpAcc2));
    }

    @Test
    default void testUpdateAgentWhenUpdatingRuleFromAlreadyUsedRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final ConceptId getWetConcept = manager.getNextAvailableConceptId();
        final AcceptationId getWetEsAcc = addSimpleAcceptation(manager, esAlphabet, getWetConcept, "mojarse");
        final AcceptationId getWetJaAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "濡れる");

        final BunchId esVerbBunch = obtainNewBunch(manager, esAlphabet, "Verbo español");
        manager.addAcceptationInBunch(esVerbBunch, getWetEsAcc);

        final BunchId jaVerbBunch = obtainNewBunch(manager, esAlphabet, "Verbo japonés");
        manager.addAcceptationInBunch(jaVerbBunch, getWetJaAcc);

        final RuleId badCausalRule = obtainNewRule(manager, esAlphabet, "causalización");
        final RuleId causalRule = obtainNewRule(manager, esAlphabet, "causal");

        final AgentId esAgent = addSingleAlphabetAgent(manager, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", causalRule);

        final AgentId jaAgent = addSingleAlphabetAgent(manager, setOf(), setOf(jaVerbBunch), setOf(), jaAlphabet, null, null, "る", "させる", causalRule);

        updateSingleAlphabetAgent(manager, esAgent, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", badCausalRule);

        final AcceptationId makeWetEsAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(esAgent, getWetEsAcc);
        assertSinglePair(esAlphabet, "hacer que se moje", manager.getAcceptationTexts(makeWetEsAcc));

        final AcceptationId makeWetJaAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(jaAgent, getWetJaAcc);
        assertSinglePair(jaAlphabet, "濡れさせる", manager.getAcceptationTexts(makeWetJaAcc));

        final ConceptId makeWetJaConcept = manager.conceptFromAcceptation(makeWetJaAcc);
        assertNotEquals(makeWetJaConcept, manager.conceptFromAcceptation(makeWetEsAcc));
    }

    @Test
    default void testUpdateAgentWhenAgentRuleToAlreadyUsedRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final ConceptId getWetConcept = manager.getNextAvailableConceptId();
        final AcceptationId getWetEsAcc = addSimpleAcceptation(manager, esAlphabet, getWetConcept, "mojarse");
        final AcceptationId getWetJaAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "濡れる");

        final BunchId esVerbBunch = obtainNewBunch(manager, esAlphabet, "Verbo español");
        manager.addAcceptationInBunch(esVerbBunch, getWetEsAcc);

        final BunchId jaVerbBunch = obtainNewBunch(manager, esAlphabet, "Verbo japonés");
        manager.addAcceptationInBunch(jaVerbBunch, getWetJaAcc);

        final RuleId badCausalRule = obtainNewRule(manager, esAlphabet, "causalización");

        final AgentId esAgent = addSingleAlphabetAgent(manager, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", badCausalRule);

        final RuleId causalRule = obtainNewRule(manager, esAlphabet, "causal");

        final AgentId jaAgent = addSingleAlphabetAgent(manager, setOf(), setOf(jaVerbBunch), setOf(), jaAlphabet, null, null, "る", "させる", causalRule);

        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(badCausalRule));

        updateSingleAlphabetAgent(manager, esAgent, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", causalRule);

        final AcceptationId makeWetEsAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(esAgent, getWetEsAcc);
        assertSinglePair(esAlphabet, "hacer que se moje", manager.getAcceptationTexts(makeWetEsAcc));

        final AcceptationId makeWetJaAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(jaAgent, getWetJaAcc);
        assertSinglePair(jaAlphabet, "濡れさせる", manager.getAcceptationTexts(makeWetJaAcc));

        final ConceptId makeWetConcept = manager.conceptFromAcceptation(makeWetJaAcc);
        assertEquals(makeWetConcept, manager.conceptFromAcceptation(makeWetEsAcc));
        assertEmpty(manager.findRuledConceptsByRule(badCausalRule));
        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(causalRule));
    }

    @Test
    default void testUpdateAgentWhenUpdatingRuleBetweenUsedRules() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final ConceptId getWetConcept = manager.getNextAvailableConceptId();
        final AcceptationId getWetEsAcc = addSimpleAcceptation(manager, esAlphabet, getWetConcept, "mojarse");
        final AcceptationId getWetJaAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "濡れる");
        final AcceptationId getWetNaruAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "びしょびしょになる");

        final BunchId esVerbBunch = obtainNewBunch(manager, esAlphabet, "Verbo español");
        manager.addAcceptationInBunch(esVerbBunch, getWetEsAcc);

        final BunchId jaVerbBunch = obtainNewBunch(manager, esAlphabet, "Verbo japonés");
        manager.addAcceptationInBunch(jaVerbBunch, getWetJaAcc);

        final BunchId naruVerbBunch = obtainNewBunch(manager, esAlphabet, "Adjetivo con naru");
        manager.addAcceptationInBunch(naruVerbBunch, getWetNaruAcc);

        final RuleId badCausalRule = obtainNewRule(manager, esAlphabet, "causalización");

        final AgentId esAgent = addSingleAlphabetAgent(manager, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", badCausalRule);

        final RuleId causalRule = obtainNewRule(manager, esAlphabet, "causal");

        final AgentId jaAgent = addSingleAlphabetAgent(manager, setOf(), setOf(jaVerbBunch), setOf(), jaAlphabet, null, null, "る", "させる", causalRule);

        final AgentId naruAgent = addSingleAlphabetAgent(manager, setOf(), setOf(naruVerbBunch), setOf(), jaAlphabet, null, null, "になる", "にする", badCausalRule);

        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(badCausalRule));

        updateSingleAlphabetAgent(manager, esAgent, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", causalRule);

        final AcceptationId makeWetEsAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(esAgent, getWetEsAcc);
        assertSinglePair(esAlphabet, "hacer que se moje", manager.getAcceptationTexts(makeWetEsAcc));

        final AcceptationId makeWetJaAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(jaAgent, getWetJaAcc);
        assertSinglePair(jaAlphabet, "濡れさせる", manager.getAcceptationTexts(makeWetJaAcc));

        final AcceptationId makeWetNaruAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(naruAgent, getWetNaruAcc);
        assertSinglePair(jaAlphabet, "びしょびしょにする", manager.getAcceptationTexts(makeWetNaruAcc));

        final ConceptId makeWetConcept = manager.conceptFromAcceptation(makeWetJaAcc);
        assertEquals(makeWetConcept, manager.conceptFromAcceptation(makeWetEsAcc));
        assertNotEquals(makeWetConcept, manager.conceptFromAcceptation(makeWetNaruAcc));
        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(badCausalRule));
        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(causalRule));
    }

    @Test
    default void testShareConceptWhenLinkingRuleConcepts() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final AcceptationId dieJaAcc = obtainNewAcceptation(manager, jaAlphabet, "死ぬ");

        final ConceptId verbConcept = manager.getNextAvailableConceptId();
        final AcceptationId verbJaAcc = addSimpleAcceptation(manager, jaAlphabet, verbConcept, "動詞");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        manager.addAcceptationInBunch(verbBunch, dieJaAcc);

        final ConceptId accidentalConcept = manager.getNextAvailableConceptId();
        final AcceptationId accidentalAcc = addSimpleAcceptation(manager, esAlphabet, accidentalConcept, "accidental");

        final RuleId accidentalRule = conceptAsRuleId(accidentalConcept);
        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぬ", "んでしまう", accidentalRule);

        final ConceptId accidentalConcept2 = manager.getNextAvailableConceptId();
        final AcceptationId accidentalAcc2 = addSimpleAcceptation(manager, esAlphabet, accidentalConcept2, "accidental informal");

        final RuleId accidentalRule2 = conceptAsRuleId(accidentalConcept2);
        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぬ", "んじゃう", accidentalRule2);

        assertTrue(manager.shareConcept(accidentalAcc, accidentalConcept2));

        final AcceptationId accidentalDieAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, dieJaAcc);
        assertNotEquals(dieJaAcc, accidentalDieAcc);
        assertNotEquals(verbJaAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc2, accidentalDieAcc);

        final AcceptationId accidentalDieAcc2 = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, dieJaAcc);
        assertNotEquals(dieJaAcc, accidentalDieAcc2);
        assertNotEquals(verbJaAcc, accidentalDieAcc2);
        assertNotEquals(accidentalAcc, accidentalDieAcc2);
        assertNotEquals(accidentalAcc2, accidentalDieAcc2);
        assertNotEquals(accidentalDieAcc, accidentalDieAcc2);

        final ConceptId accidentalDieConcept = manager.conceptFromAcceptation(accidentalDieAcc);
        assertEquals(accidentalDieConcept, manager.conceptFromAcceptation(accidentalDieAcc2));
    }

    @Test
    default void testShareConceptWhenLinkingRuleToNonRuleConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final ConceptId dieConcept = manager.getNextAvailableConceptId();
        final AcceptationId dieJaAcc = addSimpleAcceptation(manager, jaAlphabet, dieConcept, "死ぬ");

        final ConceptId verbConcept = manager.getNextAvailableConceptId();
        final AcceptationId verbJaAcc = addSimpleAcceptation(manager, jaAlphabet, verbConcept, "動詞");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        manager.addAcceptationInBunch(verbBunch, dieJaAcc);

        final ConceptId accidentalConcept = manager.getNextAvailableConceptId();
        final AcceptationId accidentalAcc = addSimpleAcceptation(manager, esAlphabet, accidentalConcept, "accidental");

        final RuleId accidentalRule = conceptAsRuleId(accidentalConcept);
        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぬ", "んでしまう", accidentalRule);

        final ConceptId accidentalConcept2 = manager.getNextAvailableConceptId();
        final AcceptationId accidentalAcc2 = addSimpleAcceptation(manager, esAlphabet, accidentalConcept2, "accidental informal");

        assertTrue(manager.shareConcept(accidentalAcc2, accidentalConcept));

        final AcceptationId accidentalDieAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, dieJaAcc);
        assertNotEquals(dieJaAcc, accidentalDieAcc);
        assertNotEquals(verbJaAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc2, accidentalDieAcc);

        assertEmpty(manager.findRuledConceptsByRule(accidentalRule));
        final RuleId accidentalRule2 = conceptAsRuleId(accidentalConcept2);
        final ImmutableMap<ConceptId, ConceptId> ruledConcepts = manager.findRuledConceptsByRule(accidentalRule2);
        assertContainsOnly(dieConcept, ruledConcepts);

        final ConceptId accidentalDieConcept = ruledConcepts.keyAt(0);
        assertEquals(accidentalDieConcept, manager.conceptFromAcceptation(accidentalDieAcc));
    }

    @Test
    default void testUpdateAgentWhenChangingAdderInFirstChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final AcceptationId callJaAcc = obtainNewAcceptation(manager, jaAlphabet, "呼ぶ");

        final BunchId verbBunch = obtainNewBunch(manager, jaAlphabet, "動詞");
        manager.addAcceptationInBunch(verbBunch, callJaAcc);

        final RuleId accidentalRule = obtainNewRule(manager, esAlphabet, "accidental");
        final BunchId canBePastBunch = obtainNewBunch(manager, esAlphabet, "puede ser pasado");

        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(canBePastBunch), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぶ", "じまう", accidentalRule);

        final RuleId pastRule = obtainNewRule(manager, esAlphabet, "pasado");

        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(canBePastBunch), setOf(), jaAlphabet, null, null, "う", "った", pastRule);

        assertTrue(updateSingleAlphabetAgent(manager, agent1, setOf(canBePastBunch), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぶ", "んじまう", accidentalRule));

        final ImmutableMap<AcceptationId, AcceptationId> ruledAccs1 = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent1);
        assertContainsOnly(callJaAcc, ruledAccs1);

        final ImmutableMap<AcceptationId, AcceptationId> ruledAccs2 = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent2);
        assertContainsOnly(ruledAccs1.keyAt(0), ruledAccs2);

        final AcceptationId callAccidentalPastAcc = ruledAccs2.keyAt(0);
        assertSinglePair(jaAlphabet, "呼んじまった", manager.getAcceptationTexts(callAccidentalPastAcc));
    }

    @Test
    default void testUpdateAgentWhenChangingAdderInFirstChainedAgentWhenPickedSampleAcceptationForSecondIsOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final AcceptationId singJaAcc = obtainNewAcceptation(manager, jaAlphabet, "歌う");
        final AcceptationId callJaAcc = obtainNewAcceptation(manager, jaAlphabet, "呼ぶ");

        final BunchId buVerbBunch = obtainNewBunch(manager, jaAlphabet, "verbo acabado en ぶ");
        manager.addAcceptationInBunch(buVerbBunch, callJaAcc);

        final RuleId accidentalRule = obtainNewRule(manager, esAlphabet, "accidental");

        final BunchId uVerbBunch = obtainNewBunch(manager, esAlphabet, "verbo acabado en う");
        manager.addAcceptationInBunch(uVerbBunch, singJaAcc);

        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(uVerbBunch), setOf(buVerbBunch), setOf(), jaAlphabet, null, null, "ぶ", "じまう", accidentalRule);

        final RuleId pastRule = obtainNewRule(manager, esAlphabet, "pasado");

        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(uVerbBunch), setOf(), jaAlphabet, null, null, "う", "った", pastRule);

        assertTrue(updateSingleAlphabetAgent(manager, agent1, setOf(uVerbBunch), setOf(buVerbBunch), setOf(), jaAlphabet, null, null, "ぶ", "んじまう", accidentalRule));

        final ImmutableMap<AcceptationId, AcceptationId> ruledAccs1 = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent1);
        assertContainsOnly(callJaAcc, ruledAccs1);

        final ImmutableMap<AcceptationId, AcceptationId> ruledAccs2 = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent2);
        assertContainsOnly(ruledAccs1.keyAt(0), singJaAcc, ruledAccs2);

        final AcceptationId callAccidentalPastAcc = ruledAccs2.keyAt(equal(ruledAccs2.valueAt(0), singJaAcc)? 1 : 0);
        assertSinglePair(jaAlphabet, "呼んじまった", manager.getAcceptationTexts(callAccidentalPastAcc));
    }

    @Test
    default void testFindAcceptationAndRulesFromTextForStaticAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final ImmutableList<SearchResult<AcceptationId, RuleId>> searchResults = manager.findAcceptationAndRulesFromText("cant", DbQuery.RestrictionStringTypes.STARTS_WITH, new ImmutableIntRange(0, 19));
        assertSize(1, searchResults);

        final SearchResult<AcceptationId, RuleId> searchResult = searchResults.valueAt(0);
        assertFalse(searchResult.isDynamic());
        assertEquals(singAcceptation, searchResult.getId());
        assertEmpty(searchResult.getAppliedRules());
        assertEquals("cantar", searchResult.getStr());
        assertEquals("cantar", searchResult.getMainStr());
    }

    @Test
    default void testFindAcceptationAndRulesFromTextForDynamicAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AcceptationId singAcceptation = obtainNewAcceptation(manager, alphabet, "cantar");

        final BunchId sourceBunch = obtainNewBunch(manager, alphabet, "origen");
        assertTrue(manager.addAcceptationInBunch(sourceBunch, singAcceptation));

        final RuleId firstPersonOfPresentRule = obtainNewRule(manager, alphabet, "primera persona del presente de indicativo");
        final AgentId agentId = addSingleAlphabetAgent(manager, setOf(), setOf(sourceBunch), setOf(), alphabet, null, null, "ar", "o", firstPersonOfPresentRule);

        final AcceptationId ruledAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, singAcceptation);

        final ImmutableList<SearchResult<AcceptationId, RuleId>> searchResults = manager.findAcceptationAndRulesFromText("canto", DbQuery.RestrictionStringTypes.STARTS_WITH, new ImmutableIntRange(0, 19));
        assertSize(1, searchResults);

        final SearchResult<AcceptationId, RuleId> searchResult = searchResults.valueAt(0);
        assertTrue(searchResult.isDynamic());
        assertEquals(ruledAcceptation, searchResult.getId());
        assertContainsOnly(firstPersonOfPresentRule, searchResult.getAppliedRules());
        assertEquals("canto", searchResult.getStr());
        assertEquals("canto", searchResult.getMainStr());
        assertEquals("cantar", searchResult.getMainAccMainStr());
    }

    @Test
    default void testReplaceConversionFromNonMatchingToMatchingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId kana = manager.addLanguage("ja").mainAlphabet;
        final ImmutableMap<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("い", "i")
                .put("う", "u")
                .put("か", "ka")
                .put("き", "ki")
                .put("け", "ke")
                .put("し", "shi")
                .put("た", "ta")
                .put("て", "te")
                .put("ひ", "hi")
                .put("よ", "yo")
                .build();

        final AlphabetId roumaji = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, conversionMap);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final AcceptationId highAcceptation = obtainNewAcceptation(manager, kana, "たかい");

        final BunchId sourceBunch = obtainNewBunch(manager, kana, "いけいようし");
        assertTrue(manager.addAcceptationInBunch(sourceBunch, highAcceptation));

        final RuleId negativeRule = obtainNewRule(manager, kana, "ひていてき");
        final AgentId agent = addSingleAlphabetAgent(manager, setOf(), setOf(sourceBunch), setOf(), kana, null, null, "い", "くない", negativeRule);
        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, highAcceptation));

        final ImmutableMap<String, String> newConversionMap = conversionMap
                .put("く", "ku")
                .put("な", "na");

        final Conversion<AlphabetId> newConversion = new Conversion<>(kana, roumaji, newConversionMap);
        assertTrue(manager.replaceConversion(newConversion));

        final AcceptationId notHighAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, highAcceptation);
        assertNotNull(notHighAcceptation);
        assertNotEquals(highAcceptation, notHighAcceptation);

        final ImmutableList<SearchResult<AcceptationId, RuleId>> searchResults = manager.findAcceptationAndRulesFromText("taka", DbQuery.RestrictionStringTypes.STARTS_WITH, new ImmutableIntRange(0, 19));
        assertSize(2, searchResults);

        final SearchResult<AcceptationId, RuleId> notHighSearchResult;
        if (highAcceptation.equals(searchResults.valueAt(0).getId())) {
            notHighSearchResult = searchResults.valueAt(1);
            assertEquals(notHighAcceptation, notHighSearchResult.getId());
        }
        else {
            notHighSearchResult = searchResults.valueAt(0);
            assertEquals(notHighAcceptation, notHighSearchResult.getId());
            assertEquals(highAcceptation, searchResults.valueAt(1).getId());
        }

        assertContainsOnly(negativeRule, notHighSearchResult.getAppliedRules());
        assertEquals("たかくない", notHighSearchResult.getMainStr());
        assertEquals("takakunai", notHighSearchResult.getStr());
        assertTrue(notHighSearchResult.isDynamic());
    }

    @Test
    default void testReplaceConversionFromMatchingToNonMatchingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId kana = manager.addLanguage("ja").mainAlphabet;
        final ImmutableMap<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("い", "i")
                .put("う", "u")
                .put("か", "ka")
                .put("き", "ki")
                .put("く", "ku")
                .put("け", "ke")
                .put("し", "shi")
                .put("た", "ta")
                .put("て", "te")
                .put("ひ", "hi")
                .put("よ", "yo")
                .put("な", "na")
                .build();

        final AlphabetId roumaji = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, conversionMap);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final AcceptationId highAcceptation = obtainNewAcceptation(manager, kana, "たかい");

        final BunchId sourceBunch = obtainNewBunch(manager, kana, "いけいようし");
        assertTrue(manager.addAcceptationInBunch(sourceBunch, highAcceptation));

        final RuleId negativeRule = obtainNewRule(manager, kana, "ひていてき");
        final AgentId agent = addSingleAlphabetAgent(manager, setOf(), setOf(sourceBunch), setOf(), kana, null, null, "い", "くない", negativeRule);

        final AcceptationId notHighAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, highAcceptation);
        assertNotNull(notHighAcceptation);
        assertNotEquals(highAcceptation, notHighAcceptation);

        ImmutableList<SearchResult<AcceptationId, RuleId>> searchResults = manager.findAcceptationAndRulesFromText("taka", DbQuery.RestrictionStringTypes.STARTS_WITH, new ImmutableIntRange(0, 19));
        assertSize(2, searchResults);

        final SearchResult<AcceptationId, RuleId> notHighSearchResult;
        if (highAcceptation.equals(searchResults.valueAt(0).getId())) {
            notHighSearchResult = searchResults.valueAt(1);
            assertEquals(notHighAcceptation, notHighSearchResult.getId());
        }
        else {
            notHighSearchResult = searchResults.valueAt(0);
            assertEquals(notHighAcceptation, notHighSearchResult.getId());
            assertEquals(highAcceptation, searchResults.valueAt(1).getId());
        }

        assertContainsOnly(negativeRule, notHighSearchResult.getAppliedRules());
        assertEquals("たかくない", notHighSearchResult.getMainStr());
        assertEquals("takakunai", notHighSearchResult.getStr());
        assertTrue(notHighSearchResult.isDynamic());

        final ImmutableMap<String, String> newConversionMap = conversionMap.filter(str -> !"ku".equals(str) && !"na".equals(str));
        final Conversion<AlphabetId> newConversion = new Conversion<>(kana, roumaji, newConversionMap);
        assertTrue(manager.replaceConversion(newConversion));

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, highAcceptation));

        searchResults = manager.findAcceptationAndRulesFromText("taka", DbQuery.RestrictionStringTypes.STARTS_WITH, new ImmutableIntRange(0, 19));
        assertSize(1, searchResults);
        assertEquals(highAcceptation, searchResults.valueAt(0).getId());
    }

    @Test
    default void testReplaceConversionFromNonMatchingToMatchingAcceptationInChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId kana = manager.addLanguage("ja").mainAlphabet;
        final ImmutableMap<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("い", "i")
                .put("う", "u")
                .put("か", "ka")
                .put("き", "ki")
                .put("け", "ke")
                .put("こ", "ko")
                .put("し", "shi")
                .put("た", "ta")
                .put("った", "tta")
                .put("て", "te")
                .put("ひ", "hi")
                .put("よ", "yo")
                .build();

        final AlphabetId roumaji = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, conversionMap);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final AcceptationId highAcceptation = obtainNewAcceptation(manager, kana, "たかい");

        final BunchId sourceBunch = obtainNewBunch(manager, kana, "いけいようし");
        assertTrue(manager.addAcceptationInBunch(sourceBunch, highAcceptation));

        final BunchId targetBunch = obtainNewBunch(manager, kana, "ひていてき");
        final RuleId negativeRule = obtainNewRule(manager, kana, "ひていてき");
        final RuleId pastRule = obtainNewRule(manager, kana, "かこ");
        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(targetBunch), setOf(sourceBunch), setOf(), kana, null, null, "い", "くない", negativeRule);
        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(sourceBunch, targetBunch), setOf(), kana, null, null, "い", "かった", pastRule);

        final ImmutableMap<String, String> newConversionMap = conversionMap
                .put("く", "ku")
                .put("な", "na");

        final Conversion<AlphabetId> newConversion = new Conversion<>(kana, roumaji, newConversionMap);
        assertTrue(manager.replaceConversion(newConversion));

        final AcceptationId notHighAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, highAcceptation);
        final AcceptationId wasHighAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, highAcceptation);
        final AcceptationId wasNotHighAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, notHighAcceptation);
        assertNotNull(notHighAcceptation);
        assertNotNull(wasHighAcceptation);
        assertNotNull(wasNotHighAcceptation);
        assertNotEquals(highAcceptation, notHighAcceptation);
        assertNotEquals(highAcceptation, wasHighAcceptation);
        assertNotEquals(highAcceptation, wasNotHighAcceptation);
        assertNotEquals(notHighAcceptation, wasHighAcceptation);
        assertNotEquals(notHighAcceptation, wasNotHighAcceptation);
        assertNotEquals(wasHighAcceptation, wasNotHighAcceptation);

        final ImmutableList<SearchResult<AcceptationId, RuleId>> searchResults = manager.findAcceptationAndRulesFromText("taka", DbQuery.RestrictionStringTypes.STARTS_WITH, new ImmutableIntRange(0, 19));
        assertSize(4, searchResults);

        final SearchResult<AcceptationId, RuleId> notHighSearchResult = searchResults.findFirst(result -> notHighAcceptation.equals(result.getId()), null);
        assertContainsOnly(negativeRule, notHighSearchResult.getAppliedRules());
        assertEquals("たかくない", notHighSearchResult.getMainStr());
        assertEquals("takakunai", notHighSearchResult.getStr());
        assertTrue(notHighSearchResult.isDynamic());

        final SearchResult<AcceptationId, RuleId> wasHighSearchResult = searchResults.findFirst(result -> wasHighAcceptation.equals(result.getId()), null);
        assertContainsOnly(pastRule, wasHighSearchResult.getAppliedRules());
        assertEquals("たかかった", wasHighSearchResult.getMainStr());
        assertEquals("takakatta", wasHighSearchResult.getStr());
        assertTrue(wasHighSearchResult.isDynamic());

        final SearchResult<AcceptationId, RuleId> wasNotHighSearchResult = searchResults.findFirst(result -> wasNotHighAcceptation.equals(result.getId()), null);
        assertContainsOnly(negativeRule, pastRule, wasNotHighSearchResult.getAppliedRules());
        assertEquals("たかくなかった", wasNotHighSearchResult.getMainStr());
        assertEquals("takakunakatta", wasNotHighSearchResult.getStr());
        assertTrue(wasNotHighSearchResult.isDynamic());
    }

    @Test
    default void testReplaceConversionFromMatchingToNonMatchingAcceptationInChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId kana = manager.addLanguage("ja").mainAlphabet;
        final ImmutableMap<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("い", "i")
                .put("う", "u")
                .put("か", "ka")
                .put("き", "ki")
                .put("く", "ku")
                .put("け", "ke")
                .put("こ", "ko")
                .put("し", "shi")
                .put("た", "ta")
                .put("った", "tta")
                .put("て", "te")
                .put("な", "na")
                .put("ひ", "hi")
                .put("よ", "yo")
                .build();

        final AlphabetId roumaji = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, conversionMap);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final AcceptationId highAcceptation = obtainNewAcceptation(manager, kana, "たかい");

        final BunchId sourceBunch = obtainNewBunch(manager, kana, "いけいようし");
        assertTrue(manager.addAcceptationInBunch(sourceBunch, highAcceptation));

        final BunchId targetBunch = obtainNewBunch(manager, kana, "ひていてき");
        final RuleId negativeRule = obtainNewRule(manager, kana, "ひていてき");
        final RuleId pastRule = obtainNewRule(manager, kana, "かこ");
        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(targetBunch), setOf(sourceBunch), setOf(), kana, null, null, "い", "くない", negativeRule);
        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(sourceBunch, targetBunch), setOf(), kana, null, null, "い", "かった", pastRule);

        final ImmutableMap<String, String> newConversionMap = conversionMap.filter(str -> !str.equals("ku") && !str.equals("na"));
        final Conversion<AlphabetId> newConversion = new Conversion<>(kana, roumaji, newConversionMap);
        assertTrue(manager.replaceConversion(newConversion));

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, highAcceptation));
        final AcceptationId wasHighAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, highAcceptation);
        assertNotNull(wasHighAcceptation);
        assertNotEquals(highAcceptation, wasHighAcceptation);

        final ImmutableList<SearchResult<AcceptationId, RuleId>> searchResults = manager.findAcceptationAndRulesFromText("taka", DbQuery.RestrictionStringTypes.STARTS_WITH, new ImmutableIntRange(0, 19));
        assertSize(2, searchResults);

        final SearchResult<AcceptationId, RuleId> highSearchResult = searchResults.findFirst(result -> highAcceptation.equals(result.getId()), null);
        assertEmpty(highSearchResult.getAppliedRules());
        assertEquals("たかい", highSearchResult.getMainStr());
        assertEquals("takai", highSearchResult.getStr());
        assertFalse(highSearchResult.isDynamic());

        final SearchResult<AcceptationId, RuleId> wasHighSearchResult = searchResults.findFirst(result -> wasHighAcceptation.equals(result.getId()), null);
        assertContainsOnly(pastRule, wasHighSearchResult.getAppliedRules());
        assertEquals("たかかった", wasHighSearchResult.getMainStr());
        assertEquals("takakatta", wasHighSearchResult.getStr());
        assertTrue(wasHighSearchResult.isDynamic());
    }

    @Test
    default void testReplaceConversionFromNonMatchingToMatchingAcceptationInNonRuleChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId kana = manager.addLanguage("ja").mainAlphabet;
        final ImmutableMap<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("い", "i")
                .put("う", "u")
                .put("か", "ka")
                .put("き", "ki")
                .put("け", "ke")
                .put("し", "shi")
                .put("た", "ta")
                .put("て", "te")
                .put("ひ", "hi")
                .put("よ", "yo")
                .build();

        final AlphabetId roumaji = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, conversionMap);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final AcceptationId highAcceptation = obtainNewAcceptation(manager, kana, "たかい");

        final BunchId sourceBunch = obtainNewBunch(manager, kana, "いけいようし");
        assertTrue(manager.addAcceptationInBunch(sourceBunch, highAcceptation));

        final BunchId middleBunch = obtainNewBunch(manager, kana, "ひていてき");
        final BunchId targetBunch = obtainNewBunch(manager, kana, "よい");
        final RuleId negativeRule = obtainNewRule(manager, kana, "ひていてき");
        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(middleBunch), setOf(sourceBunch), setOf(), kana, null, null, "い", "くない", negativeRule);
        final AgentId agent2 = addSingleAlphabetAgent(manager, setOf(targetBunch), setOf(sourceBunch, middleBunch), setOf(), null, null, null, null, null, null);
        assertContainsOnly(highAcceptation, manager.getAcceptationsInBunch(targetBunch));

        final ImmutableMap<String, String> newConversionMap = conversionMap
                .put("く", "ku")
                .put("な", "na");

        final Conversion<AlphabetId> newConversion = new Conversion<>(kana, roumaji, newConversionMap);
        assertTrue(manager.replaceConversion(newConversion));

        final AcceptationId notHighAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, highAcceptation);
        assertNotNull(notHighAcceptation);
        assertNotEquals(highAcceptation, notHighAcceptation);
        assertContainsOnly(highAcceptation, notHighAcceptation, manager.getAcceptationsInBunch(targetBunch));
    }

    @Test
    default void testReplaceConversionFromMatchingToNonMatchingAcceptationInNonRuleChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId kana = manager.addLanguage("ja").mainAlphabet;
        final ImmutableMap<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("い", "i")
                .put("う", "u")
                .put("か", "ka")
                .put("き", "ki")
                .put("く", "ku")
                .put("け", "ke")
                .put("し", "shi")
                .put("た", "ta")
                .put("て", "te")
                .put("な", "na")
                .put("ひ", "hi")
                .put("よ", "yo")
                .build();

        final AlphabetId roumaji = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, conversionMap);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final AcceptationId highAcceptation = obtainNewAcceptation(manager, kana, "たかい");

        final BunchId sourceBunch = obtainNewBunch(manager, kana, "いけいようし");
        assertTrue(manager.addAcceptationInBunch(sourceBunch, highAcceptation));

        final BunchId middleBunch = obtainNewBunch(manager, kana, "ひていてき");
        final BunchId targetBunch = obtainNewBunch(manager, kana, "よい");
        final RuleId negativeRule = obtainNewRule(manager, kana, "ひていてき");
        final AgentId agent1 = addSingleAlphabetAgent(manager, setOf(middleBunch), setOf(sourceBunch), setOf(), kana, null, null, "い", "くない", negativeRule);
        assertNotNull(addSingleAlphabetAgent(manager, setOf(targetBunch), setOf(sourceBunch, middleBunch), setOf(), null, null, null, null, null, null));

        final ImmutableMap<String, String> newConversionMap = conversionMap.filter(str -> !"ku".equals(str) && !"na".equals(str));
        final Conversion<AlphabetId> newConversion = new Conversion<>(kana, roumaji, newConversionMap);
        assertTrue(manager.replaceConversion(newConversion));

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, highAcceptation));
        assertContainsOnly(highAcceptation, manager.getAcceptationsInBunch(targetBunch));
    }

    final class DoubleAlphabetCorrelationComposer<AlphabetId> {
        final AlphabetId first;
        final AlphabetId second;

        DoubleAlphabetCorrelationComposer(AlphabetId first, AlphabetId second) {
            this.first = first;
            this.second = second;
        }

        ImmutableCorrelation<AlphabetId> compose(String text1, String text2) {
            return new ImmutableCorrelation.Builder<AlphabetId>()
                    .put(first, text1)
                    .put(second, text2)
                    .build();
        }
    }

    @Test
    default void testAddAcceptationCreatesRuledAcceptationWithMultipleCorrelationForTaberu1() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);

        final ImmutableCorrelation<AlphabetId> taberuCorrelation = correlationComposer.compose("食べる", "たべる");
        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taberuCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableCorrelation<AlphabetId> tabeCorrelation = correlationComposer.compose("食べ", "たべ");
        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(2, correlationIds);
        assertEquals(tabeCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
    }

    @Test
    default void testAddAcceptationCreatesRuledAcceptationWithMultipleCorrelationsForTaberu2() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> beruCorrelation = correlationComposer.compose("べる", "べる");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testAddAcceptationCreatesRuledAcceptationWithMultipleCorrelationsForTaberu3() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testAddAcceptationCreatesRuledAcceptationWithMultipleCorrelationsForSuru1() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");
        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule);

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
    }

    @Test
    default void testAddAcceptationCreatesRuledAcceptationsWithMultipleCorrelationsForSuru2() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suCorrelation = correlationComposer.compose("為", "す");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");
        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule);

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
    }

    @Test
    default void testAddAgentCreatesRuledAcceptationWithMultipleCorrelationForTaberu1() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taberuCorrelation = correlationComposer.compose("食べる", "たべる");
        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taberuCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableCorrelation<AlphabetId> tabeCorrelation = correlationComposer.compose("食べ", "たべ");
        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(2, correlationIds);
        assertEquals(tabeCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
    }

    @Test
    default void testAddAgentCreatesRuledAcceptationWithMultipleCorrelationsForTaberu2() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> beruCorrelation = correlationComposer.compose("べる", "べる");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testAddAgentCreatesRuledAcceptationWithMultipleCorrelationsForTaberu3() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testAddAgentCreatesRuledAcceptationWithMultipleCorrelationsForSuru1() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
    }

    @Test
    default void testAddAgentCreatesRuledAcceptationsWithMultipleCorrelationsForSuru2() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suCorrelation = correlationComposer.compose("為", "す");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");
        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule);

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
    }

    @Test
    default void testAddAcceptationInBunchCreatesRuledAcceptationsWithMultipleCorrelationsForTaberu1() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taberuCorrelation = correlationComposer.compose("食べる", "たべる");
        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taberuCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);

        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableCorrelation<AlphabetId> tabeCorrelation = correlationComposer.compose("食べ", "たべ");
        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(2, correlationIds);
        assertEquals(tabeCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
    }

    @Test
    default void testAddAcceptationInBunchCreatesRuledAcceptationsWithMultipleCorrelationsForTaberu2() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> beruCorrelation = correlationComposer.compose("べる", "べる");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);

        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testAddAcceptationInBunchCreatesRuledAcceptationWithMultipleCorrelationsForTaberu3() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);

        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testAddAcceptationInBunchCreatesRuledAcceptationWithMultipleCorrelationForSuru1() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule);

        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
    }

    @Test
    default void testAddAcceptationInBunchCreatedRuledAcceptationWithMultipleCorrelationsForSuru2() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suCorrelation = correlationComposer.compose("為", "す");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");
        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule);

        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
    }

    @Test
    default void testUpdateAgentCreatesRuledAcceptationWithMultipleCorrelationForTaberu1WhenChangingSourceBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taberuCorrelation = correlationComposer.compose("食べる", "たべる");
        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taberuCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch1 = obtainNewBunch(manager, enAlphabet, "source1");
        final BunchId sourceBunch2 = obtainNewBunch(manager, enAlphabet, "source2");
        manager.addAcceptationInBunch(sourceBunch2, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch1), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);
        assertTrue(manager.updateAgent(agent, setOf(), setOf(sourceBunch2), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableCorrelation<AlphabetId> tabeCorrelation = correlationComposer.compose("食べ", "たべ");
        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(2, correlationIds);
        assertEquals(tabeCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
    }

    @Test
    default void testUpdateAgentCreatesRuledAcceptationWithMultipleCorrelationsForTaberu2WhenChangingSourceBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> beruCorrelation = correlationComposer.compose("べる", "べる");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch1 = obtainNewBunch(manager, enAlphabet, "source1");
        final BunchId sourceBunch2 = obtainNewBunch(manager, enAlphabet, "source2");
        manager.addAcceptationInBunch(sourceBunch2, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch1), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);
        assertTrue(manager.updateAgent(agent, setOf(), setOf(sourceBunch2), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testUpdateAgentCreatesRuledAcceptationWithMultipleCorrelationsForTaberu3WhenChangingSourceBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch1 = obtainNewBunch(manager, enAlphabet, "source1");
        final BunchId sourceBunch2 = obtainNewBunch(manager, enAlphabet, "source2");
        manager.addAcceptationInBunch(sourceBunch2, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch1), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule);
        assertTrue(manager.updateAgent(agent, setOf(), setOf(sourceBunch2), setOf(), emptyCorrelation, emptyCorrelation, ruCorrelation, taiCorrelation, desireRule));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testUpdateAgentCreatesRuledAcceptationWithMultipleCorrelationsForSuru1WhenChangingSourceBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch1 = obtainNewBunch(manager, enAlphabet, "source1");
        final BunchId sourceBunch2 = obtainNewBunch(manager, enAlphabet, "source2");
        manager.addAcceptationInBunch(sourceBunch2, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch1), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule);
        assertTrue(manager.updateAgent(agent, setOf(), setOf(sourceBunch2), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
    }

    @Test
    default void testUpdateAgentCreatesRuledAcceptationsWithMultipleCorrelationsForSuru2WhenChangingSourceBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suCorrelation = correlationComposer.compose("為", "す");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);
        assertNotNull(eatAcceptation);

        final BunchId sourceBunch1 = obtainNewBunch(manager, enAlphabet, "source1");
        final BunchId sourceBunch2 = obtainNewBunch(manager, enAlphabet, "source2");
        manager.addAcceptationInBunch(sourceBunch2, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");
        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch1), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule);
        assertTrue(manager.updateAgent(agent, setOf(), setOf(sourceBunch2), setOf(), emptyCorrelation, emptyCorrelation, suruCorrelation, shitaiCorrelation, desireRule));

        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableList<CorrelationId> correlationIds = manager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, manager.getCorrelationWithText(correlationIds.valueAt(0)));
    }
}
