package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableList;
import sword.collections.List;
import sword.collections.MutableIntPairMap;
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
import static sword.collections.IntKeyMapTestUtils.assertSinglePair;
import static sword.collections.IntPairMapTestUtils.assertSinglePair;
import static sword.collections.IntSetTestUtils.assertEqualSet;
import static sword.collections.IntSetTestUtils.intSetOf;
import static sword.collections.IntTraversableTestUtils.assertContainsOnly;
import static sword.collections.IntTraversableTestUtils.getSingleValue;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.updateAcceptationSimpleCorrelationArray;
import static sword.langbook3.android.db.BunchesManagerTest.addSpanishSingAcceptation;

/**
 * Include all test related to all responsibilities of a AgentsManager.
 *
 * AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> responsibilities include all responsibilities from BunchesManager, and include the following ones:
 * <li>Bunch sets</li>
 * <li>Rules</li>
 * <li>Ruled concepts</li>
 * <li>Ruled acceptations</li>
 * <li>Agents</li>
 */
interface AgentsManagerTest<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> extends BunchesManagerTest<LanguageId, AlphabetId, CorrelationId> {

    @Override
    AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> createManager(MemoryDatabase db);

    static ImmutableIntPairMap findRuledAcceptationsByAgent(DbExporter.Database db, int agent) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(ruledAccs)
                .where(ruledAccs.getAgentColumnIndex(), agent)
                .select(ruledAccs.getIdColumnIndex(), ruledAccs.getAcceptationColumnIndex());

        final MutableIntPairMap map = MutableIntPairMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                map.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return map.toImmutable();
    }

    static <LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> Integer addSingleAlphabetAgent(AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, int rule) {
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

    static <LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> boolean updateSingleAlphabetAgent(AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager, int agentId, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, int rule) {
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

    static <LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> void assertOnlyOneMorphology(AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager, int staticAcceptation, AlphabetId preferredAlphabet, String expectedText, int expectedRule) {
        final MorphologyResult morphology = getSingleValue(manager.readMorphologiesFromAcceptation(staticAcceptation, preferredAlphabet).morphologies);
        assertEquals(expectedText, morphology.text);
        assertContainsOnly(expectedRule, morphology.rules);
    }

    static <LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> void assertNoRuledAcceptationsPresentForChainedAgents(AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager, Add3ChainedAgentsResult result) {
        assertEmpty(manager.getAgentProcessedMap(result.agent1Id));
        assertEmpty(manager.getAgentProcessedMap(result.agent2Id));
        assertEmpty(manager.getAgentProcessedMap(result.agent3Id));
    }

    @Test
    default void testAddAgentApplyingRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        final int acceptation = addSimpleAcceptation(manager, alphabet, concept, "cantar");
        assertTrue(manager.addAcceptationInBunch(verbConcept, acceptation));

        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final int ruledConcept = manager.findRuledConcept(gerund, concept);
        final int ruledAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, acceptation);
        assertEquals(ruledConcept, manager.conceptFromAcceptation(ruledAcceptation));

        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(ruledAcceptation));
        assertEquals(acceptation, manager.getStaticAcceptationFromDynamic(ruledAcceptation));
        assertEquals("cantando", manager.readAcceptationMainText(ruledAcceptation));
    }

    @Test
    default void testAddAgentComposingBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int erVerbConcept = arVerbConcept + 1;
        final int verbConcept = erVerbConcept + 1;
        final int singConcept = erVerbConcept + 1;
        final int coughtConcept = singConcept + 1;

        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        assertTrue(manager.addAcceptationInBunch(verbConcept, singAcceptation));

        final int coughtAcceptation = addSimpleAcceptation(manager, alphabet, coughtConcept, "toser");
        assertTrue(manager.addAcceptationInBunch(verbConcept, coughtAcceptation));

        final int agentId = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept), intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ar", 0);

        assertContainsOnly(singAcceptation, coughtAcceptation, manager.getAcceptationsInBunch(verbConcept));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(arVerbConcept, agentId));
    }

    @Test
    default void testAddAgentCopyingToTwoBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int erVerbConcept = arVerbConcept + 1;
        final int actionConcept = erVerbConcept + 1;
        final int verbConcept = actionConcept + 1;
        final int singConcept = erVerbConcept + 1;
        final int coughtConcept = singConcept + 1;

        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        assertTrue(manager.addAcceptationInBunch(verbConcept, singAcceptation));

        final int coughtAcceptation = addSimpleAcceptation(manager, alphabet, coughtConcept, "toser");
        assertTrue(manager.addAcceptationInBunch(verbConcept, coughtAcceptation));

        final int agentId = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept, actionConcept), intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ar", 0);

        assertContainsOnly(singAcceptation, coughtAcceptation, manager.getAcceptationsInBunch(verbConcept));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(arVerbConcept, agentId));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(actionConcept, agentId));
    }

    default void checkAdd2ChainedAgents(boolean reversedAdditionOrder, boolean addExtraMiddleTargetBunch) {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int arVerbConcept = verbConcept + 1;
        final int extraBunch = arVerbConcept + 1;
        final int singConcept = extraBunch + 1;

        final int acceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        assertTrue(manager.addAcceptationInBunch(verbConcept, acceptation));

        final ImmutableCorrelation<AlphabetId> nullCorrelation = new ImmutableCorrelation.Builder<AlphabetId>().build();
        final ImmutableCorrelation<AlphabetId> matcher = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ar")
                .build();
        final ImmutableCorrelation<AlphabetId> adder = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet arVerbBunchSet = intSetOf(arVerbConcept);
        final ImmutableIntSet verbBunchSet = intSetOf(verbConcept);
        final ImmutableIntSet diffBunches = intSetOf();
        final ImmutableIntSet firstTargetBunches = addExtraMiddleTargetBunch? arVerbBunchSet.add(extraBunch) :
                arVerbBunchSet;

        final int agent2Id;
        if (reversedAdditionOrder) {
            agent2Id = manager.addAgent(intSetOf(), arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);
            manager.addAgent(firstTargetBunches, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);
        }
        else {
            manager.addAgent(firstTargetBunches, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);
            agent2Id = manager.addAgent(intSetOf(), arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);
        }

        final int ruledConcept = manager.findRuledConcept(gerund, singConcept);
        final int ruledAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, acceptation);
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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int maleStudentConcept = manager.getMaxConcept() + 1;
        final int maleStudentAcc = addSimpleAcceptation(manager, alphabet, maleStudentConcept, "alumno");

        final int femenineRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineRule, "femenino");

        final int pluralRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineRule, "plural");

        final int feminableWordsBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, feminableWordsBunch, "feminizable");

        final int pluralableWordsBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, pluralableWordsBunch, "pluralizable");

        final int agent1 = addSingleAlphabetAgent(manager, intSetOf(pluralableWordsBunch), intSetOf(feminableWordsBunch), intSetOf(), alphabet, null, null, "o", "a", femenineRule);

        final int agent2 = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(pluralableWordsBunch), intSetOf(), alphabet, null, null, null, "s", pluralRule);

        manager.addAcceptationInBunch(feminableWordsBunch, maleStudentAcc);

        final int femaleStudentConcept = manager.findRuledConcept(femenineRule, maleStudentConcept);
        final int femaleStudentAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, maleStudentAcc);
        assertEquals(femaleStudentConcept, manager.conceptFromAcceptation(femaleStudentAcc));
        assertSinglePair(alphabet, "alumna", manager.getAcceptationTexts(femaleStudentAcc));

        final int pluralFemaleStudentConcept = manager.findRuledConcept(pluralRule, femaleStudentConcept);
        final int pluralFemaleStudentAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, femaleStudentAcc);
        assertEquals(pluralFemaleStudentConcept, manager.conceptFromAcceptation(pluralFemaleStudentAcc));
        assertSinglePair(alphabet, "alumnas", manager.getAcceptationTexts(pluralFemaleStudentAcc));
    }

    default void checkAdd2ChainedAgentsFirstWithoutSource(boolean reversedAdditionOrder, boolean acceptationBeforeAgents) {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int bunchConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, bunchConcept, "pluralizable sustituyendo ón por ones");

        int acceptation = 0;
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

        final ImmutableIntSet middleBunchSet = intSetOf(bunchConcept);
        final ImmutableIntSet noBunchSet = intSetOf();

        final int pluralConcept;
        if (reversedAdditionOrder) {
            pluralConcept = manager.getMaxConcept() + 1;
            assertNotNull(manager.addAgent(intSetOf(), middleBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, adder, pluralConcept));
            assertNotNull(manager.addAgent(middleBunchSet, noBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, matcher, 0));
        }
        else {
            assertNotNull(manager.addAgent(middleBunchSet, noBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, matcher, 0));
            pluralConcept = manager.getMaxConcept() + 1;
            assertNotNull(manager.addAgent(intSetOf(), middleBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, adder, pluralConcept));
        }

        if (!acceptationBeforeAgents) {
            final int songConcept = manager.getMaxConcept() + 1;
            acceptation = addSimpleAcceptation(manager, alphabet, songConcept, "canción");
        }

        assertOnlyOneMorphology(manager, acceptation, alphabet, "canciones", pluralConcept);
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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int arEndingNounConcept = arVerbConcept + 1;
        final int singConcept = arEndingNounConcept + 1;
        final int palateConcept = singConcept + 1;

        final ImmutableIntSet sourceBunches = intSetOf();
        final ImmutableIntSet diffBunches = intSetOf(arEndingNounConcept);

        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        final int palateAcceptation;
        final int agentId;
        if (addAgentBeforeAcceptations) {
            agentId = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept), sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", 0);
            palateAcceptation = addSimpleAcceptation(manager, alphabet, palateConcept, "paladar");
            manager.addAcceptationInBunch(arEndingNounConcept, palateAcceptation);
        }
        else {
            palateAcceptation = addSimpleAcceptation(manager, alphabet, palateConcept, "paladar");
            manager.addAcceptationInBunch(arEndingNounConcept, palateAcceptation);
            agentId = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept), sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", 0);
        }

        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunch(arVerbConcept));
        assertContainsOnly(agentId, manager.findAllAgentsThatIncludedAcceptationInBunch(arVerbConcept, singAcceptation));
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

    static <LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> Add3ChainedAgentsResult add3ChainedAgents(
            AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager,
            AlphabetId alphabet, ImmutableIntSet sourceBunchSet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntSet noBunches = intSetOf();
        final int agent3Id = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(actionConcept), noBunches, alphabet, null, null, null, "s", pluralRule);
        final int agent2Id = addSingleAlphabetAgent(manager, intSetOf(actionConcept), intSetOf(arVerbConcept), noBunches, alphabet, null, null, "ar", "o", nominalizationRule);
        final int agent1Id = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept), sourceBunchSet, noBunches, alphabet, null, null, "ar", "ar", 0);

        return new Add3ChainedAgentsResult(agent1Id, agent2Id, agent3Id);
    }

    static <LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> Add3ChainedAgentsResult add3ChainedAgents(AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager,
            AlphabetId alphabet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();
        return add3ChainedAgents(manager, alphabet, noBunches, arVerbConcept, actionConcept, nominalizationRule, pluralRule);
    }

    @Test
    default void testAdd3ChainedAgents() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet,
                arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        final ImmutableIntPairMap nominalizationRuledConcepts = manager.findRuledConceptsByRule(nominalizationRule);
        assertContainsOnly(singConcept, nominalizationRuledConcepts);
        final int nounRuledConcept = nominalizationRuledConcepts.keyAt(0);

        final ImmutableIntPairMap pluralRuledConcepts = manager.findRuledConceptsByRule(pluralRule);
        assertContainsOnly(nounRuledConcept, pluralRuledConcepts);
        final int pluralRuledConcept = pluralRuledConcepts.keyAt(0);

        final ImmutableIntPairMap processedMap = manager.getAgentProcessedMap(addAgentsResult.agent2Id);
        assertSize(1, processedMap);
        assertEquals(acceptation, processedMap.keyAt(0));
        final int nounRuledAcceptation = processedMap.valueAt(0);

        final ImmutableIntPairMap pluralProcessedMap = manager.getAgentProcessedMap(addAgentsResult.agent3Id);
        assertSize(1, pluralProcessedMap);
        assertEquals(nounRuledAcceptation, pluralProcessedMap.keyAt(0));
        final int pluralRuledAcceptation = pluralProcessedMap.valueAt(0);

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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int studentConcept = manager.getMaxConcept() + 1;
        final int studentAcceptation = addSimpleAcceptation(manager, alphabet, studentConcept, "alumno");

        final int femenineRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineRule, "femenino");

        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(), intSetOf(), alphabet, null, null, "o", "a", femenineRule);

        final int femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int studentConcept = manager.getMaxConcept() + 1;
        final int studentAcceptation = addSimpleAcceptation(manager, alphabet, studentConcept, "alumno");

        final int sourceBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, sourceBunch, "mis palabras");
        manager.addAcceptationInBunch(sourceBunch, studentAcceptation);

        final int femenineRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineRule, "femenino");

        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(sourceBunch), intSetOf(), alphabet, null, null, "o", "a", femenineRule);

        final int femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int studentConcept = manager.getMaxConcept() + 1;
        final int studentAcceptation = addSimpleAcceptation(manager, alphabet, studentConcept, "alumno");

        final int femenineRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineRule, "femenino");

        final int targetBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetBunch, "destino");

        final int sourceBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, sourceBunch, "origen");
        manager.addAcceptationInBunch(sourceBunch, studentAcceptation);

        final int diffBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, diffBunch, "diferencial");

        final int agentId = addSingleAlphabetAgent(manager, intSetOf(targetBunch), intSetOf(sourceBunch), intSetOf(diffBunch), alphabet, null, null, "o", "a", femenineRule);

        final int femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
        final ImmutableList<CorrelationId> correlationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        final AgentRegister<CorrelationId> agentRegister = manager.getAgentRegister(agentId);
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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet,
                arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        final int nounAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent2Id, acceptation);
        final int pluralAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent3Id, nounAcceptation);
        assertNotEquals(0, pluralAcceptation);

        manager.removeAgent(addAgentsResult.agent1Id);
        assertNoRuledAcceptationsPresentForChainedAgents(manager, addAgentsResult);

        assertEquals(singConcept, manager.conceptFromAcceptation(acceptation));
        assertEquals(0, manager.conceptFromAcceptation(nounAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(pluralAcceptation));

        assertEmpty(manager.getAcceptationsInBunch(arVerbConcept));
        assertEmpty(manager.getAcceptationsInBunch(actionConcept));
    }

    @Test
    default void testRemoveAcceptationWithChainedAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet, arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        final int nounAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent2Id, acceptation);
        final int pluralAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent3Id, nounAcceptation);
        assertNotEquals(0, pluralAcceptation);

        manager.removeAcceptation(acceptation);
        assertNoRuledAcceptationsPresentForChainedAgents(manager, addAgentsResult);

        assertEquals(0, manager.conceptFromAcceptation(acceptation));
        assertEquals(0, manager.conceptFromAcceptation(nounAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(pluralAcceptation));

        assertEmpty(manager.getAcceptationsInBunch(arVerbConcept));
        assertEmpty(manager.getAcceptationsInBunch(actionConcept));
    }

    @Test
    default void testRemoveAcceptationWithBunchChainedAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int verbConcept = manager.getMaxConcept() + 1;
        final int arVerbConcept = verbConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        manager.addAcceptationInBunch(verbConcept, acceptation);

        final ImmutableIntSet sourceBunches = new ImmutableIntSetCreator().add(verbConcept).build();
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet, sourceBunches, arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        final int nounAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent2Id, acceptation);
        final int pluralAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(addAgentsResult.agent3Id, nounAcceptation);
        assertNotEquals(0, pluralAcceptation);

        manager.removeAcceptationFromBunch(verbConcept, acceptation);
        assertNoRuledAcceptationsPresentForChainedAgents(manager, addAgentsResult);

        assertEquals(singConcept, manager.conceptFromAcceptation(acceptation));
        assertEquals(0, manager.conceptFromAcceptation(nounAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(pluralAcceptation));

        assertEmpty(manager.getAcceptationsInBunch(verbConcept));
        assertEmpty(manager.getAcceptationsInBunch(arVerbConcept));
        assertEmpty(manager.getAcceptationsInBunch(actionConcept));
    }

    @Test
    default void testReadAllMatchingBunchesForSingleMatching() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbArConcept = gerund + 1;
        final int verbErConcept = verbArConcept + 1;

        addSimpleAcceptation(manager, alphabet, verbArConcept, "verbo ar");
        addSimpleAcceptation(manager, alphabet, verbErConcept, "verbo er");

        final ImmutableIntSet diffBunches = intSetOf();
        addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbArConcept), diffBunches, alphabet, null, null, "ar", "ando", gerund);

        addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbErConcept), diffBunches, alphabet, null, null, "er", "iendo", gerund);

        ImmutableCorrelation<AlphabetId> texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "provocar").build();
        assertSinglePair(verbArConcept, "verbo ar", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "comer").build();
        assertSinglePair(verbErConcept, "verbo er", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "dormir").build();
        assertEmpty(manager.readAllMatchingBunches(texts, alphabet));
    }

    @Test
    default void testReadAllMatchingBunchesForMultipleMatching() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbArConcept = gerund + 1;
        final int verbErConcept = verbArConcept + 1;
        final int sustantivable = verbErConcept + 1;
        final int noun = sustantivable + 1;

        addSimpleAcceptation(manager, alphabet, verbArConcept, "verbo ar");
        addSimpleAcceptation(manager, alphabet, verbErConcept, "verbo er");
        addSimpleAcceptation(manager, alphabet, sustantivable, "sustantivable");

        final ImmutableIntSet diffBunches = intSetOf();
        addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbArConcept), diffBunches, alphabet, null, null, "ar", "ando", gerund);

        addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbErConcept), diffBunches, alphabet, null, null, "er", "iendo", gerund);

        addSingleAlphabetAgent(manager, intSetOf(noun), intSetOf(sustantivable), diffBunches, alphabet, null, null, "ar", "ación", gerund);

        ImmutableCorrelation<AlphabetId> texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "provocar").build();
        final ImmutableIntKeyMap<String> bunches = manager.readAllMatchingBunches(texts, alphabet);
        assertContainsOnly(verbArConcept, sustantivable, bunches.keySet());
        assertEquals("verbo ar", bunches.get(verbArConcept));
        assertEquals("sustantivable", bunches.get(sustantivable));

        texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "comer").build();
        assertSinglePair(verbErConcept, "verbo er", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, "dormir").build();
        assertEmpty(manager.readAllMatchingBunches(texts, alphabet));
    }

    @Test
    default void testUpdateCorrelationArrayForAcceptationWithRuleAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int concept = manager.getMaxConcept() + 1;
        final int gerundRule = concept + 1;
        final int firstConjugationVerbBunch = gerundRule + 1;

        final int acceptationId = addSimpleAcceptation(manager, alphabet, concept, "contar");
        manager.addAcceptationInBunch(firstConjugationVerbBunch, acceptationId);

        addSingleAlphabetAgent(manager, intSetOf(), intSetOf(firstConjugationVerbBunch), intSetOf(), alphabet, null, null, "ar", "ando", gerundRule);

        updateAcceptationSimpleCorrelationArray(manager, alphabet, acceptationId, "cantar");

        final int ruledConcept = manager.findRuledConcept(gerundRule, concept);
        assertNotEquals(concept, ruledConcept);

        final int ruledAcceptation = getSingleValue(manager.findAcceptationsByConcept(ruledConcept));
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(ruledAcceptation).toImmutable());
        assertEquals("cantando", manager.readAcceptationMainText(ruledAcceptation));
        assertEquals(acceptationId, manager.getStaticAcceptationFromDynamic(ruledAcceptation));
    }

    @Test
    default void testUnableToRemoveAcceptationsWhenTheyAreUniqueAgentSourceOrTargetBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int verbConcept = manager.getMaxConcept() + 1;
        final int firstConjugationVerbConcept = verbConcept + 1;
        final int singConcept = firstConjugationVerbConcept + 1;

        final int verbAcc = addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");
        final int firstConjugationVerbAcc = addSimpleAcceptation(manager, alphabet, firstConjugationVerbConcept, "verbo ar");
        final int singAcc = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        assertTrue(manager.addAcceptationInBunch(verbConcept, singAcc));

        assertNotEquals(null, addSingleAlphabetAgent(manager, intSetOf(firstConjugationVerbConcept), intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ar", 0));

        assertFalse(manager.removeAcceptation(verbAcc));
        assertFalse(manager.removeAcceptation(firstConjugationVerbAcc));

        assertEquals("verbo", manager.getAcceptationTexts(verbAcc).get(alphabet));
        assertEquals("verbo ar", manager.getAcceptationTexts(firstConjugationVerbAcc).get(alphabet));
    }

    @Test
    default void testMultipleAgentsTargetingSameBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int verbConcept = manager.getMaxConcept() + 1;
        final int verbAcc = addSimpleAcceptation(manager, alphabet, verbConcept, "desconfiar");

        final int myBunch = manager.getMaxConcept() + 1;
        final String myBunchText = "palabaras raras";
        addSimpleAcceptation(manager, alphabet, myBunch, myBunchText);

        final ImmutableIntSet emptyBunchSet = intSetOf();
        final int desAgent = addSingleAlphabetAgent(manager, intSetOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, 0);
        final int arAgent = addSingleAlphabetAgent(manager, intSetOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", 0);

        assertContainsOnly(desAgent, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc));
    }

    @Test
    default void testAcceptationAddedInBunchBeforeAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int bedConcept = manager.getMaxConcept() + 1;
        final int bedAcc = addSimpleAcceptation(manager, alphabet, bedConcept, "cama");

        final int verbConcept1 = manager.getMaxConcept() + 1;
        final int verbAcc1 = addSimpleAcceptation(manager, alphabet, verbConcept1, "confiar");

        final int verbConcept2 = manager.getMaxConcept() + 1;
        final int verbAcc2 = addSimpleAcceptation(manager, alphabet, verbConcept2, "desconfiar");

        final int myBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myBunch, "palabras raras");

        assertTrue(manager.addAcceptationInBunch(myBunch, bedAcc));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc1));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc2));

        final ImmutableIntSet emptyBunchSet = intSetOf();
        final int desAgent = addSingleAlphabetAgent(manager, intSetOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, 0);
        final int arAgent = addSingleAlphabetAgent(manager, intSetOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", 0);

        assertContainsOnly(0, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, bedAcc));
        assertContainsOnly(0, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc1));
        assertContainsOnly(0, desAgent, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc2));
    }

    @Test
    default void testAcceptationAddedInBunchAfterAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int bedConcept = manager.getMaxConcept() + 1;
        final int bedAcc = addSimpleAcceptation(manager, alphabet, bedConcept, "cama");

        final int verbConcept1 = manager.getMaxConcept() + 1;
        final int verbAcc1 = addSimpleAcceptation(manager, alphabet, verbConcept1, "confiar");

        final int verbConcept2 = manager.getMaxConcept() + 1;
        final int verbAcc2 = addSimpleAcceptation(manager, alphabet, verbConcept2, "desconfiar");

        final int myBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myBunch, "palabras raras");

        final ImmutableIntSet emptyBunchSet = intSetOf();
        final int desAgent = addSingleAlphabetAgent(manager, intSetOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, 0);
        final int arAgent = addSingleAlphabetAgent(manager, intSetOf(myBunch), emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", 0);

        assertTrue(manager.addAcceptationInBunch(myBunch, bedAcc));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc1));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc2));

        assertContainsOnly(0, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, bedAcc));
        assertContainsOnly(0, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc1));
        assertContainsOnly(0, desAgent, arAgent, manager.findAllAgentsThatIncludedAcceptationInBunch(myBunch, verbAcc2));
    }

    @Test
    default void testUpdateAgentTargetForNoChainedAgentWithoutRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(erVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(arVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(arVerbConcept, manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
    }

    @Test
    default void testIncludeExtraTargetForNoChainedAgentWithoutRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(erVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(arVerbConcept, erVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(arVerbConcept, erVerbConcept, manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
    }

    @Test
    default void testRemoveExtraTargetForNoChainedAgentWithoutRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept, erVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(arVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(arVerbConcept, manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
    }

    @Test
    default void testIncludeExtraTargetForNoChainedAgentWithRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int gerundRule = manager.getMaxConcept() + 1;
        final int singConcept = gerundRule + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(erVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(arVerbConcept, erVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final int dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));

        assertEmpty(manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
        assertContainsOnly(arVerbConcept, erVerbConcept, manager.findBunchesWhereAcceptationIsIncluded(dynamicAcceptation));
    }

    @Test
    default void testRemoveExtraTargetForNoChainedAgentWithRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int gerundRule = manager.getMaxConcept() + 1;
        final int singConcept = gerundRule + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept, erVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(arVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundRule));

        final int dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));

        assertEmpty(manager.findBunchesWhereAcceptationIsIncluded(singAcceptation));
        assertContainsOnly(arVerbConcept, manager.findBunchesWhereAcceptationIsIncluded(dynamicAcceptation));
    }

    @Test
    default void testUpdateAgentTargetForChainedAgentWithoutRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final ImmutableIntSet noBunches = intSetOf();
        final int agent1Id = addSingleAlphabetAgent(manager, intSetOf(erVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        final int agent2Id = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(arVerbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agent1Id, intSetOf(arVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        final int dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));
    }

    @Test
    default void testRemoveAgentTargetFromSecondChainedAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int recentWordsConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, recentWordsConcept, "palabras recientes");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final ImmutableIntSet noBunches = intSetOf();
        addSingleAlphabetAgent(manager, intSetOf(arVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        final int agent2Id = addSingleAlphabetAgent(manager, intSetOf(recentWordsConcept), intSetOf(arVerbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agent2Id, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        final int dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));
        assertEmpty(manager.getAcceptationsInBunch(recentWordsConcept));
    }

    @Test
    default void testIncludeAgentTargetToSecondChainedAgent() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int recentWordsConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, recentWordsConcept, "palabras recientes");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final ImmutableIntSet noBunches = intSetOf();
        addSingleAlphabetAgent(manager, intSetOf(arVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        final int agent2Id = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(arVerbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agent2Id, intSetOf(recentWordsConcept), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        final int dynamicAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2Id, singAcceptation);
        assertSinglePair(alphabet, "cantando", manager.getAcceptationTexts(dynamicAcceptation));
        assertContainsOnly(dynamicAcceptation, manager.getAcceptationsInBunch(recentWordsConcept));
    }

    @Test
    default void testIncludeAgentSourceBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(allVocabulary), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(allVocabulary), intSetOf(chapter1), noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabulary, agentId));
    }

    @Test
    default void testRemoveAgentSourceBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(allVocabulary), intSetOf(chapter1), noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(allVocabulary), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        assertContainsOnly(singAcceptation, touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabulary, agentId));
    }

    @Test
    default void testChangeAgentSourceBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(allVocabulary), intSetOf(chapter1), noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(allVocabulary), intSetOf(chapter2), noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabulary, agentId));
    }

    @Test
    default void testIncludeExtraSourceBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int passConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, passConcept, "pasar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(allVocabulary), intSetOf(chapter1), noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(allVocabulary), intSetOf(chapter1, chapter2), noBunches, alphabet, null, null, "ar", "ar", 0));

        final ImmutableIntSet expectedAcceptations = intSetOf(singAcceptation, touchAcceptation);
        assertEqualSet(expectedAcceptations, manager.getAcceptationsInBunchByBunchAndAgent(allVocabulary, agentId));
    }

    @Test
    default void testRemoveOneSourceBunch() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int passConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, passConcept, "pasar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(allVocabulary), intSetOf(chapter1, chapter2), noBunches, alphabet, null, null, "ar", "ar", 0);

        final ImmutableIntSet chapter1Only = new ImmutableIntSetCreator().add(chapter1).build();
        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(allVocabulary), chapter1Only, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabulary, agentId));
    }

    @Test
    default void testIncludeAgentDiffBunchMatchingSource() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, singAcceptation));
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntSet chapter2Only = intSetOf(chapter2);
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(allVocabulary), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(allVocabulary), chapter2Only, intSetOf(chapter1), alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabulary, agentId));
    }

    @Test
    default void testRemoveAgentDiffBunchMatchingSource() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, singAcceptation));
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntSet chapter2Only = intSetOf(chapter2);
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(allVocabulary), chapter2Only, intSetOf(chapter1), alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(allVocabulary), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", 0));

        final ImmutableIntSet expectedAcceptations = intSetOf(singAcceptation, touchAcceptation);
        assertEqualSet(expectedAcceptations, manager.getAcceptationsInBunchByBunchAndAgent(allVocabulary, agentId));
    }

    @Test
    default void testIncludeAgentDiffBunchNoMatchingSource() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntSet chapter2Only = intSetOf(chapter2);
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(allVocabulary), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(allVocabulary), chapter2Only, intSetOf(chapter1), alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabulary, agentId));
    }

    @Test
    default void testRemoveAgentDiffBunchNoMatchingSource() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntSet chapter1Only = intSetOf(chapter1);
        final ImmutableIntSet chapter2Only = intSetOf(chapter2);
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(allVocabulary), chapter2Only, chapter1Only, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(allVocabulary), chapter2Only, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(touchAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(allVocabulary, agentId));
    }

    @Test
    default void testChangeAgentEndMatcherAndAdder() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int eatConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, eatConcept, "comer");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept), noBunches, noBunches, alphabet, null, null, "er", "er", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(arVerbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(arVerbConcept, agentId));
    }

    @Test
    default void testChangeAgentStartMatcherAndAdder() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int trustConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, trustConcept, "confiar");

        final int untrustConcept = manager.getMaxConcept() + 1;
        final int untrustAcceptation = addSimpleAcceptation(manager, alphabet, untrustConcept, "desconfiar");

        final int unVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, unVerbConcept, "verbo que comienza por des");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(unVerbConcept), noBunches, noBunches, alphabet, "con", "con", null, null, 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(unVerbConcept), noBunches, noBunches, alphabet, "des", "des", null, null, 0));
        assertContainsOnly(untrustAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(unVerbConcept, agentId));
    }

    @Test
    default void testChangeRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int pastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, pastConcept, "pasado");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", pastConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundConcept);
    }

    @Test
    default void testChangeAdder() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "aba", gerundConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundConcept);
    }

    @Test
    default void testChangeAdderForMultipleAcceptations() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int cryConcept = manager.getMaxConcept() + 1;
        final int cryAcceptation = addSimpleAcceptation(manager, alphabet, cryConcept, "llorar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "aba", gerundConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundConcept);
        assertOnlyOneMorphology(manager, cryAcceptation, alphabet, "llorando", gerundConcept);
    }

    @Test
    default void testChangeAdderAndRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int pastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, pastConcept, "pasado");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "aba", pastConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundConcept);
    }

    @Test
    default void testAddAdderAndRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetBunch, "mi lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        final MorphologyResult morphology = getSingleValue(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertEquals("cantando", morphology.text);
        assertContainsOnly(gerundConcept, morphology.rules);
        assertContainsOnly(morphology.dynamicAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
    }

    @Test
    default void testAddAdderAndRuleForMultipleTargetBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetBunch, "mi lista");

        final int myTargetBunch2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetBunch2, "mi otra lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        final MorphologyResult morphology = getSingleValue(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertEquals("cantando", morphology.text);
        assertContainsOnly(gerundConcept, morphology.rules);
        assertContainsOnly(morphology.dynamicAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
        assertContainsOnly(morphology.dynamicAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch2, agentId));
    }

    @Test
    default void testRemoveAdderAndRule() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetBunch, "mi lista");

        final int myTargetBunch2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetBunch2, "mi otra lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(myTargetBunch, myTargetBunch2), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch2, agentId));
    }

    @Test
    default void testRemoveAdderAndRuleForMultipleTargetBunches() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetBunch, "mi lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, intSetOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, intSetOf(myTargetBunch), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
        assertContainsOnly(singAcceptation, manager.getAcceptationsInBunchByBunchAndAgent(myTargetBunch, agentId));
    }

    @Test
    default void testUpdateCorrelationArrayMatchingAgentBefore() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        assertNotNull(addSingleAlphabetAgent(manager, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar (sin instrumentos)"));
        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
    }

    @Test
    default void testUpdateCorrelationArrayMatchingAgentAfter() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar (sin instrumentos)");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        assertNotNull(addSingleAlphabetAgent(manager, intSetOf(), noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar"));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundConcept);
    }

    @Test
    default void testUpdateCorrelationArrayMatchingChainedAgentBefore() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final int verbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");

        final ImmutableIntSet noBunches = intSetOf();
        assertNotNull(addSingleAlphabetAgent(manager, intSetOf(verbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        assertNotNull(addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar (sin instrumentos)"));
        assertEmpty(manager.readMorphologiesFromAcceptation(singAcceptation, alphabet).morphologies);
    }

    @Test
    default void testUpdateCorrelationArrayMatchingChainedAgentAfter() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar (sin instrumentos)");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final int verbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");

        final ImmutableIntSet noBunches = intSetOf();
        assertNotNull(addSingleAlphabetAgent(manager, intSetOf(verbConcept), noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        assertNotNull(addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar"));
        assertOnlyOneMorphology(manager, singAcceptation, alphabet, "cantando", gerundConcept);
    }

    @Test
    default void testAgentWithJustEndAdderForAcceptationFromOtherLanguage() {
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(new MemoryDatabase());
        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, esAlphabet, singConcept, "cantar");

        final int myBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, myBunch, "palabras");
        manager.addAcceptationInBunch(myBunch, singAcceptation);

        final int verbalitationConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, verbalitationConcept, "verbalización");

        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(myBunch), intSetOf(), jaAlphabet, null, null, null, "する", verbalitationConcept);
        assertEmpty(manager.getAgentProcessedMap(agentId));

        final int studyConcept = manager.getMaxConcept() + 1;
        final int studyAcceptation = addSimpleAcceptation(manager, jaAlphabet, studyConcept, "べんきょう");
        manager.addAcceptationInBunch(myBunch, studyAcceptation);
        assertContainsOnly(studyAcceptation, manager.getAgentProcessedMap(agentId).keySet());
    }

    @Test
    default void testAvoidDuplicatedBunchSetsWhenSharingConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int guyConcept = manager.getMaxConcept() + 1;
        final int guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final int personConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final int targetConcept1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept1, "mis palabras 1");

        final int targetConcept2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept2, "mis palabras 2");

        final int agent1 = addSingleAlphabetAgent(manager, intSetOf(targetConcept1), intSetOf(guyConcept), intSetOf(), alphabet, null, null, null, null, 0);

        final int agent2 = addSingleAlphabetAgent(manager, intSetOf(targetConcept2), intSetOf(personConcept), intSetOf(), alphabet, null, null, null, null, 0);

        final int oldSetId = manager.getAgentRegister(agent2).sourceBunchSetId;
        assertTrue(manager.shareConcept(guyAcc, personConcept));

        final int setId = manager.getAgentRegister(agent1).sourceBunchSetId;
        assertEquals(setId, manager.getAgentRegister(agent2).sourceBunchSetId);

        assertContainsOnly(guyConcept, manager.getBunchSet(setId));
        assertEmpty(manager.getBunchSet(oldSetId));
    }

    @Test
    default void testReuseBunchSetWhenSharingConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int guyConcept = manager.getMaxConcept() + 1;
        final int guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final int personConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final int targetConcept1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept1, "mis palabras 1");

        final int targetConcept2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept2, "mis palabras 2");

        final int agent1 = addSingleAlphabetAgent(manager, intSetOf(targetConcept1), intSetOf(guyConcept), intSetOf(), alphabet, null, null, null, null, 0);

        final int agent2 = addSingleAlphabetAgent(manager, intSetOf(targetConcept2), intSetOf(guyConcept, personConcept), intSetOf(), alphabet, null, null, null, null, 0);

        final int setId = manager.getAgentRegister(agent1).sourceBunchSetId;
        final int oldAgent2SetId = manager.getAgentRegister(agent2).sourceBunchSetId;
        assertTrue(manager.shareConcept(guyAcc, personConcept));

        assertEquals(setId, manager.getAgentRegister(agent1).sourceBunchSetId);
        assertEquals(setId, manager.getAgentRegister(agent2).sourceBunchSetId);
        assertContainsOnly(guyConcept, manager.getBunchSet(setId));
        assertEmpty(manager.getBunchSet(oldAgent2SetId));
    }

    @Test
    default void testAvoidDuplicatedBunchInBunchSetWhenSharingConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int guyConcept = manager.getMaxConcept() + 1;
        final int guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final int personConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final int targetConcept1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept1, "mis palabras 1");

        final int targetConcept2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, targetConcept2, "mis palabras 2");

        final int agent = addSingleAlphabetAgent(manager, intSetOf(targetConcept2), intSetOf(guyConcept, personConcept), intSetOf(), alphabet, null, null, null, null, 0);

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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int jumpConcept = manager.getMaxConcept() + 1;
        final int jumpAcc = addSimpleAcceptation(manager, alphabet, jumpConcept, "saltar");

        final int jumpConcept2 = manager.getMaxConcept() + 1;
        final int jumpAcc2 = addSimpleAcceptation(manager, alphabet, jumpConcept2, "brincar");

        final int bunchConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, bunchConcept, "mi lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final int continuousConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, continuousConcept, "continuo");

        final int agent1 = addSingleAlphabetAgent(manager, intSetOf(bunchConcept), intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerundConcept);

        final int agent2 = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(bunchConcept), intSetOf(), alphabet, null, "estoy ", null, null, continuousConcept);

        final int ruledJumpAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, jumpAcc);
        final int ruledJumpAcc2 = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, jumpAcc2);
        assertNotEquals(ruledJumpAcc, ruledJumpAcc2);

        final int ruled2JumpAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, ruledJumpAcc);
        final int ruled2JumpAcc2 = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, ruledJumpAcc2);
        assertNotEquals(ruled2JumpAcc, ruled2JumpAcc2);

        final int ruledJumpConcept = manager.conceptFromAcceptation(ruledJumpAcc);
        final int ruledJumpConcept2 = manager.conceptFromAcceptation(ruledJumpAcc2);
        assertNotEquals(ruledJumpConcept, ruledJumpConcept2);

        final int ruled2JumpConcept = manager.conceptFromAcceptation(ruled2JumpAcc);
        final int ruled2JumpConcept2 = manager.conceptFromAcceptation(ruled2JumpAcc2);
        assertNotEquals(ruled2JumpConcept, ruled2JumpConcept2);

        assertTrue(manager.shareConcept(jumpAcc, jumpConcept2));
        assertSinglePair(ruledJumpConcept, jumpConcept, manager.findRuledConceptsByRule(gerundConcept));
        final ImmutableIntPairMap ruledAcceptations = findRuledAcceptationsByAgent(db, agent1);
        assertSize(2, ruledAcceptations);
        assertEquals(jumpAcc, ruledAcceptations.get(ruledJumpAcc));
        assertEquals(jumpAcc2, ruledAcceptations.get(ruledJumpAcc2));
        assertEquals(ruledJumpConcept, manager.conceptFromAcceptation(ruledJumpAcc));
        assertEquals(ruledJumpConcept, manager.conceptFromAcceptation(ruledJumpAcc2));

        assertSinglePair(alphabet, "saltando", manager.getAcceptationTexts(ruledJumpAcc));
        assertSinglePair(alphabet, "brincando", manager.getAcceptationTexts(ruledJumpAcc2));

        assertSinglePair(ruled2JumpConcept, ruledJumpConcept, manager.findRuledConceptsByRule(continuousConcept));
        final ImmutableIntPairMap ruled2Acceptations = findRuledAcceptationsByAgent(db, agent2);
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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int getWetConcept = manager.getMaxConcept() + 1;
        final int getWetEsAcc = addSimpleAcceptation(manager, esAlphabet, getWetConcept, "mojarse");
        final int getWetJaAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "濡れる");

        final int esVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, esVerbConcept, "Verbo español");
        manager.addAcceptationInBunch(esVerbConcept, getWetEsAcc);

        final int jaVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, jaVerbConcept, "Verbo japonés");
        manager.addAcceptationInBunch(jaVerbConcept, getWetJaAcc);

        final int badCausalRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, badCausalRule, "causalización");

        final int causalRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, causalRule, "causal");

        final int esAgent = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(esVerbConcept), intSetOf(), esAlphabet, null, "hacer que se ", "arse", "e", causalRule);

        final int jaAgent = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(jaVerbConcept), intSetOf(), jaAlphabet, null, null, "る", "させる", causalRule);

        updateSingleAlphabetAgent(manager, esAgent, intSetOf(), intSetOf(esVerbConcept), intSetOf(), esAlphabet, null, "hacer que se ", "arse", "e", badCausalRule);

        final int makeWetEsAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(esAgent, getWetEsAcc);
        assertSinglePair(esAlphabet, "hacer que se moje", manager.getAcceptationTexts(makeWetEsAcc));

        final int makeWetJaAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(jaAgent, getWetJaAcc);
        assertSinglePair(jaAlphabet, "濡れさせる", manager.getAcceptationTexts(makeWetJaAcc));

        final int makeWetJaConcept = manager.conceptFromAcceptation(makeWetJaAcc);
        assertNotEquals(makeWetJaConcept, manager.conceptFromAcceptation(makeWetEsAcc));
    }

    @Test
    default void testUpdateAgentRuleToAlreadyUsedRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int getWetConcept = manager.getMaxConcept() + 1;
        final int getWetEsAcc = addSimpleAcceptation(manager, esAlphabet, getWetConcept, "mojarse");
        final int getWetJaAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "濡れる");

        final int esVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, esVerbConcept, "Verbo español");
        manager.addAcceptationInBunch(esVerbConcept, getWetEsAcc);

        final int jaVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, jaVerbConcept, "Verbo japonés");
        manager.addAcceptationInBunch(jaVerbConcept, getWetJaAcc);

        final int badCausalRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, badCausalRule, "causalización");

        final int esAgent = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(esVerbConcept), intSetOf(), esAlphabet, null, "hacer que se ", "arse", "e", badCausalRule);

        final int causalRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, causalRule, "causal");

        final int jaAgent = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(jaVerbConcept), intSetOf(), jaAlphabet, null, null, "る", "させる", causalRule);

        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(badCausalRule));

        updateSingleAlphabetAgent(manager, esAgent, intSetOf(), intSetOf(esVerbConcept), intSetOf(), esAlphabet, null, "hacer que se ", "arse", "e", causalRule);

        final int makeWetEsAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(esAgent, getWetEsAcc);
        assertSinglePair(esAlphabet, "hacer que se moje", manager.getAcceptationTexts(makeWetEsAcc));

        final int makeWetJaAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(jaAgent, getWetJaAcc);
        assertSinglePair(jaAlphabet, "濡れさせる", manager.getAcceptationTexts(makeWetJaAcc));

        final int makeWetConcept = manager.conceptFromAcceptation(makeWetJaAcc);
        assertEquals(makeWetConcept, manager.conceptFromAcceptation(makeWetEsAcc));
        assertEmpty(manager.findRuledConceptsByRule(badCausalRule));
        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(causalRule));
    }

    @Test
    default void testUpdateAgentRuleBetweenUsedRules() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int getWetConcept = manager.getMaxConcept() + 1;
        final int getWetEsAcc = addSimpleAcceptation(manager, esAlphabet, getWetConcept, "mojarse");
        final int getWetJaAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "濡れる");
        final int getWetNaruAcc = addSimpleAcceptation(manager, jaAlphabet, getWetConcept, "びしょびしょになる");

        final int esVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, esVerbConcept, "Verbo español");
        manager.addAcceptationInBunch(esVerbConcept, getWetEsAcc);

        final int jaVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, jaVerbConcept, "Verbo japonés");
        manager.addAcceptationInBunch(jaVerbConcept, getWetJaAcc);

        final int naruVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, naruVerbConcept, "Adjetivo con naru");
        manager.addAcceptationInBunch(naruVerbConcept, getWetNaruAcc);

        final int badCausalRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, badCausalRule, "causalización");

        final int esAgent = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(esVerbConcept), intSetOf(), esAlphabet, null, "hacer que se ", "arse", "e", badCausalRule);

        final int causalRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, causalRule, "causal");

        final int jaAgent = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(jaVerbConcept), intSetOf(), jaAlphabet, null, null, "る", "させる", causalRule);

        final int naruAgent = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(naruVerbConcept), intSetOf(), jaAlphabet, null, null, "になる", "にする", badCausalRule);

        assertContainsOnly(getWetConcept, manager.findRuledConceptsByRule(badCausalRule));

        updateSingleAlphabetAgent(manager, esAgent, intSetOf(), intSetOf(esVerbConcept), intSetOf(), esAlphabet, null, "hacer que se ", "arse", "e", causalRule);

        final int makeWetEsAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(esAgent, getWetEsAcc);
        assertSinglePair(esAlphabet, "hacer que se moje", manager.getAcceptationTexts(makeWetEsAcc));

        final int makeWetJaAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(jaAgent, getWetJaAcc);
        assertSinglePair(jaAlphabet, "濡れさせる", manager.getAcceptationTexts(makeWetJaAcc));

        final int makeWetNaruAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(naruAgent, getWetNaruAcc);
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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int dieConcept = manager.getMaxConcept() + 1;
        final int dieJaAcc = addSimpleAcceptation(manager, jaAlphabet, dieConcept, "死ぬ");

        final int verbConcept = manager.getMaxConcept() + 1;
        final int verbJaAcc = addSimpleAcceptation(manager, jaAlphabet, verbConcept, "動詞");
        manager.addAcceptationInBunch(verbConcept, dieJaAcc);

        final int accidentalRule = manager.getMaxConcept() + 1;
        final int accidentalAcc = addSimpleAcceptation(manager, esAlphabet, accidentalRule, "accidental");

        final int agent1 = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbConcept), intSetOf(), jaAlphabet, null, null, "ぬ", "んでしまう", accidentalRule);

        final int accidentalRule2 = manager.getMaxConcept() + 1;
        final int accidentalAcc2 = addSimpleAcceptation(manager, esAlphabet, accidentalRule2, "accidental informal");

        final int agent2 = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbConcept), intSetOf(), jaAlphabet, null, null, "ぬ", "んじゃう", accidentalRule2);

        assertTrue(manager.shareConcept(accidentalAcc, accidentalRule2));

        final int accidentalDieAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, dieJaAcc);
        assertNotEquals(dieJaAcc, accidentalDieAcc);
        assertNotEquals(verbJaAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc2, accidentalDieAcc);

        final int accidentalDieAcc2 = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, dieJaAcc);
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
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int dieConcept = manager.getMaxConcept() + 1;
        final int dieJaAcc = addSimpleAcceptation(manager, jaAlphabet, dieConcept, "死ぬ");

        final int verbConcept = manager.getMaxConcept() + 1;
        final int verbJaAcc = addSimpleAcceptation(manager, jaAlphabet, verbConcept, "動詞");
        manager.addAcceptationInBunch(verbConcept, dieJaAcc);

        final int accidentalRule = manager.getMaxConcept() + 1;
        final int accidentalAcc = addSimpleAcceptation(manager, esAlphabet, accidentalRule, "accidental");

        final int agent1 = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbConcept), intSetOf(), jaAlphabet, null, null, "ぬ", "んでしまう", accidentalRule);

        final int accidentalRule2 = manager.getMaxConcept() + 1;
        final int accidentalAcc2 = addSimpleAcceptation(manager, esAlphabet, accidentalRule2, "accidental informal");

        assertTrue(manager.shareConcept(accidentalAcc2, accidentalRule));

        final int accidentalDieAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, dieJaAcc);
        assertNotEquals(dieJaAcc, accidentalDieAcc);
        assertNotEquals(verbJaAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc, accidentalDieAcc);
        assertNotEquals(accidentalAcc2, accidentalDieAcc);

        assertEmpty(manager.findRuledConceptsByRule(accidentalRule));
        final ImmutableIntPairMap ruledConcepts = manager.findRuledConceptsByRule(accidentalRule2);
        assertContainsOnly(dieConcept, ruledConcepts);

        final int accidentalDieConcept = ruledConcepts.keyAt(0);
        assertEquals(accidentalDieConcept, manager.conceptFromAcceptation(accidentalDieAcc));
    }

    @Test
    default void testChangeAdderInFirstChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int callConcept = manager.getMaxConcept() + 1;
        final int callJaAcc = addSimpleAcceptation(manager, jaAlphabet, callConcept, "呼ぶ");

        final int verbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, jaAlphabet, verbConcept, "動詞");
        manager.addAcceptationInBunch(verbConcept, callJaAcc);

        final int accidentalRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, accidentalRule, "accidental");

        final int canBePastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, canBePastConcept, "puede ser pasado");

        final int agent1 = addSingleAlphabetAgent(manager, intSetOf(canBePastConcept), intSetOf(verbConcept), intSetOf(), jaAlphabet, null, null, "ぶ", "じまう", accidentalRule);

        final int pastRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, pastRule, "pasado");

        final int agent2 = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(canBePastConcept), intSetOf(), jaAlphabet, null, null, "う", "った", pastRule);

        assertTrue(updateSingleAlphabetAgent(manager, agent1, intSetOf(canBePastConcept), intSetOf(verbConcept), intSetOf(), jaAlphabet, null, null, "ぶ", "んじまう", accidentalRule));

        final ImmutableIntPairMap ruledAccs1 = findRuledAcceptationsByAgent(db, agent1);
        assertContainsOnly(callJaAcc, ruledAccs1);

        final ImmutableIntPairMap ruledAccs2 = findRuledAcceptationsByAgent(db, agent2);
        assertContainsOnly(ruledAccs1.keyAt(0), ruledAccs2);

        final int callAccidentalPastAcc = ruledAccs2.keyAt(0);
        assertSinglePair(jaAlphabet, "呼んじまった", manager.getAcceptationTexts(callAccidentalPastAcc));
    }

    @Test
    default void testChangeAdderInFirstChainedAgentWhenPickedSampleAcceptationForSecondIsOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singJaAcc = addSimpleAcceptation(manager, jaAlphabet, singConcept, "歌う");

        final int callConcept = manager.getMaxConcept() + 1;
        final int callJaAcc = addSimpleAcceptation(manager, jaAlphabet, callConcept, "呼ぶ");

        final int buVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, jaAlphabet, buVerbConcept, "verbo acabado en ぶ");
        manager.addAcceptationInBunch(buVerbConcept, callJaAcc);

        final int accidentalRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, accidentalRule, "accidental");

        final int uVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, uVerbConcept, "verbo acabado en う");
        manager.addAcceptationInBunch(uVerbConcept, singJaAcc);

        final int agent1 = addSingleAlphabetAgent(manager, intSetOf(uVerbConcept), intSetOf(buVerbConcept), intSetOf(), jaAlphabet, null, null, "ぶ", "じまう", accidentalRule);

        final int pastRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, pastRule, "pasado");

        final int agent2 = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(uVerbConcept), intSetOf(), jaAlphabet, null, null, "う", "った", pastRule);

        assertTrue(updateSingleAlphabetAgent(manager, agent1, intSetOf(uVerbConcept), intSetOf(buVerbConcept), intSetOf(), jaAlphabet, null, null, "ぶ", "んじまう", accidentalRule));

        final ImmutableIntPairMap ruledAccs1 = findRuledAcceptationsByAgent(db, agent1);
        assertContainsOnly(callJaAcc, ruledAccs1);

        final ImmutableIntPairMap ruledAccs2 = findRuledAcceptationsByAgent(db, agent2);
        assertContainsOnly(ruledAccs1.keyAt(0), singJaAcc, ruledAccs2);

        final int callAccidentalPastAcc = ruledAccs2.keyAt((ruledAccs2.valueAt(0) == singJaAcc)? 1 : 0);
        assertSinglePair(jaAlphabet, "呼んじまった", manager.getAcceptationTexts(callAccidentalPastAcc));
    }
}
