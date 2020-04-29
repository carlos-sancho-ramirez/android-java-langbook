package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.MorphologyResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.updateAcceptationSimpleCorrelationArray;
import static sword.langbook3.android.db.BunchesManagerTest.addSpanishSingAcceptation;
import static sword.langbook3.android.db.IntKeyMapTestUtils.assertSinglePair;
import static sword.langbook3.android.db.IntSetTestUtils.assertEqualSet;
import static sword.langbook3.android.db.IntSetTestUtils.intSetOf;
import static sword.langbook3.android.db.IntTraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.db.IntTraversableTestUtils.getSingleValue;
import static sword.langbook3.android.db.SizableTestUtils.assertEmpty;
import static sword.langbook3.android.db.SizableTestUtils.assertSize;
import static sword.langbook3.android.db.TraversableTestUtils.getSingleValue;

/**
 * Include all test related to all responsibilities of a AgentsManager.
 *
 * AgentsManager responsibilities include all responsibilities from BunchesManager, and include the following ones:
 * <li>Bunch sets</li>
 * <li>Rules</li>
 * <li>Ruled concepts</li>
 * <li>Ruled acceptations</li>
 * <li>Agents</li>
 */
interface AgentsManagerTest extends BunchesManagerTest {

    @Override
    AgentsManager createManager(MemoryDatabase db);

    static Integer addSingleAlphabetAgent(AgentsManager manager, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, int alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, int rule) {
        final ImmutableIntKeyMap<String> startMatcher = (startMatcherText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, startMatcherText).build();

        final ImmutableIntKeyMap<String> startAdder = (startAdderText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, startAdderText).build();

        final ImmutableIntKeyMap<String> endMatcher = (endMatcherText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, endMatcherText).build();

        final ImmutableIntKeyMap<String> endAdder = (endAdderText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, endAdderText).build();

        return manager.addAgent(targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    static boolean updateSingleAlphabetAgent(AgentsManager manager, int agentId, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, int alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, int rule) {
        final ImmutableIntKeyMap<String> startMatcher = (startMatcherText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, startMatcherText).build();

        final ImmutableIntKeyMap<String> startAdder = (startAdderText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, startAdderText).build();

        final ImmutableIntKeyMap<String> endMatcher = (endMatcherText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, endMatcherText).build();

        final ImmutableIntKeyMap<String> endAdder = (endAdderText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, endAdderText).build();

        return manager.updateAgent(agentId, targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    static void assertOnlyOneMorphology(AgentsManager manager, int staticAcceptation, int preferredAlphabet, String expectedText, int expectedRule) {
        final MorphologyResult morphology = getSingleValue(manager.readMorphologiesFromAcceptation(staticAcceptation, preferredAlphabet).morphologies);
        assertEquals(expectedText, morphology.text);
        assertContainsOnly(expectedRule, morphology.rules);
    }

    static void assertNoRuledAcceptationsPresentForChainedAgents(AgentsManager manager, Add3ChainedAgentsResult result) {
        assertEmpty(manager.getAgentProcessedMap(result.agent1Id));
        assertEmpty(manager.getAgentProcessedMap(result.agent2Id));
        assertEmpty(manager.getAgentProcessedMap(result.agent3Id));
    }

    @Test
    default void testAddAgentApplyingRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int arVerbConcept = verbConcept + 1;
        final int extraBunch = arVerbConcept + 1;
        final int singConcept = extraBunch + 1;

        final int acceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        assertTrue(manager.addAcceptationInBunch(verbConcept, acceptation));

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int bunchConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, bunchConcept, "pluralizable sustituyendo ón por ones");

        int acceptation = 0;
        if (acceptationBeforeAgents) {
            final int songConcept = manager.getMaxConcept() + 1;
            acceptation = addSimpleAcceptation(manager, alphabet, songConcept, "canción");
        }

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ón")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
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
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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

    static Add3ChainedAgentsResult add3ChainedAgents(
            AgentsManager manager,
            int alphabet, ImmutableIntSet sourceBunchSet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntSet noBunches = intSetOf();
        final int agent3Id = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(actionConcept), noBunches, alphabet, null, null, null, "s", pluralRule);
        final int agent2Id = addSingleAlphabetAgent(manager, intSetOf(actionConcept), intSetOf(arVerbConcept), noBunches, alphabet, null, null, "ar", "o", nominalizationRule);
        final int agent1Id = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept), sourceBunchSet, noBunches, alphabet, null, null, "ar", "ar", 0);

        return new Add3ChainedAgentsResult(agent1Id, agent2Id, agent3Id);
    }

    static Add3ChainedAgentsResult add3ChainedAgents(AgentsManager manager,
            int alphabet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();
        return add3ChainedAgents(manager, alphabet, noBunches, arVerbConcept, actionConcept, nominalizationRule, pluralRule);
    }

    @Test
    default void testAdd3ChainedAgents() {
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int studentConcept = manager.getMaxConcept() + 1;
        final int studentAcceptation = addSimpleAcceptation(manager, alphabet, studentConcept, "alumno");

        final int femenineRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineRule, "femenino");

        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(), intSetOf(), alphabet, null, null, "o", "a", femenineRule);

        final int femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
        final ImmutableIntList correlationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        manager.removeAgent(agentId);

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(femaleStudentAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(femaleStudentAcceptation));
        for (int correlationId : correlationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }
    }

    @Test
    default void testRemoveDynamicAcceptationsWhenAcceptationFromSourceBunch() {
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int studentConcept = manager.getMaxConcept() + 1;
        final int studentAcceptation = addSimpleAcceptation(manager, alphabet, studentConcept, "alumno");

        final int sourceBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, sourceBunch, "mis palabras");
        manager.addAcceptationInBunch(sourceBunch, studentAcceptation);

        final int femenineRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, femenineRule, "femenino");

        final int agentId = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(sourceBunch), intSetOf(), alphabet, null, null, "o", "a", femenineRule);

        final int femaleStudentAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation);
        final ImmutableIntList studentCorrelationArray = manager.getAcceptationCorrelationArray(studentAcceptation);
        final ImmutableIntList femaleStudentCorrelationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        assertTrue(manager.removeAcceptation(studentAcceptation));

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(femaleStudentAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(studentAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(femaleStudentAcceptation));
        for (int correlationId : studentCorrelationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }
        for (int correlationId : femaleStudentCorrelationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }
    }

    @Test
    default void testRemoveUnusedBunchSetsWhenRemovingAgent() {
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final ImmutableIntList correlationArray = manager.getAcceptationCorrelationArray(femaleStudentAcceptation);
        final AgentRegister agentRegister = manager.getAgentRegister(agentId);
        manager.removeAgent(agentId);

        assertNull(manager.findRuledAcceptationByAgentAndBaseAcceptation(agentId, studentAcceptation));
        assertEmpty(manager.getAcceptationTexts(femaleStudentAcceptation));
        assertEquals(0, manager.conceptFromAcceptation(femaleStudentAcceptation));
        for (int correlationId : correlationArray) {
            assertEmpty(manager.getCorrelationWithText(correlationId));
        }

        assertEmpty(manager.getBunchSet(agentRegister.targetBunchSetId));
        assertEmpty(manager.getBunchSet(agentRegister.sourceBunchSetId));
        assertEmpty(manager.getBunchSet(agentRegister.diffBunchSetId));
    }

    @Test
    default void testRemoveChainedAgent() {
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbArConcept = gerund + 1;
        final int verbErConcept = verbArConcept + 1;

        addSimpleAcceptation(manager, alphabet, verbArConcept, "verbo ar");
        addSimpleAcceptation(manager, alphabet, verbErConcept, "verbo er");

        final ImmutableIntSet diffBunches = intSetOf();
        addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbArConcept), diffBunches, alphabet, null, null, "ar", "ando", gerund);

        addSingleAlphabetAgent(manager, intSetOf(), intSetOf(verbErConcept), diffBunches, alphabet, null, null, "er", "iendo", gerund);

        ImmutableIntKeyMap<String> texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "provocar").build();
        assertSinglePair(verbArConcept, "verbo ar", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "comer").build();
        assertSinglePair(verbErConcept, "verbo er", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "dormir").build();
        assertEmpty(manager.readAllMatchingBunches(texts, alphabet));
    }

    @Test
    default void testReadAllMatchingBunchesForMultipleMatching() {
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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

        ImmutableIntKeyMap<String> texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "provocar").build();
        final ImmutableIntKeyMap<String> bunches = manager.readAllMatchingBunches(texts, alphabet);
        assertContainsOnly(verbArConcept, sustantivable, bunches.keySet());
        assertEquals("verbo ar", bunches.get(verbArConcept));
        assertEquals("sustantivable", bunches.get(sustantivable));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "comer").build();
        assertSinglePair(verbErConcept, "verbo er", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "dormir").build();
        assertEmpty(manager.readAllMatchingBunches(texts, alphabet));
    }

    @Test
    default void testUpdateCorrelationArrayForAcceptationWithRuleAgent() {
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
        final AgentsManager manager = createManager(new MemoryDatabase());

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int alphabet = manager.addLanguage("es").mainAlphabet;

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
        final AgentsManager manager = createManager(new MemoryDatabase());
        final int esAlphabet = manager.addLanguage("es").mainAlphabet;
        final int jaAlphabet = manager.addLanguage("ja").mainAlphabet;

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
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
    default void testAvoidDuplicatedBunchInBunchSetWhenSharingConcept() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
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
}
