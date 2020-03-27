package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.Conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.cloneBySerializing;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.findAcceptationsMatchingText;
import static sword.langbook3.android.sdb.IntKeyMapTestUtils.assertSinglePair;
import static sword.langbook3.android.sdb.IntSetTestUtils.intSetOf;
import static sword.langbook3.android.sdb.IntTraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.sdb.IntTraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.SizableTestUtils.assertEmpty;
import static sword.langbook3.android.sdb.SizableTestUtils.assertSize;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
interface AgentsSerializerTest extends BunchesSerializerTest {

    static int addSimpleAcceptation(AcceptationsManager manager, int alphabet, int concept, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static void addSingleAlphabetAgent(AgentsManager manager, int targetBunch, ImmutableIntSet sourceBunches,
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

        manager.addAgent(targetBunch, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    AgentsManager createManager(MemoryDatabase db);

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        addSingleAlphabetAgent(inManager, 0, intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        assertEmpty(outManager.findRuledConceptsByRule(outGerundConcept));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, 0, intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        assertNotEquals(outGerundAcceptation, outSingAcceptation);

        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outGerundConcept, outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        final ImmutableIntPairMap outRuledConcepts = outManager.findRuledConceptsByRule(outGerundConcept);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final int outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableIntPairMap processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptationWithEmptyDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int exceptions = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, exceptions, "excepciones");

        addSingleAlphabetAgent(inManager, 0, intSetOf(), intSetOf(exceptions), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outExceptionsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "excepciones"));
        final int outExceptionsConcept = outManager.conceptFromAcceptation(outExceptionsAcceptation);
        assertNotEquals(outGerundConcept, outExceptionsConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(outExceptionsConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        assertEmpty(outManager.findRuledConceptsByRule(outGerundConcept));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingAcceptationWithEmptyDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int exceptions = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, exceptions, "excepciones");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, 0, intSetOf(), intSetOf(exceptions), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outExceptionsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "excepciones"));
        final int outExceptionsConcept = outManager.conceptFromAcceptation(outExceptionsAcceptation);
        assertNotEquals(outGerundConcept, outExceptionsConcept);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outGerundConcept, outSingConcept);
        assertNotEquals(outExceptionsConcept, outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(outExceptionsConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        final ImmutableIntPairMap outRuledConcepts = outManager.findRuledConceptsByRule(outGerundConcept);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final int outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableIntPairMap processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptationWithMatchingAcceptationInDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int exceptions = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, exceptions, "excepciones");

        final int palate = inManager.getMaxConcept() + 1;
        final int palateAcc = addSimpleAcceptation(inManager, alphabet, palate, "paladar");
        inManager.addAcceptationInBunch(exceptions, palateAcc);

        addSingleAlphabetAgent(inManager, 0, intSetOf(), intSetOf(exceptions), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outExceptionsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "excepciones"));
        final int outExceptionsConcept = outManager.conceptFromAcceptation(outExceptionsAcceptation);
        assertNotEquals(outGerundConcept, outExceptionsConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(outExceptionsConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        assertEmpty(outManager.findRuledConceptsByRule(outGerundConcept));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingAcceptationWithMatchingAcceptationInDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int exceptions = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, exceptions, "excepciones");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final int palate = inManager.getMaxConcept() + 1;
        final int palateAcc = addSimpleAcceptation(inManager, alphabet, palate, "paladar");
        inManager.addAcceptationInBunch(exceptions, palateAcc);

        addSingleAlphabetAgent(inManager, 0, intSetOf(), intSetOf(exceptions), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outExceptionsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "excepciones"));
        final int outExceptionsConcept = outManager.conceptFromAcceptation(outExceptionsAcceptation);
        assertNotEquals(outGerundConcept, outExceptionsConcept);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outGerundConcept, outSingConcept);
        assertNotEquals(outExceptionsConcept, outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(outExceptionsConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        final ImmutableIntPairMap outRuledConcepts = outManager.findRuledConceptsByRule(outGerundConcept);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final int outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableIntPairMap processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithTargetWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int gerundBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerundBunch, "gerundios");

        addSingleAlphabetAgent(inManager, gerundBunch, intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outGerundBunchAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundios"));
        final int outGerundBunchConcept = outManager.conceptFromAcceptation(outGerundBunchAcceptation);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outGerundBunchConcept, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        assertEmpty(outManager.findRuledConceptsByRule(outGerundConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outGerundBunchConcept));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithTargetWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int gerundBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerundBunch, "gerundios");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, gerundBunch, intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outGerundBunchAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundios"));
        assertNotEquals(outGerundAcceptation, outGerundBunchAcceptation);
        final int outGerundBunchConcept = outManager.conceptFromAcceptation(outGerundBunchAcceptation);
        assertNotEquals(outGerundConcept, outGerundBunchConcept);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        assertNotEquals(outGerundAcceptation, outSingAcceptation);
        assertNotEquals(outGerundBunchAcceptation, outSingAcceptation);

        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outGerundConcept, outSingConcept);
        assertNotEquals(outGerundBunchConcept, outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outGerundBunchConcept, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        final ImmutableIntPairMap outRuledConcepts = outManager.findRuledConceptsByRule(outGerundConcept);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final int outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableIntPairMap processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
        assertContainsOnly(processedMap.valueAt(0), outManager.getAcceptationsInBunch(outGerundBunchConcept));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingBothStaticAndDynamicAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;

        final int repeat = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, repeat, "repetición");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, 0, intSetOf(), intSetOf(), alphabet, null, null, "ar", "arar", repeat);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outRepeatAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "repetición"));
        final int outRepeatConcept = outManager.conceptFromAcceptation(outRepeatAcceptation);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        assertNotEquals(outRepeatAcceptation, outSingAcceptation);

        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outRepeatConcept, outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "arar", outAgentDetails.endAdder);
        assertEquals(outRepeatConcept, outAgentDetails.rule);

        final ImmutableIntPairMap outRuledConcepts = outManager.findRuledConceptsByRule(outRepeatConcept);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final int outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableIntPairMap processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithEmptySourceBunch() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;
        final int gerund = inManager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");
        addSimpleAcceptation(inManager, alphabet, concept, "cantar");

        addSingleAlphabetAgent(inManager, 0, intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outGerundConcept, outVerbConcept);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outGerundConcept, outSingConcept);
        assertNotEquals(outVerbConcept, outSingConcept);

        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
        assertEmpty(outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertContainsOnly(outVerbConcept, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        assertNull(outManager.findRuledConcept(outGerundConcept, outSingConcept));
        assertNull(outManager.findRuledAcceptationByAgentAndBaseAcceptation(outAgentId, outSingAcceptation));
    }

    @Test
    default void testSerializeAgentApplyingRuleNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;
        final int gerund = inManager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");
        final int acceptation = addSimpleAcceptation(inManager, alphabet, concept, "cantar");
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");
        inManager.addAcceptationInBunch(verbConcept, acceptation);

        addSingleAlphabetAgent(inManager, 0, intSetOf(verbConcept), intSetOf(), alphabet, null, null, "er", "iendo", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outGerundConcept, outVerbConcept);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outGerundConcept, outSingConcept);
        assertNotEquals(outVerbConcept, outSingConcept);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
        assertContainsOnly(outVerbConcept, outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertContainsOnly(outVerbConcept, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "er", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "iendo", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        assertEmpty(outManager.findRuledConceptsByRule(outGerundConcept));
        assertNull(outManager.findRuledAcceptationByAgentAndBaseAcceptation(outAgentId, outSingAcceptation));
    }

    @Test
    default void testSerializeAgentApplyingRuleMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;
        final int gerund = inManager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");
        final int acceptation = addSimpleAcceptation(inManager, alphabet, concept, "cantar");
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");
        inManager.addAcceptationInBunch(verbConcept, acceptation);

        addSingleAlphabetAgent(inManager, 0, intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outGerundConcept, outVerbConcept);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outGerundConcept, outSingConcept);
        assertNotEquals(outVerbConcept, outSingConcept);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
        assertContainsOnly(outVerbConcept, outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertContainsOnly(outVerbConcept, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        final int outSingingConcept = outManager.findRuledConcept(outGerundConcept, outSingConcept);
        final int outSingingAcceptation = outManager.findRuledAcceptationByAgentAndBaseAcceptation(outAgentId, outSingAcceptation);
        assertNotEquals(outSingAcceptation, outSingingAcceptation);

        assertEquals(outSingingConcept, outManager.conceptFromAcceptation(outSingingAcceptation));
        assertSinglePair(outAlphabet, "cantando", outManager.getAcceptationTexts(outSingingAcceptation));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        inManager.addAgent(verbBunch, intSetOf(arVerbBunch), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final int outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo de primera conjugación"));
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetAgentWithAMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final ImmutableIntSet sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        inManager.addAgent(verbBunch, sourceBunches, noBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final int outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo de primera conjugación"));
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbConcept));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int arEndedNounsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsBunch, "sustantivos acabados en ar");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final ImmutableIntSet sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableIntSet diffBunches = noBunches.add(arEndedNounsBunch);
        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        inManager.addAgent(verbBunch, sourceBunches, diffBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final int outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo de primera conjugación"));
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final int outArEndedNounsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "sustantivos acabados en ar"));
        final int outArEndedNoundConcept = outManager.conceptFromAcceptation(outArEndedNounsAcceptation);
        assertNotEquals(outArVerbConcept, outArEndedNoundConcept);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArEndedNoundConcept, outVerbConcept);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNoundConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNoundConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int arEndedNounsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsBunch, "sustantivos acabados en ar");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        inManager.addAgent(verbBunch, intSetOf(arVerbBunch), intSetOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final int outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo de primera conjugación"));
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final int outArEndedNounsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "sustantivos acabados en ar"));
        final int outArEndedNoundConcept = outManager.conceptFromAcceptation(outArEndedNounsAcceptation);
        assertNotEquals(outArVerbConcept, outArEndedNoundConcept);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArEndedNoundConcept, outVerbConcept);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outArEndedNoundConcept, outSingConcept);
        assertNotEquals(outArVerbConcept, outSingConcept);
        assertNotEquals(outVerbConcept, outSingConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNoundConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNoundConcept));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptationsInBothSourceAndDiffBunches() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int arEndedNounsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsBunch, "sustantivos acabados en ar");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);
        inManager.addAcceptationInBunch(arEndedNounsBunch, inAcceptation);

        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        inManager.addAgent(verbBunch, intSetOf(arVerbBunch), intSetOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final int outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo de primera conjugación"));
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final int outArEndedNounsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "sustantivos acabados en ar"));
        final int outArEndedNoundConcept = outManager.conceptFromAcceptation(outArEndedNounsAcceptation);
        assertNotEquals(outArVerbConcept, outArEndedNoundConcept);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArEndedNoundConcept, outVerbConcept);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outArEndedNoundConcept, outSingConcept);
        assertNotEquals(outArVerbConcept, outSingConcept);
        assertNotEquals(outVerbConcept, outSingConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNoundConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbConcept));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArEndedNoundConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptationsOnlyInDiffBunch() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int arEndedNounsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsBunch, "sustantivos acabados en ar");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        inManager.addAcceptationInBunch(arEndedNounsBunch, inAcceptation);

        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        inManager.addAgent(verbBunch, intSetOf(arVerbBunch), intSetOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final int outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo de primera conjugación"));
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final int outArEndedNounsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "sustantivos acabados en ar"));
        final int outArEndedNoundConcept = outManager.conceptFromAcceptation(outArEndedNounsAcceptation);
        assertNotEquals(outArVerbConcept, outArEndedNoundConcept);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArEndedNoundConcept, outVerbConcept);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        assertNotEquals(outArEndedNoundConcept, outSingConcept);
        assertNotEquals(outArVerbConcept, outSingConcept);
        assertNotEquals(outVerbConcept, outSingConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNoundConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbConcept));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArEndedNoundConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithConversionPresent() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int alphabet = inManager.addLanguage("es").mainAlphabet;
        final int upperCaseAlphabet = inManager.getMaxConcept() + 1;
        final Conversion conversion = new Conversion(alphabet, upperCaseAlphabet, AcceptationsSerializerTest.upperCaseConversion);
        inManager.addAlphabetAsConversionTarget(conversion);

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, 0, intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final ImmutableIntPairMap conversionMap = outManager.getConversionsMap();
        assertSize(1, conversionMap);
        final int outAlphabet = conversionMap.valueAt(0);
        final int outUpperCaseAlphabet = conversionMap.keyAt(0);

        final int outGerundAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "gerundio"));
        final int outGerundConcept = outManager.conceptFromAcceptation(outGerundAcceptation);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(0, outAgentDetails.targetBunch);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        final ImmutableIntPairMap outRuledConcepts = outManager.findRuledConceptsByRule(outGerundConcept);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final int outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableIntPairMap processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));

        final ImmutableIntKeyMap<String> texts = outManager.getAcceptationTexts(processedMap.valueAt(0));
        assertSize(2, texts);
        assertEquals("cantando", texts.get(outAlphabet));
        assertEquals("CANTANDO", texts.get(outUpperCaseAlphabet));
    }

    @Test
    default void testSerializeAgentWithJustEndAdderForAcceptationFromOtherLanguage() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager manager = createManager(inDb);
        final int esAlphabet = manager.addLanguage("es").mainAlphabet;
        final int jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, esAlphabet, singConcept, "cantar");

        final int myBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, myBunch, "palabras");
        manager.addAcceptationInBunch(myBunch, singAcceptation);

        final int studyConcept = manager.getMaxConcept() + 1;
        final int studyAcceptation = addSimpleAcceptation(manager, jaAlphabet, studyConcept, "べんきょう");
        manager.addAcceptationInBunch(myBunch, studyAcceptation);

        final int verbalitationConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, verbalitationConcept, "verbalización");

        addSingleAlphabetAgent(manager, 0, intSetOf(myBunch), intSetOf(), jaAlphabet, null, null, null, "する", verbalitationConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outStudyAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "べんきょう"));
        final int outAgentId = getSingleValue(outManager.getAgentIds());
        assertContainsOnly(outStudyAcceptation, manager.getAgentProcessedMap(outAgentId).keySet());
    }
}
