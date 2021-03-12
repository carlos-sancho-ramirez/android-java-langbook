package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntPairMap;
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
import sword.langbook3.android.models.MorphologyResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.IntPairMapTestUtils.assertSinglePair;
import static sword.collections.IntTraversableTestUtils.assertContainsOnly;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SetTestUtils.assertEqualSet;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.collections.EqualUtils.equal;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.updateAcceptationSimpleCorrelationArray;
import static sword.langbook3.android.db.BunchesManagerTest.addSpanishSingAcceptation;

/**
 * Include all test related to all responsibilities of a AgentsManager.
 *
 * AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> responsibilities include all responsibilities from BunchesManager, and include the following ones:
 * <li>Bunch sets</li>
 * <li>Rules</li>
 * <li>Ruled concepts</li>
 * <li>Ruled acceptations</li>
 * <li>Agents</li>
 */
interface AgentsManagerTest<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> extends BunchesManagerTest<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> {

    @Override
    AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> createManager(MemoryDatabase db);
    IntSetter<AcceptationId> getAcceptationIdManager();
    RuleId conceptAsRuleId(int conceptId);

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

    static <AcceptationId> ImmutableMap<AcceptationId, AcceptationId> findRuledAcceptationsByAgent(DbExporter.Database db, IntSetter<AcceptationId> acceptationIdSetter, int agent) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(ruledAccs)
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

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> Integer addSingleAlphabetAgent(AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
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

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> boolean updateSingleAlphabetAgent(AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager, int agentId, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
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

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> void assertOnlyOneMorphology(AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager, AcceptationId staticAcceptation, AlphabetId preferredAlphabet, String expectedText, RuleId expectedRule) {
        final MorphologyResult<AcceptationId, RuleId> morphology = getSingleValue(manager.readMorphologiesFromAcceptation(staticAcceptation, preferredAlphabet).morphologies);
        assertEquals(expectedText, morphology.text);
        assertContainsOnly(expectedRule, morphology.rules);
    }

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> void assertNoRuledAcceptationsPresentForChainedAgents(AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager, Add3ChainedAgentsResult result) {
        assertEmpty(manager.getAgentProcessedMap(result.agent1Id));
        assertEmpty(manager.getAgentProcessedMap(result.agent2Id));
        assertEmpty(manager.getAgentProcessedMap(result.agent3Id));
    }

    @Test
    default void testAddAgentApplyingRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        final AcceptationId acceptation = addSimpleAcceptation(manager, alphabet, concept, "cantar");
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        assertTrue(manager.addAcceptationInBunch(verbBunch, acceptation));

        final RuleId gerundRule = conceptAsRuleId(gerund);
        final int agentId = addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final int ruledConcept = manager.findRuledConcept(gerundRule, concept);
        final AcceptationId ruledAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, acceptation);
        assertEquals(ruledConcept, manager.conceptFromAcceptation(ruledAcceptation));

        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(ruledAcceptation));
        assertEquals(acceptation, manager.getStaticAcceptationFromDynamic(ruledAcceptation));
        assertEquals("cantando", manager.readAcceptationMainText(ruledAcceptation));
    }

    @Test
    default void testAddAgentComposingBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int erVerbConcept = arVerbConcept + 1;
        final int verbConcept = erVerbConcept + 1;
        final int singConcept = erVerbConcept + 1;
        final int coughtConcept = singConcept + 1;

        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        assertTrue(manager.addAcceptationInBunch(verbBunch, singAcceptation));

        final AcceptationId coughtAcceptation = addSimpleAcceptation(manager, alphabet, coughtConcept, "toser");
        assertTrue(manager.addAcceptationInBunch(verbBunch, coughtAcceptation));

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ar", null);

        assertContainsOnly(singAcceptation, coughtAcceptation, manager.getAcceptationsInBunch(verbBunch));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(arVerbBunch, agentId));
    }

    @Test
    default void testAddAgentCopyingToTwoBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int erVerbConcept = arVerbConcept + 1;
        final int actionConcept = erVerbConcept + 1;
        final int verbConcept = actionConcept + 1;
        final int singConcept = erVerbConcept + 1;
        final int coughtConcept = singConcept + 1;

        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        assertTrue(manager.addAcceptationInBunch(verbBunch, singAcceptation));

        final AcceptationId coughtAcceptation = addSimpleAcceptation(manager, alphabet, coughtConcept, "toser");
        assertTrue(manager.addAcceptationInBunch(verbBunch, coughtAcceptation));

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId actionBunch = conceptAsBunchId(actionConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch, actionBunch), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ar", null);

        assertContainsOnly(singAcceptation, coughtAcceptation, manager.getAcceptationsInBunch(verbBunch));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(arVerbBunch, agentId));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(actionBunch, agentId));
    }

    default void checkAdd2ChainedAgents(boolean reversedAdditionOrder, boolean addExtraMiddleTargetBunch) {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int arVerbConcept = verbConcept + 1;
        final int extraConcept = arVerbConcept + 1;
        final int singConcept = extraConcept + 1;

        final AcceptationId acceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        assertTrue(manager.addAcceptationInBunch(verbBunch, acceptation));

        final ImmutableCorrelation<AlphabetId> nullCorrelation = new ImmutableCorrelation.Builder<AlphabetId>().build();
        final ImmutableCorrelation<AlphabetId> matcher = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ar")
                .build();
        final ImmutableCorrelation<AlphabetId> adder = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ando")
                .build();

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId extraBunch = conceptAsBunchId(extraConcept);
        final ImmutableSet<BunchId> arVerbBunchSet = setOf(arVerbBunch);
        final ImmutableSet<BunchId> verbBunchSet = setOf(verbBunch);
        final ImmutableSet<BunchId> diffBunches = setOf();
        final ImmutableSet<BunchId> firstTargetBunches = addExtraMiddleTargetBunch? arVerbBunchSet.add(extraBunch) :
                arVerbBunchSet;

        final RuleId gerundRule = conceptAsRuleId(gerund);
        final int agent2Id;
        if (reversedAdditionOrder) {
            agent2Id = manager.addAgent(setOf(), arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerundRule);
            manager.addAgent(firstTargetBunches, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, null);
        }
        else {
            manager.addAgent(firstTargetBunches, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, null);
            agent2Id = manager.addAgent(setOf(), arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerundRule);
        }

        final int ruledConcept = manager.findRuledConcept(gerundRule, singConcept);
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
    default void testAdd2ChainedAgents() {
        checkAdd2ChainedAgents(false, false);
    }

    @Test
    default void testAdd2ChainedAgentsReversedAdditionOrder() {
        checkAdd2ChainedAgents(true, false);
    }

    @Test
    default void testAdd2ChainedAgentsWithExtraMiddleTargetBunch() {
        checkAdd2ChainedAgents(false, true);
    }

    @Test
    default void testAdd2ChainedAgentsReversedAdditionOrderWithExtraMiddleTargetBunch() {
        checkAdd2ChainedAgents(true, true);
    }

    @Test
    default void testAddAcceptationInFirstAgentSourceBunchForChainedAgents() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int maleStudentConcept = manager.getMaxConcept() + 1;
        final AcceptationId maleStudentAcc = addSimpleAcceptation(manager, alphabet, maleStudentConcept, "alumno");

        final int femenineConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineConcept, "femenino");

        final int pluralConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, pluralConcept, "plural");

        final int feminableWordsConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, feminableWordsConcept, "feminizable");

        final int pluralableWordsConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, pluralableWordsConcept, "pluralizable");

        final BunchId pluralableWordsBunch = conceptAsBunchId(pluralableWordsConcept);
        final BunchId feminableWordsBunch = conceptAsBunchId(feminableWordsConcept);
        final RuleId femenineRule = conceptAsRuleId(femenineConcept);
        final int agent1 = addSingleAlphabetAgent(manager, setOf(pluralableWordsBunch), setOf(feminableWordsBunch), setOf(), alphabet, null, null, "o", "a", femenineRule);

        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        final int agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(pluralableWordsBunch), setOf(), alphabet, null, null, null, "s", pluralRule);

        manager.addAcceptationInBunch(feminableWordsBunch, maleStudentAcc);

        final int femaleStudentConcept = manager.findRuledConcept(femenineRule, maleStudentConcept);
        final AcceptationId femaleStudentAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, maleStudentAcc);
        assertEquals(femaleStudentConcept, manager.conceptFromAcceptation(femaleStudentAcc));
        assertSinglePair(alphabet, "alumna", manager.getAcceptationTexts(femaleStudentAcc));

        final int pluralFemaleStudentConcept = manager.findRuledConcept(pluralRule, femaleStudentConcept);
        final AcceptationId pluralFemaleStudentAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, femaleStudentAcc);
        assertEquals(pluralFemaleStudentConcept, manager.conceptFromAcceptation(pluralFemaleStudentAcc));
        assertSinglePair(alphabet, "alumnas", manager.getAcceptationTexts(pluralFemaleStudentAcc));
    }

    default void checkAdd2ChainedAgentsFirstWithoutSource(boolean reversedAdditionOrder, boolean acceptationBeforeAgents) {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int bunchConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, bunchConcept, "pluralizable sustituyendo ón por ones");

        AcceptationId acceptation = null;
        if (acceptationBeforeAgents) {
            final int songConcept = manager.getMaxConcept() + 1;
            acceptation = addSimpleAcceptation(manager, alphabet, songConcept, "canción");
        }

        final ImmutableCorrelation<AlphabetId> nullCorrelation = new ImmutableCorrelation.Builder<AlphabetId>().build();
        final ImmutableCorrelation<AlphabetId> matcher = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ón")
                .build();
        final ImmutableCorrelation<AlphabetId> adder = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ones")
                .build();

        final BunchId bunchBunch = conceptAsBunchId(bunchConcept);
        final ImmutableSet<BunchId> middleBunchSet = setOf(bunchBunch);
        final ImmutableSet<BunchId> noBunchSet = setOf();

        final RuleId pluralRule;
        if (reversedAdditionOrder) {
            final int pluralConcept = manager.getMaxConcept() + 1;
            pluralRule = conceptAsRuleId(pluralConcept);
            assertNotNull(manager.addAgent(setOf(), middleBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, adder, pluralRule));
            assertNotNull(manager.addAgent(middleBunchSet, noBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, matcher, null));
        }
        else {
            assertNotNull(manager.addAgent(middleBunchSet, noBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, matcher, null));
            final int pluralConcept = manager.getMaxConcept() + 1;
            pluralRule = conceptAsRuleId(pluralConcept);
            assertNotNull(manager.addAgent(setOf(), middleBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, adder, pluralRule));
        }

        if (!acceptationBeforeAgents) {
            final int songConcept = manager.getMaxConcept() + 1;
            acceptation = addSimpleAcceptation(manager, alphabet, songConcept, "canción");
        }

        assertOnlyOneMorphology(manager, acceptation, alphabet, "canciones", pluralRule);
    }

    @Test
    default void testAdd2ChainedAgentsFirstWithoutSourceBeforeMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(false, false);
    }

    @Test
    default void testAdd2ChainedAgentsFirstWithoutSourceReversedAdditionOrderBeforeMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(true, false);
    }

    @Test
    default void testAdd2ChainedAgentsFirstWithoutSourceAfterMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(false, true);
    }

    @Test
    default void testAdd2ChainedAgentsFirstWithoutSourceReversedAdditionOrderAfterMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(true, true);
    }

    default void checkAddAgentWithDiffBunch(boolean addAgentBeforeAcceptations) {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int arEndingNounConcept = arVerbConcept + 1;
        final int singConcept = arEndingNounConcept + 1;
        final int palateConcept = singConcept + 1;

        final BunchId arEndingNounBunch = conceptAsBunchId(arEndingNounConcept);
        final ImmutableSet<BunchId> sourceBunches = setOf();
        final ImmutableSet<BunchId> diffBunches = setOf(arEndingNounBunch);

        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        final AcceptationId palateAcceptation;
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final int agentId;
        if (addAgentBeforeAcceptations) {
            agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", null);
            palateAcceptation = addSimpleAcceptation(manager, alphabet, palateConcept, "paladar");
            manager.addAcceptationInBunch(arEndingNounBunch, palateAcceptation);
        }
        else {
            palateAcceptation = addSimpleAcceptation(manager, alphabet, palateConcept, "paladar");
            manager.addAcceptationInBunch(arEndingNounBunch, palateAcceptation);
            agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", null);
        }

        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunch(arVerbBunch));
        assertContainsOnly(agentId, manager.findAllAgentsThatIncludedAcceptationInBunch(arVerbBunch, singAcceptation));
    }

    @Test
    default void testAddAcceptationBeforeAgentWithDiffBunch() {
        checkAddAgentWithDiffBunch(false);
    }

    @Test
    default void testAddAcceptationAfterAgentWithDiffBunch() {
        checkAddAgentWithDiffBunch(true);
    }

    class Add3ChainedAgentsResult {
        final int agent1Id;
        final int agent2Id;
        final int agent3Id;

        Add3ChainedAgentsResult(int agent1Id, int agent2Id, int agent3Id) {
            this.agent1Id = agent1Id;
            this.agent2Id = agent2Id;
            this.agent3Id = agent3Id;
        }
    }

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> Add3ChainedAgentsResult add3ChainedAgents(
            AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager,
            AlphabetId alphabet, ImmutableSet<BunchId> sourceBunchSet, BunchId arVerbBunch, BunchId actionBunch,
            RuleId nominalizationRule, RuleId pluralRule) {

        final ImmutableSet<BunchId> noBunches = setOf();
        final int agent3Id = addSingleAlphabetAgent(manager, setOf(), setOf(actionBunch), noBunches, alphabet, null, null, null, "s", pluralRule);
        final int agent2Id = addSingleAlphabetAgent(manager, setOf(actionBunch), setOf(arVerbBunch), noBunches, alphabet, null, null, "ar", "o", nominalizationRule);
        final int agent1Id = addSingleAlphabetAgent(manager, setOf(arVerbBunch), sourceBunchSet, noBunches, alphabet, null, null, "ar", "ar", null);

        return new Add3ChainedAgentsResult(agent1Id, agent2Id, agent3Id);
    }

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> Add3ChainedAgentsResult add3ChainedAgents(AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager,
            AlphabetId alphabet, BunchId arVerbBunch, BunchId actionBunch,
            RuleId nominalizationRule, RuleId pluralRule) {

        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        return add3ChainedAgents(manager, alphabet, noBunches, arVerbBunch, actionBunch, nominalizationRule, pluralRule);
    }

    @Test
    default void testAdd3ChainedAgents() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationConcept = actionConcept + 1;
        final int pluralConcept = nominalizationConcept + 1;
        final int singConcept = pluralConcept + 1;

        final AcceptationId acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId actionBunch = conceptAsBunchId(actionConcept);
        final RuleId nominalizationRule = conceptAsRuleId(nominalizationConcept);
        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet,
                arVerbBunch, actionBunch, nominalizationRule, pluralRule);

        final ImmutableIntPairMap nominalizationRuledConcepts = manager.findRuledConceptsByRule(nominalizationRule);
        assertContainsOnly(singConcept, nominalizationRuledConcepts);
        final int nounRuledConcept = nominalizationRuledConcepts.keyAt(0);

        final ImmutableIntPairMap pluralRuledConcepts = manager.findRuledConceptsByRule(pluralRule);
        assertContainsOnly(nounRuledConcept, pluralRuledConcepts);
        final int pluralRuledConcept = pluralRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = manager.getAgentProcessedMap(addAgentsResult.agent2Id);
        assertSize(1, processedMap);
        assertEquals(acceptation, processedMap.keyAt(0));
        final AcceptationId nounRuledAcceptation = processedMap.valueAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> pluralProcessedMap = manager.getAgentProcessedMap(addAgentsResult.agent3Id);
        assertSize(1, pluralProcessedMap);
        assertEquals(nounRuledAcceptation, pluralProcessedMap.keyAt(0));
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
    default void testRemoveDynamicAcceptationsWhenRemovingAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int studentConcept = manager.getMaxConcept() + 1;
        final AcceptationId studentAcceptation = addSimpleAcceptation(manager, alphabet, studentConcept, "alumno");

        final int femenineConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineConcept, "femenino");

        final RuleId femenineRule = conceptAsRuleId(femenineConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(), setOf(), setOf(), alphabet, null, null, "o", "a", femenineRule);

        final AcceptationId femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
        final ImmutableList<CorrelationId> correlationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        manager.removeAgent(agentId);

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(femaleStudentAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(femaleStudentAcceptation));
        for (CorrelationId correlationId : correlationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }
    }

    @Test
    default void testRemoveDynamicAcceptationsWhenAcceptationFromSourceBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int studentConcept = manager.getMaxConcept() + 1;
        final AcceptationId studentAcceptation = addSimpleAcceptation(manager, alphabet, studentConcept, "alumno");

        final int sourceConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, sourceConcept, "mis palabras");

        final BunchId sourceBunch = conceptAsBunchId(sourceConcept);
        manager.addAcceptationInBunch(sourceBunch, studentAcceptation);

        final int femenineConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineConcept, "femenino");

        final RuleId femenineRule = conceptAsRuleId(femenineConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(), setOf(sourceBunch), setOf(), alphabet, null, null, "o", "a", femenineRule);

        final AcceptationId femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
        final ImmutableList<CorrelationId> studentCorrelationArray = manager.getAcceptationCorrelationArray(studentAcceptation);
        final ImmutableList<CorrelationId> femaleStudentCorrelationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        assertTrue(manager.removeAcceptation(studentAcceptation));

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(femaleStudentAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(studentAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(femaleStudentAcceptation));
        for (CorrelationId correlationId : studentCorrelationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }
        for (CorrelationId correlationId : femaleStudentCorrelationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }
    }

    @Test
    default void testRemoveUnusedBunchSetsWhenRemovingAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int studentConcept = manager.getMaxConcept() + 1;
        final AcceptationId studentAcceptation = addSimpleAcceptation(manager, alphabet, studentConcept, "alumno");

        final int femenineConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineConcept, "femenino");

        final int targetConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept, "destino");

        final int sourceConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, sourceConcept, "origen");

        final BunchId sourceBunch = conceptAsBunchId(sourceConcept);
        manager.addAcceptationInBunch(sourceBunch, studentAcceptation);

        final int diffConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, diffConcept, "diferencial");

        final BunchId targetBunch = conceptAsBunchId(targetConcept);
        final BunchId diffBunch = conceptAsBunchId(diffConcept);
        final RuleId femenineRule = conceptAsRuleId(femenineConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(targetBunch), setOf(sourceBunch), setOf(diffBunch), alphabet, null, null, "o", "a", femenineRule);

        final AcceptationId femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
        final ImmutableList<CorrelationId> correlationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        final AgentRegister<CorrelationId, RuleId> agentRegister = manager.getAgentRegister(agentId);
        manager.removeAgent(agentId);

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(femaleStudentAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(femaleStudentAcceptation));
        for (CorrelationId correlationId : correlationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }

        assertEmpty(manager.getBunchSet(agentRegister.targetBunchSetId));
        assertEmpty(manager.getBunchSet(agentRegister.sourceBunchSetId));
        assertEmpty(manager.getBunchSet(agentRegister.diffBunchSetId));
    }

    @Test
    default void testRemoveChainedAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationConcept = actionConcept + 1;
        final int pluralConcept = nominalizationConcept + 1;
        final int singConcept = pluralConcept + 1;

        final AcceptationId acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId actionBunch = conceptAsBunchId(actionConcept);
        final RuleId nominalizationRule = conceptAsRuleId(nominalizationConcept);
        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet,
                arVerbBunch, actionBunch, nominalizationRule, pluralRule);

        final AcceptationId nounAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent2Id, acceptation);
        final AcceptationId pluralAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent3Id, nounAcceptation);
        assertNotEquals(0, pluralAcceptation);

        manager.removeAgent(addAgentsResult.agent1Id);
        assertNoRuledAcceptationsPresentForChainedAgents(manager, addAgentsResult);

        assertEquals(singConcept, manager.conceptFromAcceptation(acceptation));
        assertEquals(0, manager.conceptFromAcceptation(nounAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(pluralAcceptation));

        assertEmpty(manager.getAcceptationsInBunch(arVerbBunch));
        assertEmpty(manager.getAcceptationsInBunch(actionBunch));
    }

    @Test
    default void testRemoveAcceptationWithChainedAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationConcept = actionConcept + 1;
        final int pluralConcept = nominalizationConcept + 1;
        final int singConcept = pluralConcept + 1;

        final AcceptationId acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId actionBunch = conceptAsBunchId(actionConcept);
        final RuleId nominalizationRule = conceptAsRuleId(nominalizationConcept);
        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet, arVerbBunch, actionBunch, nominalizationRule, pluralRule);

        final AcceptationId nounAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent2Id, acceptation);
        final AcceptationId pluralAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent3Id, nounAcceptation);
        assertNotEquals(0, pluralAcceptation);

        manager.removeAcceptation(acceptation);
        assertNoRuledAcceptationsPresentForChainedAgents(manager, addAgentsResult);

        assertEquals(0, manager.conceptFromAcceptation(acceptation));
        assertEquals(0, manager.conceptFromAcceptation(nounAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(pluralAcceptation));

        assertEmpty(manager.getAcceptationsInBunch(arVerbBunch));
        assertEmpty(manager.getAcceptationsInBunch(actionBunch));
    }

    @Test
    default void testRemoveAcceptationWithBunchChainedAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int verbConcept = manager.getMaxConcept() + 1;
        final int arVerbConcept = verbConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationConcept = actionConcept + 1;
        final int pluralConcept = nominalizationConcept + 1;
        final int singConcept = pluralConcept + 1;

        final AcceptationId acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        manager.addAcceptationInBunch(verbBunch, acceptation);

        final ImmutableSet<BunchId> sourceBunches = new ImmutableHashSet.Builder<BunchId>().add(verbBunch).build();
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId actionBunch = conceptAsBunchId(actionConcept);
        final RuleId nominalizationRule = conceptAsRuleId(nominalizationConcept);
        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet, sourceBunches, arVerbBunch, actionBunch, nominalizationRule, pluralRule);

        final AcceptationId nounAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent2Id, acceptation);
        final AcceptationId pluralAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent3Id, nounAcceptation);
        assertNotEquals(0, pluralAcceptation);

        manager.removeAcceptationFromBunch(verbBunch, acceptation);
        assertNoRuledAcceptationsPresentForChainedAgents(manager, addAgentsResult);

        assertEquals(singConcept, manager.conceptFromAcceptation(acceptation));
        assertEquals(0, manager.conceptFromAcceptation(nounAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(pluralAcceptation));

        assertEmpty(manager.getAcceptationsInBunch(verbBunch));
        assertEmpty(manager.getAcceptationsInBunch(arVerbBunch));
        assertEmpty(manager.getAcceptationsInBunch(actionBunch));
    }

    @Test
    default void testReadAllMatchingBunchesForSingleMatching() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbArConcept = gerund + 1;
        final int verbErConcept = verbArConcept + 1;

        addSimpleAcceptation(manager, alphabet, verbArConcept, "verbo ar");
        addSimpleAcceptation(manager, alphabet, verbErConcept, "verbo er");

        final ImmutableSet<BunchId> diffBunches = setOf();
        final BunchId verbArBunch = conceptAsBunchId(verbArConcept);
        final BunchId verbErBunch = conceptAsBunchId(verbErConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbArConcept = gerund + 1;
        final int verbErConcept = verbArConcept + 1;
        final int sustantivable = verbErConcept + 1;
        final int noun = sustantivable + 1;

        addSimpleAcceptation(manager, alphabet, verbArConcept, "verbo ar");
        addSimpleAcceptation(manager, alphabet, verbErConcept, "verbo er");
        addSimpleAcceptation(manager, alphabet, sustantivable, "sustantivable");

        final ImmutableSet<BunchId> diffBunches = setOf();
        final BunchId verbArBunch = conceptAsBunchId(verbArConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(manager, setOf(), setOf(verbArBunch), diffBunches, alphabet, null, null, "ar", "ando", gerundRule);

        final BunchId verbErBunch = conceptAsBunchId(verbErConcept);
        addSingleAlphabetAgent(manager, setOf(), setOf(verbErBunch), diffBunches, alphabet, null, null, "er", "iendo", gerundRule);

        final BunchId nounBunch = conceptAsBunchId(noun);
        final BunchId sustantivableBunch = conceptAsBunchId(sustantivable);
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

    @Test
    default void testUpdateCorrelationArrayForAcceptationWithRuleAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int concept = manager.getMaxConcept() + 1;
        final int gerund = concept + 1;
        final int firstConjugationVerbConcept = gerund + 1;

        final AcceptationId acceptationId = addSimpleAcceptation(manager, alphabet, concept, "contar");
        final BunchId firstConjugationVerbBunch = conceptAsBunchId(firstConjugationVerbConcept);
        manager.addAcceptationInBunch(firstConjugationVerbBunch, acceptationId);

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(manager, setOf(), setOf(firstConjugationVerbBunch), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        updateAcceptationSimpleCorrelationArray(manager, alphabet, acceptationId, "cantar");

        final int ruledConcept = manager.findRuledConcept(gerundRule, concept);
        assertNotEquals(concept, ruledConcept);

        final AcceptationId ruledAcceptation = getSingleValue(manager.findAcceptationsByConcept(ruledConcept));
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(ruledAcceptation).toImmutable());
        assertEquals("cantando", manager.readAcceptationMainText(ruledAcceptation));
        assertEquals(acceptationId, manager.getStaticAcceptationFromDynamic(ruledAcceptation));
    }

    @Test
    default void testUnableToRemoveAcceptationsWhenTheyAreUniqueAgentSourceOrTargetBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int verbConcept = manager.getMaxConcept() + 1;
        final int firstConjugationVerbConcept = verbConcept + 1;
        final int singConcept = firstConjugationVerbConcept + 1;

        final AcceptationId verbAcc = addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");
        final AcceptationId firstConjugationVerbAcc = addSimpleAcceptation(manager, alphabet, firstConjugationVerbConcept, "verbo ar");
        final AcceptationId singAcc = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

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
    default void testMultipleAgentsTargetingSameBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int verbConcept = manager.getMaxConcept() + 1;
        final AcceptationId verbAcc = addSimpleAcceptation(manager, alphabet, verbConcept, "desconfiar");

        final int myConcept = manager.getMaxConcept() + 1;
        final String myBunchText = "palabaras raras";
        addSimpleAcceptation(manager, alphabet, myConcept, myBunchText);

        final ImmutableSet<BunchId> emptyBunchSet = setOf();
        final BunchId myBunch = conceptAsBunchId(myConcept);
        final int desAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, null);
        final int arAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", null);

        assertContainsOnly(desAgent, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc));
    }

    @Test
    default void testAcceptationAddedInBunchBeforeAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int bedConcept = manager.getMaxConcept() + 1;
        final AcceptationId bedAcc = addSimpleAcceptation(manager, alphabet, bedConcept, "cama");

        final int verbConcept1 = manager.getMaxConcept() + 1;
        final AcceptationId verbAcc1 = addSimpleAcceptation(manager, alphabet, verbConcept1, "confiar");

        final int verbConcept2 = manager.getMaxConcept() + 1;
        final AcceptationId verbAcc2 = addSimpleAcceptation(manager, alphabet, verbConcept2, "desconfiar");

        final int myConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myConcept, "palabras raras");

        final BunchId myBunch = conceptAsBunchId(myConcept);
        assertTrue(manager.addAcceptationInBunch(myBunch, bedAcc));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc1));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc2));

        final ImmutableSet<BunchId> emptyBunchSet = setOf();
        final int desAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, null);
        final int arAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", null);

        assertContainsOnly(0, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, bedAcc));
        assertContainsOnly(0, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc1));
        assertContainsOnly(0, desAgent, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc2));
    }

    @Test
    default void testAcceptationAddedInBunchAfterAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int bedConcept = manager.getMaxConcept() + 1;
        final AcceptationId bedAcc = addSimpleAcceptation(manager, alphabet, bedConcept, "cama");

        final int verbConcept1 = manager.getMaxConcept() + 1;
        final AcceptationId verbAcc1 = addSimpleAcceptation(manager, alphabet, verbConcept1, "confiar");

        final int verbConcept2 = manager.getMaxConcept() + 1;
        final AcceptationId verbAcc2 = addSimpleAcceptation(manager, alphabet, verbConcept2, "desconfiar");

        final int myConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myConcept, "palabras raras");

        final ImmutableSet<BunchId> emptyBunchSet = setOf();
        final BunchId myBunch = conceptAsBunchId(myConcept);
        final int desAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, null);
        final int arAgent = addSingleAlphabetAgent(manager, setOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", null);

        assertTrue(manager.addAcceptationInBunch(myBunch, bedAcc));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc1));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc2));

        assertContainsOnly(0, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, bedAcc));
        assertContainsOnly(0, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc1));
        assertContainsOnly(0, desAgent, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc2));
    }

    @Test
    default void testUpdateAgentTargetForNoChainedAgentWithoutRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId erVerbBunch = conceptAsBunchId(erVerbConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(arVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
    }

    @Test
    default void testIncludeExtraTargetForNoChainedAgentWithoutRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId erVerbBunch = conceptAsBunchId(erVerbConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch, erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(arVerbBunch, erVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
    }

    @Test
    default void testRemoveExtraTargetForNoChainedAgentWithoutRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId erVerbBunch = conceptAsBunchId(erVerbConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch, erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(arVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
    }

    @Test
    default void testIncludeExtraTargetForNoChainedAgentWithRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int gerund = manager.getMaxConcept() + 1;
        final int singConcept = gerund + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId erVerbBunch = conceptAsBunchId(erVerbConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        final int agentId = addSingleAlphabetAgent(manager, setOf(erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch, erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));

        assertEmpty(manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
        assertContainsOnly(arVerbBunch, erVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(dynamicAcceptation));
    }

    @Test
    default void testRemoveExtraTargetForNoChainedAgentWithRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int gerund = manager.getMaxConcept() + 1;
        final int singConcept = gerund + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId erVerbBunch = conceptAsBunchId(erVerbConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        final int agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch, erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));

        assertEmpty(manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
        assertContainsOnly(arVerbBunch, manager.findBunchesWhereAcceptationIsIncluded(dynamicAcceptation));
    }

    @Test
    default void testUpdateAgentTargetForChainedAgentWithoutRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId erVerbBunch = conceptAsBunchId(erVerbConcept);
        final int agent1Id = addSingleAlphabetAgent(manager, setOf(erVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        final int agent2Id = addSingleAlphabetAgent(manager, setOf(), setOf(arVerbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule);
        assertTrue(updateSingleAlphabetAgent(manager, agent1Id, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));
    }

    @Test
    default void testRemoveAgentTargetFromSecondChainedAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int recentWordsConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, recentWordsConcept, "palabras recientes");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        addSingleAlphabetAgent(manager, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final BunchId recentWordsBunch = conceptAsBunchId(recentWordsConcept);
        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        final int agent2Id = addSingleAlphabetAgent(manager, setOf(recentWordsBunch), setOf(arVerbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule);
        assertTrue(updateSingleAlphabetAgent(manager, agent2Id, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));
        assertEmpty(manager.getAcceptationsInBunch(recentWordsBunch));
    }

    @Test
    default void testIncludeAgentTargetToSecondChainedAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int recentWordsConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, recentWordsConcept, "palabras recientes");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        addSingleAlphabetAgent(manager, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        final int agent2Id = addSingleAlphabetAgent(manager, setOf(), setOf(arVerbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule);
        final BunchId recentWordsBunch = conceptAsBunchId(recentWordsConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agent2Id, setOf(recentWordsBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final AcceptationId dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));
        assertContainsOnly(dynamicAcceptation, manager.getAcceptationsInBunch(recentWordsBunch));
    }

    @Test
    default void testIncludeAgentSourceBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1Concept, "vocabulario del capítulo 1");

        final BunchId chapter1Bunch = conceptAsBunchId(chapter1Concept);
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final int allVocabularyConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabularyConcept, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId allVocabularyBunch = conceptAsBunchId(allVocabularyConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), setOf(chapter1Bunch), noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testRemoveAgentSourceBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final AcceptationId touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1Concept, "vocabulario del capítulo 1");

        final BunchId chapter1Bunch = conceptAsBunchId(chapter1Concept);
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final int allVocabularyConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabularyConcept, "vocabulario");

        final BunchId allVocabularyBunch = conceptAsBunchId(allVocabularyConcept);
        final ImmutableSet<BunchId> noBunches = setOf();
        final int agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), setOf(chapter1Bunch), noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        assertContainsOnly(singAcceptation, touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testChangeAgentSourceBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final AcceptationId touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1Concept, "vocabulario del capítulo 1");
        final BunchId chapter1Bunch = conceptAsBunchId(chapter1Concept);
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final int chapter2Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2Concept, "vocabulario del capítulo 2");
        final BunchId chapter2Bunch = conceptAsBunchId(chapter2Concept);
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final int allVocabularyConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabularyConcept, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId allVocabularyBunch = conceptAsBunchId(allVocabularyConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), setOf(chapter1Bunch), noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), setOf(chapter2Bunch), noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testIncludeExtraSourceBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final AcceptationId touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int passConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, passConcept, "pasar");

        final int chapter1Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1Concept, "vocabulario del capítulo 1");

        final BunchId chapter1Bunch = conceptAsBunchId(chapter1Concept);
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final int chapter2Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2Concept, "vocabulario del capítulo 2");

        final BunchId chapter2Bunch = conceptAsBunchId(chapter2Concept);
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final int allVocabularyConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabularyConcept, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId allVocabularyBunch = conceptAsBunchId(allVocabularyConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), setOf(chapter1Bunch), noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), setOf(chapter1Bunch, chapter2Bunch), noBunches, alphabet, null, null, "ar", "ar", null));

        final ImmutableSet<AcceptationId> expectedAcceptations = setOf(singAcceptation, touchAcceptation);
        assertEqualSet(expectedAcceptations, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testRemoveOneSourceBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final AcceptationId touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int passConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, passConcept, "pasar");

        final int chapter1Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1Concept, "vocabulario del capítulo 1");

        final BunchId chapter1Bunch = conceptAsBunchId(chapter1Concept);
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final int chapter2Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2Concept, "vocabulario del capítulo 2");

        final BunchId chapter2Bunch = conceptAsBunchId(chapter2Concept);
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final int allVocabularyConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabularyConcept, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId allVocabularyBunch = conceptAsBunchId(allVocabularyConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), setOf(chapter1Bunch, chapter2Bunch), noBunches, alphabet, null, null, "ar", "ar", null);

        final ImmutableSet<BunchId> chapter1Only = new ImmutableHashSet.Builder<BunchId>().add(chapter1Bunch).build();
        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter1Only, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testIncludeAgentDiffBunchMatchingSource() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final AcceptationId touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1Concept, "vocabulario del capítulo 1");

        final BunchId chapter1Bunch = conceptAsBunchId(chapter1Concept);
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final int chapter2Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2Concept, "vocabulario del capítulo 2");

        final BunchId chapter2Bunch = conceptAsBunchId(chapter2Concept);
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, singAcceptation));
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final int allVocabularyConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabularyConcept, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableSet<BunchId> chapter2Only = setOf(chapter2Bunch);

        final BunchId allVocabularyBunch = conceptAsBunchId(allVocabularyConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter2Only, setOf(chapter1Bunch), alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testRemoveAgentDiffBunchMatchingSource() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final AcceptationId touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1Concept, "vocabulario del capítulo 1");

        final BunchId chapter1Bunch = conceptAsBunchId(chapter1Concept);
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final int chapter2Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2Concept, "vocabulario del capítulo 2");
        final BunchId chapter2Bunch = conceptAsBunchId(chapter2Concept);
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, singAcceptation));
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final int allVocabularyConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabularyConcept, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableSet<BunchId> chapter2Only = setOf(chapter2Bunch);
        final BunchId allVocabularyBunch = conceptAsBunchId(allVocabularyConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), chapter2Only, setOf(chapter1Bunch), alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", null));

        final ImmutableSet<AcceptationId> expectedAcceptations = setOf(singAcceptation, touchAcceptation);
        assertEqualSet(expectedAcceptations, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testIncludeAgentDiffBunchNoMatchingSource() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final AcceptationId touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1Concept, "vocabulario del capítulo 1");

        final BunchId chapter1Bunch = conceptAsBunchId(chapter1Concept);
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final int chapter2Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2Concept, "vocabulario del capítulo 2");

        final BunchId chapter2Bunch = conceptAsBunchId(chapter2Concept);
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final int allVocabularyConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabularyConcept, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableSet<BunchId> chapter2Only = setOf(chapter2Bunch);

        final BunchId allVocabularyBunch = conceptAsBunchId(allVocabularyConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter2Only, setOf(chapter1Bunch), alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testRemoveAgentDiffBunchNoMatchingSource() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final AcceptationId touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1Concept, "vocabulario del capítulo 1");

        final BunchId chapter1Bunch = conceptAsBunchId(chapter1Concept);
        assertTrue(manager.addAcceptationInBunch(chapter1Bunch, singAcceptation));

        final int chapter2Concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2Concept, "vocabulario del capítulo 2");

        final BunchId chapter2Bunch = conceptAsBunchId(chapter2Concept);
        assertTrue(manager.addAcceptationInBunch(chapter2Bunch, touchAcceptation));

        final int allVocabularyConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabularyConcept, "vocabulario a repasar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableSet<BunchId> chapter1Only = setOf(chapter1Bunch);
        final ImmutableSet<BunchId> chapter2Only = setOf(chapter2Bunch);

        final BunchId allVocabularyBunch = conceptAsBunchId(allVocabularyConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(allVocabularyBunch), chapter2Only, chapter1Only, alphabet, null, null, "ar", "ar", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(allVocabularyBunch), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabularyBunch, agentId));
    }

    @Test
    default void testChangeAgentEndMatcherAndAdder() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int eatConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, eatConcept, "comer");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "er", "er", null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(arVerbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(arVerbBunch, agentId));
    }

    @Test
    default void testChangeAgentStartMatcherAndAdder() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int trustConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, trustConcept, "confiar");

        final int untrustConcept = manager.getMaxConcept() + 1;
        final AcceptationId untrustAcceptation = addSimpleAcceptation(manager, alphabet, untrustConcept, "desconfiar");

        final int unVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, unVerbConcept, "verbo que comienza por des");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId unVerbBunch = conceptAsBunchId(unVerbConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(unVerbBunch), noBunches, noBunches, alphabet, "con", "con", null, null, null);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(unVerbBunch), noBunches, noBunches, alphabet, "des", "des", null, null, null));
        assertContainsOnly(untrustAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(unVerbBunch, agentId));
    }

    @Test
    default void testChangeRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int pastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, pastConcept, "pasado");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final RuleId pastRule = conceptAsRuleId(pastConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", pastRule);

        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testChangeAdder() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "aba", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testChangeAdderForMultipleAcceptations() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int cryConcept = manager.getMaxConcept() + 1;
        final AcceptationId cryAcceptation = addSimpleAcceptation(manager, alphabet, cryConcept, "llorar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "aba", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
        assertOnlyOneMorphology(manager, cryAcceptation, alphabet, "llorando", gerundRule);
    }

    @Test
    default void testChangeAdderAndRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int pastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, pastConcept, "pasado");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final RuleId pastRule = conceptAsRuleId(pastConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "aba", pastRule);

        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testAddAdderAndRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetConcept, "mi lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId myTargetBunch = conceptAsBunchId(myTargetConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final MorphologyResult<AcceptationId, RuleId> morphology = getSingleValue(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertEquals("cantando", morphology.text);
        assertContainsOnly(gerundRule, morphology.rules);
        assertContainsOnly(morphology.dynamicAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
    }

    @Test
    default void testAddAdderAndRuleForMultipleTargetBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetConcept, "mi lista");

        final int myTargetConcept2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetConcept2, "mi otra lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId myTargetBunch = conceptAsBunchId(myTargetConcept);
        final BunchId myTargetBunch2 = conceptAsBunchId(myTargetConcept2);
        final int agentId = addSingleAlphabetAgent(manager, setOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ar", null);

        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final MorphologyResult<AcceptationId, RuleId> morphology = getSingleValue(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertEquals("cantando", morphology.text);
        assertContainsOnly(gerundRule, morphology.rules);
        assertContainsOnly(morphology.dynamicAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
        assertContainsOnly(morphology.dynamicAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch2, agentId));
    }

    @Test
    default void testRemoveAdderAndRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetConcept, "mi lista");

        final int myTargetConcept2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetConcept2, "mi otra lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId myTargetBunch = conceptAsBunchId(myTargetConcept);
        final BunchId myTargetBunch2 = conceptAsBunchId(myTargetConcept2);
        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch2, agentId));
    }

    @Test
    default void testRemoveAdderAndRuleForMultipleTargetBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetConcept, "mi lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final BunchId myTargetBunch = conceptAsBunchId(myTargetConcept);
        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, setOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
    }

    @Test
    default void testUpdateCorrelationArrayMatchingAgentBefore() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        assertNotNull(addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar (sin instrumentos)"));
        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
    }

    @Test
    default void testUpdateCorrelationArrayMatchingAgentAfter() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar (sin instrumentos)");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableSet<BunchId> noBunches = setOf();
        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        assertNotNull(addSingleAlphabetAgent(manager, setOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar"));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testUpdateCorrelationArrayMatchingChainedAgentBefore() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final int verbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        assertNotNull(addSingleAlphabetAgent(manager, setOf(verbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        assertNotNull(addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar (sin instrumentos)"));
        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
    }

    @Test
    default void testUpdateCorrelationArrayMatchingChainedAgentAfter() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar (sin instrumentos)");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final int verbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final ImmutableSet<BunchId> noBunches = setOf();
        assertNotNull(addSingleAlphabetAgent(manager, setOf(verbBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", null));

        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        assertNotNull(addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar"));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundRule);
    }

    @Test
    default void testAgentWithJustEndAdderForAcceptationFromOtherLanguage() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(new MemoryDatabase());
        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, esAlphabet, singConcept, "cantar");

        final int myConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, myConcept, "palabras");

        final BunchId myBunch = conceptAsBunchId(myConcept);
        manager.addAcceptationInBunch(myBunch, singAcceptation);

        final int verbalitationConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, verbalitationConcept, "verbalización");

        final RuleId verbalitationRule = conceptAsRuleId(verbalitationConcept);
        final int agentId = addSingleAlphabetAgent(manager, setOf(), setOf(myBunch), setOf(), jaAlphabet, null, null, null, "する", verbalitationRule);
        assertEmpty(manager.getAgentProcessedMap(agentId));

        final int studyConcept = manager.getMaxConcept() + 1;
        final AcceptationId studyAcceptation = addSimpleAcceptation(manager, jaAlphabet, studyConcept, "べんきょう");
        manager.addAcceptationInBunch(myBunch, studyAcceptation);
        assertContainsOnly(studyAcceptation, manager.getAgentProcessedMap(agentId).keySet());
    }

    @Test
    default void testAvoidDuplicatedBunchSetsWhenSharingConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int guyConcept = manager.getMaxConcept() + 1;
        final AcceptationId guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final int personConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final int targetConcept1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept1, "mis palabras 1");

        final int targetConcept2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept2, "mis palabras 2");

        final BunchId targetBunch1 = conceptAsBunchId(targetConcept1);
        final BunchId guyBunch = conceptAsBunchId(guyConcept);
        final int agent1 = addSingleAlphabetAgent(manager, setOf(targetBunch1), setOf(guyBunch), setOf(), alphabet, null, null, null, null, null);

        final BunchId targetBunch2 = conceptAsBunchId(targetConcept2);
        final BunchId personBunch = conceptAsBunchId(personConcept);
        final int agent2 = addSingleAlphabetAgent(manager, setOf(targetBunch2), setOf(personBunch), setOf(), alphabet, null, null, null, null, null);

        final int oldSetId = manager.getAgentRegister(agent2).sourceBunchSetId;
        assertTrue(manager.shareConcept(guyAcc, personConcept));

        final int setId = manager.getAgentRegister(agent1).sourceBunchSetId;
        assertEquals(setId, manager.getAgentRegister(agent2).sourceBunchSetId);

        assertContainsOnly(guyBunch, manager.getBunchSet(setId));
        assertEmpty(manager.getBunchSet(oldSetId));
    }

    @Test
    default void testReuseBunchSetWhenSharingConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int guyConcept = manager.getMaxConcept() + 1;
        final AcceptationId guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final int personConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final int targetConcept1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept1, "mis palabras 1");

        final int targetConcept2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept2, "mis palabras 2");

        final BunchId targetBunch1 = conceptAsBunchId(targetConcept1);
        final BunchId guyBunch = conceptAsBunchId(guyConcept);
        final int agent1 = addSingleAlphabetAgent(manager, setOf(targetBunch1), setOf(guyBunch), setOf(), alphabet, null, null, null, null, null);

        final BunchId targetBunch2 = conceptAsBunchId(targetConcept2);
        final BunchId personBunch = conceptAsBunchId(personConcept);
        final int agent2 = addSingleAlphabetAgent(manager, setOf(targetBunch2), setOf(guyBunch, personBunch), setOf(), alphabet, null, null, null, null, null);

        final int setId = manager.getAgentRegister(agent1).sourceBunchSetId;
        final int oldAgent2SetId = manager.getAgentRegister(agent2).sourceBunchSetId;
        assertTrue(manager.shareConcept(guyAcc, personConcept));

        assertEquals(setId, manager.getAgentRegister(agent1).sourceBunchSetId);
        assertEquals(setId, manager.getAgentRegister(agent2).sourceBunchSetId);
        assertContainsOnly(guyBunch, manager.getBunchSet(setId));
        assertEmpty(manager.getBunchSet(oldAgent2SetId));
    }

    @Test
    default void testAvoidDuplicatedBunchInBunchSetWhenSharingConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int guyConcept = manager.getMaxConcept() + 1;
        final AcceptationId guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final int personConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final int targetConcept1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept1, "mis palabras 1");

        final int targetConcept2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept2, "mis palabras 2");

        final BunchId targetBunch2 = conceptAsBunchId(targetConcept2);
        final BunchId guyBunch = conceptAsBunchId(guyConcept);
        final BunchId personBunch = conceptAsBunchId(personConcept);
        final int agent = addSingleAlphabetAgent(manager, setOf(targetBunch2), setOf(guyBunch, personBunch), setOf(), alphabet, null, null, null, null, null);

        final int setId = manager.getAgentRegister(agent).sourceBunchSetId;
        assertTrue(manager.shareConcept(guyAcc, personConcept));

        assertEquals(setId, manager.getAgentRegister(agent).sourceBunchSetId);

        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .select(table.getBunchColumnIndex());
        assertContainsOnly(guyConcept, db.select(query).mapToInt(row -> row.get(0).toInt()).toList());
    }

    @Test
    default void testAvoidDuplicatedRuledConceptsAndAcceptationsWhenSharingConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int jumpConcept = manager.getMaxConcept() + 1;
        final AcceptationId jumpAcc = addSimpleAcceptation(manager, alphabet, jumpConcept, "saltar");

        final int jumpConcept2 = manager.getMaxConcept() + 1;
        final AcceptationId jumpAcc2 = addSimpleAcceptation(manager, alphabet, jumpConcept2, "brincar");

        final int bunchConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, bunchConcept, "mi lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final int continuousConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, continuousConcept, "continuo");

        final BunchId bunchBunch = conceptAsBunchId(bunchConcept);
        final RuleId gerundRule = conceptAsRuleId(gerundConcept);
        final int agent1 = addSingleAlphabetAgent(manager, setOf(bunchBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final RuleId continuousRule = conceptAsRuleId(continuousConcept);
        final int agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(bunchBunch), setOf(), alphabet, null, "estoy ", null, null, continuousRule);

        final AcceptationId ruledJumpAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, jumpAcc);
        final AcceptationId ruledJumpAcc2 = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, jumpAcc2);
        assertNotEquals(ruledJumpAcc, ruledJumpAcc2);

        final AcceptationId ruled2JumpAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, ruledJumpAcc);
        final AcceptationId ruled2JumpAcc2 = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, ruledJumpAcc2);
        assertNotEquals(ruled2JumpAcc, ruled2JumpAcc2);

        final int ruledJumpConcept = manager.conceptFromAcceptation(ruledJumpAcc);
        final int ruledJumpConcept2 = manager.conceptFromAcceptation(ruledJumpAcc2);
        assertNotEquals(ruledJumpConcept, ruledJumpConcept2);

        final int ruled2JumpConcept = manager.conceptFromAcceptation(ruled2JumpAcc);
        final int ruled2JumpConcept2 = manager.conceptFromAcceptation(ruled2JumpAcc2);
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
    default void testUpdateAgentRuleFromAlreadyUsedRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int getWetConcept = manager.getMaxConcept() + 1;
        final AcceptationId getWetEsAcc = addSimpleAcceptation(manager, esAlphabet, getWetConcept, "mojarse");
        final AcceptationId getWetJaAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "濡れる");

        final int esVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, esVerbConcept, "Verbo español");

        final BunchId esVerbBunch = conceptAsBunchId(esVerbConcept);
        manager.addAcceptationInBunch(esVerbBunch, getWetEsAcc);

        final int jaVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, jaVerbConcept, "Verbo japonés");

        final BunchId jaVerbBunch = conceptAsBunchId(jaVerbConcept);
        manager.addAcceptationInBunch(jaVerbBunch, getWetJaAcc);

        final int badCausalConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, badCausalConcept, "causalización");

        final int causalConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, causalConcept, "causal");

        final RuleId causalRule = conceptAsRuleId(causalConcept);
        final int esAgent = addSingleAlphabetAgent(manager, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", causalRule);

        final int jaAgent = addSingleAlphabetAgent(manager, setOf(), setOf(jaVerbBunch), setOf(), jaAlphabet, null, null, "る", "させる", causalRule);

        final RuleId badCausalRule = conceptAsRuleId(badCausalConcept);
        updateSingleAlphabetAgent(manager, esAgent, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", badCausalRule);

        final AcceptationId makeWetEsAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(esAgent, getWetEsAcc);
        assertSinglePair(esAlphabet, "hacer que se moje", manager.getAcceptationTexts(makeWetEsAcc));

        final AcceptationId makeWetJaAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(jaAgent, getWetJaAcc);
        assertSinglePair(jaAlphabet, "濡れさせる", manager.getAcceptationTexts(makeWetJaAcc));

        final int makeWetJaConcept = manager.conceptFromAcceptation(makeWetJaAcc);
        assertNotEquals(makeWetJaConcept, manager.conceptFromAcceptation(makeWetEsAcc));
    }

    @Test
    default void testUpdateAgentRuleToAlreadyUsedRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int getWetConcept = manager.getMaxConcept() + 1;
        final AcceptationId getWetEsAcc = addSimpleAcceptation(manager, esAlphabet, getWetConcept, "mojarse");
        final AcceptationId getWetJaAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "濡れる");

        final int esVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, esVerbConcept, "Verbo español");

        final BunchId esVerbBunch = conceptAsBunchId(esVerbConcept);
        manager.addAcceptationInBunch(esVerbBunch, getWetEsAcc);

        final int jaVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, jaVerbConcept, "Verbo japonés");

        final BunchId jaVerbBunch = conceptAsBunchId(jaVerbConcept);
        manager.addAcceptationInBunch(jaVerbBunch, getWetJaAcc);

        final int badCausalConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, badCausalConcept, "causalización");

        final RuleId badCausalRule = conceptAsRuleId(badCausalConcept);
        final int esAgent = addSingleAlphabetAgent(manager, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", badCausalRule);

        final int causalConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, causalConcept, "causal");

        final RuleId causalRule = conceptAsRuleId(causalConcept);
        final int jaAgent = addSingleAlphabetAgent(manager, setOf(), setOf(jaVerbBunch), setOf(), jaAlphabet, null, null, "る", "させる", causalRule);

        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(badCausalRule));

        updateSingleAlphabetAgent(manager, esAgent, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", causalRule);

        final AcceptationId makeWetEsAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(esAgent, getWetEsAcc);
        assertSinglePair(esAlphabet, "hacer que se moje", manager.getAcceptationTexts(makeWetEsAcc));

        final AcceptationId makeWetJaAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(jaAgent, getWetJaAcc);
        assertSinglePair(jaAlphabet, "濡れさせる", manager.getAcceptationTexts(makeWetJaAcc));

        final int makeWetConcept = manager.conceptFromAcceptation(makeWetJaAcc);
        assertEquals(makeWetConcept, manager.conceptFromAcceptation(makeWetEsAcc));
        assertEmpty(manager.findRuledConceptsByRule(badCausalRule));
        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(causalRule));
    }

    @Test
    default void testUpdateAgentRuleBetweenUsedRules() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int getWetConcept = manager.getMaxConcept() + 1;
        final AcceptationId getWetEsAcc = addSimpleAcceptation(manager, esAlphabet, getWetConcept, "mojarse");
        final AcceptationId getWetJaAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "濡れる");
        final AcceptationId getWetNaruAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "びしょびしょになる");

        final int esVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, esVerbConcept, "Verbo español");

        final BunchId esVerbBunch = conceptAsBunchId(esVerbConcept);
        manager.addAcceptationInBunch(esVerbBunch, getWetEsAcc);

        final int jaVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, jaVerbConcept, "Verbo japonés");

        final BunchId jaVerbBunch = conceptAsBunchId(jaVerbConcept);
        manager.addAcceptationInBunch(jaVerbBunch, getWetJaAcc);

        final int naruVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, naruVerbConcept, "Adjetivo con naru");

        final BunchId naruVerbBunch = conceptAsBunchId(naruVerbConcept);
        manager.addAcceptationInBunch(naruVerbBunch, getWetNaruAcc);

        final int badCausalConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, badCausalConcept, "causalización");

        final RuleId badCausalRule = conceptAsRuleId(badCausalConcept);
        final int esAgent = addSingleAlphabetAgent(manager, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", badCausalRule);

        final int causalConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, causalConcept, "causal");

        final RuleId causalRule = conceptAsRuleId(causalConcept);
        final int jaAgent = addSingleAlphabetAgent(manager, setOf(), setOf(jaVerbBunch), setOf(), jaAlphabet, null, null, "る", "させる", causalRule);

        final int naruAgent = addSingleAlphabetAgent(manager, setOf(), setOf(naruVerbBunch), setOf(), jaAlphabet, null, null, "になる", "にする", badCausalRule);

        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(badCausalRule));

        updateSingleAlphabetAgent(manager, esAgent, setOf(), setOf(esVerbBunch), setOf(), esAlphabet, null, "hacer que se ", "arse", "e", causalRule);

        final AcceptationId makeWetEsAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(esAgent, getWetEsAcc);
        assertSinglePair(esAlphabet, "hacer que se moje", manager.getAcceptationTexts(makeWetEsAcc));

        final AcceptationId makeWetJaAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(jaAgent, getWetJaAcc);
        assertSinglePair(jaAlphabet, "濡れさせる", manager.getAcceptationTexts(makeWetJaAcc));

        final AcceptationId makeWetNaruAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(naruAgent, getWetNaruAcc);
        assertSinglePair(jaAlphabet, "びしょびしょにする", manager.getAcceptationTexts(makeWetNaruAcc));

        final int makeWetConcept = manager.conceptFromAcceptation(makeWetJaAcc);
        assertEquals(makeWetConcept, manager.conceptFromAcceptation(makeWetEsAcc));
        assertNotEquals(makeWetConcept, manager.conceptFromAcceptation(makeWetNaruAcc));
        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(badCausalRule));
        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(causalRule));
    }

    @Test
    default void testLinkRuleConcepts() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int dieConcept = manager.getMaxConcept() + 1;
        final AcceptationId dieJaAcc = addSimpleAcceptation(manager, jaAlphabet, dieConcept, "死ぬ");

        final int verbConcept = manager.getMaxConcept() + 1;
        final AcceptationId verbJaAcc = addSimpleAcceptation(manager, jaAlphabet, verbConcept, "動詞");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        manager.addAcceptationInBunch(verbBunch, dieJaAcc);

        final int accidentalConcept = manager.getMaxConcept() + 1;
        final AcceptationId accidentalAcc = addSimpleAcceptation(manager, esAlphabet, accidentalConcept, "accidental");

        final RuleId accidentalRule = conceptAsRuleId(accidentalConcept);
        final int agent1 = addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぬ", "んでしまう", accidentalRule);

        final int accidentalConcept2 = manager.getMaxConcept() + 1;
        final AcceptationId accidentalAcc2 = addSimpleAcceptation(manager, esAlphabet, accidentalConcept2, "accidental informal");

        final RuleId accidentalRule2 = conceptAsRuleId(accidentalConcept2);
        final int agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぬ", "んじゃう", accidentalRule2);

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

        final int accidentalDieConcept = manager.conceptFromAcceptation(accidentalDieAcc);
        assertEquals(accidentalDieConcept, manager.conceptFromAcceptation(accidentalDieAcc2));
    }

    @Test
    default void testLinkRuleToNonRuleConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int dieConcept = manager.getMaxConcept() + 1;
        final AcceptationId dieJaAcc = addSimpleAcceptation(manager, jaAlphabet, dieConcept, "死ぬ");

        final int verbConcept = manager.getMaxConcept() + 1;
        final AcceptationId verbJaAcc = addSimpleAcceptation(manager, jaAlphabet, verbConcept, "動詞");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        manager.addAcceptationInBunch(verbBunch, dieJaAcc);

        final int accidentalConcept = manager.getMaxConcept() + 1;
        final AcceptationId accidentalAcc = addSimpleAcceptation(manager, esAlphabet, accidentalConcept, "accidental");

        final RuleId accidentalRule = conceptAsRuleId(accidentalConcept);
        final int agent1 = addSingleAlphabetAgent(manager, setOf(), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぬ", "んでしまう", accidentalRule);

        final int accidentalConcept2 = manager.getMaxConcept() + 1;
        final AcceptationId accidentalAcc2 = addSimpleAcceptation(manager, esAlphabet, accidentalConcept2, "accidental informal");

        assertTrue(manager.shareConcept(accidentalAcc2, accidentalConcept));

        final AcceptationId accidentalDieAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, dieJaAcc);
        assertNotEquals(dieJaAcc, accidentalDieAcc);
        assertNotEquals(verbJaAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc2, accidentalDieAcc);

        assertEmpty(manager.findRuledConceptsByRule(accidentalRule));
        final RuleId accidentalRule2 = conceptAsRuleId(accidentalConcept2);
        final ImmutableIntPairMap ruledConcepts = manager.findRuledConceptsByRule(accidentalRule2);
        assertContainsOnly(dieConcept, ruledConcepts);

        final int accidentalDieConcept = ruledConcepts.keyAt(0);
        assertEquals(accidentalDieConcept, manager.conceptFromAcceptation(accidentalDieAcc));
    }

    @Test
    default void testChangeAdderInFirstChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int callConcept = manager.getMaxConcept() + 1;
        final AcceptationId callJaAcc = addSimpleAcceptation(manager, jaAlphabet, callConcept, "呼ぶ");

        final int verbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, jaAlphabet, verbConcept, "動詞");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        manager.addAcceptationInBunch(verbBunch, callJaAcc);

        final int accidentalConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, accidentalConcept, "accidental");

        final int canBePastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, canBePastConcept, "puede ser pasado");

        final BunchId canBePastBunch = conceptAsBunchId(canBePastConcept);
        final RuleId accidentalRule = conceptAsRuleId(accidentalConcept);
        final int agent1 = addSingleAlphabetAgent(manager, setOf(canBePastBunch), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぶ", "じまう", accidentalRule);

        final int pastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, pastConcept, "pasado");

        final RuleId pastRule = conceptAsRuleId(pastConcept);
        final int agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(canBePastBunch), setOf(), jaAlphabet, null, null, "う", "った", pastRule);

        assertTrue(updateSingleAlphabetAgent(manager, agent1, setOf(canBePastBunch), setOf(verbBunch), setOf(), jaAlphabet, null, null, "ぶ", "んじまう", accidentalRule));

        final ImmutableMap<AcceptationId, AcceptationId> ruledAccs1 = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent1);
        assertContainsOnly(callJaAcc, ruledAccs1);

        final ImmutableMap<AcceptationId, AcceptationId> ruledAccs2 = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent2);
        assertContainsOnly(ruledAccs1.keyAt(0), ruledAccs2);

        final AcceptationId callAccidentalPastAcc = ruledAccs2.keyAt(0);
        assertSinglePair(jaAlphabet, "呼んじまった", manager.getAcceptationTexts(callAccidentalPastAcc));
    }

    @Test
    default void testChangeAdderInFirstChainedAgentWhenPickedSampleAcceptationForSecondIsOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final AcceptationId singJaAcc = addSimpleAcceptation(manager, jaAlphabet, singConcept, "歌う");

        final int callConcept = manager.getMaxConcept() + 1;
        final AcceptationId callJaAcc = addSimpleAcceptation(manager, jaAlphabet, callConcept, "呼ぶ");

        final int buVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, jaAlphabet, buVerbConcept, "verbo acabado en ぶ");

        final BunchId buVerbBunch = conceptAsBunchId(buVerbConcept);
        manager.addAcceptationInBunch(buVerbBunch, callJaAcc);

        final int accidentalConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, accidentalConcept, "accidental");

        final int uVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, uVerbConcept, "verbo acabado en う");

        final BunchId uVerbBunch = conceptAsBunchId(uVerbConcept);
        manager.addAcceptationInBunch(uVerbBunch, singJaAcc);

        final RuleId accidentalRule = conceptAsRuleId(accidentalConcept);
        final int agent1 = addSingleAlphabetAgent(manager, setOf(uVerbBunch), setOf(buVerbBunch), setOf(), jaAlphabet, null, null, "ぶ", "じまう", accidentalRule);

        final int pastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, pastConcept, "pasado");

        final RuleId pastRule = conceptAsRuleId(pastConcept);
        final int agent2 = addSingleAlphabetAgent(manager, setOf(), setOf(uVerbBunch), setOf(), jaAlphabet, null, null, "う", "った", pastRule);

        assertTrue(updateSingleAlphabetAgent(manager, agent1, setOf(uVerbBunch), setOf(buVerbBunch), setOf(), jaAlphabet, null, null, "ぶ", "んじまう", accidentalRule));

        final ImmutableMap<AcceptationId, AcceptationId> ruledAccs1 = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent1);
        assertContainsOnly(callJaAcc, ruledAccs1);

        final ImmutableMap<AcceptationId, AcceptationId> ruledAccs2 = findRuledAcceptationsByAgent(db, getAcceptationIdManager(), agent2);
        assertContainsOnly(ruledAccs1.keyAt(0), singJaAcc, ruledAccs2);

        final AcceptationId callAccidentalPastAcc = ruledAccs2.keyAt(equal(ruledAccs2.valueAt(0), singJaAcc)? 1 : 0);
        assertSinglePair(jaAlphabet, "呼んじまった", manager.getAcceptationTexts(callAccidentalPastAcc));
    }
}
