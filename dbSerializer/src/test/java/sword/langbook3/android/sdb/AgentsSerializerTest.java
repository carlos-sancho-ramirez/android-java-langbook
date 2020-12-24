package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.AgentsChecker;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.MorphologyResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static sword.collections.IntPairMapTestUtils.assertSinglePair;
import static sword.collections.IntSetTestUtils.intSetOf;
import static sword.collections.IntTraversableTestUtils.assertContainsOnly;
import static sword.collections.IntTraversableTestUtils.getSingleValue;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.cloneBySerializing;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.findAcceptationsMatchingText;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
interface AgentsSerializerTest extends BunchesSerializerTest {

    final class BunchFinder {
        private final MemoryDatabase db;
        private final AgentsChecker checker;
        private final MutableIntSet found = MutableIntArraySet.empty();

        BunchFinder(MemoryDatabase db, AgentsChecker checker) {
            this.db = db;
            this.checker = checker;
        }

        int find(String text) {
            final int acceptation = getSingleValue(findAcceptationsMatchingText(db, text));
            final int bunch = checker.conceptFromAcceptation(acceptation);
            assertTrue(found.add(bunch));
            return bunch;
        }

        void assertUnknown(int concept) {
            if (found.contains(concept)) {
                fail("Concept matching an already found bunch concept");
            }
        }
    }

    static int addSimpleAcceptation(AcceptationsManager manager, AlphabetId alphabet, int concept, String text) {
        final ImmutableCorrelation correlation = new ImmutableCorrelation.Builder()
                .put(alphabet, text)
                .build();

        final ImmutableList<ImmutableCorrelation> correlationArray = new ImmutableList.Builder<ImmutableCorrelation>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static int addSingleAlphabetAgent(AgentsManager manager, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, int rule) {
        final ImmutableCorrelation startMatcher = (startMatcherText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder().put(alphabet, startMatcherText).build();

        final ImmutableCorrelation startAdder = (startAdderText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder().put(alphabet, startAdderText).build();

        final ImmutableCorrelation endMatcher = (endMatcherText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder().put(alphabet, endMatcherText).build();

        final ImmutableCorrelation endAdder = (endAdderText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder().put(alphabet, endAdderText).build();

        return manager.addAgent(targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    AgentsManager createManager(MemoryDatabase db);

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final int outGerundConcept = new BunchFinder(outDb, outManager).find("gerundio");
        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int exceptions = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, exceptions, "excepciones");

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(), intSetOf(exceptions), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outExceptionsConcept = bunchFinder.find("excepciones");

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int exceptions = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, exceptions, "excepciones");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(), intSetOf(exceptions), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outExceptionsConcept = bunchFinder.find("excepciones");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int exceptions = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, exceptions, "excepciones");

        final int palate = inManager.getMaxConcept() + 1;
        final int palateAcc = addSimpleAcceptation(inManager, alphabet, palate, "paladar");
        inManager.addAcceptationInBunch(exceptions, palateAcc);

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(), intSetOf(exceptions), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outExceptionsConcept = bunchFinder.find("excepciones");

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int exceptions = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, exceptions, "excepciones");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final int palate = inManager.getMaxConcept() + 1;
        final int palateAcc = addSimpleAcceptation(inManager, alphabet, palate, "paladar");
        inManager.addAcceptationInBunch(exceptions, palateAcc);

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(), intSetOf(exceptions), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outExceptionsConcept = bunchFinder.find("excepciones");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int gerundBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerundBunch, "gerundios");

        addSingleAlphabetAgent(inManager, intSetOf(gerundBunch), intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outGerundBunchConcept = bunchFinder.find("gerundios");

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outGerundBunchConcept, outAgentDetails.targetBunches);
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
    default void testSerializeAgentApplyingRuleWithMultipleTargetsWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int gerundBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerundBunch, "gerundios");

        final int secondBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, secondBunch, "mis palabras");

        addSingleAlphabetAgent(inManager, intSetOf(gerundBunch, secondBunch), intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outGerundBunchConcept = bunchFinder.find("gerundios");
        final int outSecondBunchConcept = bunchFinder.find("mis palabras");

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outGerundBunchConcept, outSecondBunchConcept, outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        assertEquals(outGerundConcept, outAgentDetails.rule);

        assertEmpty(outManager.findRuledConceptsByRule(outGerundConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outGerundBunchConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outSecondBunchConcept));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithTargetWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int gerundBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerundBunch, "gerundios");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, intSetOf(gerundBunch), intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outGerundBunchConcept = bunchFinder.find("gerundios");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outGerundBunchConcept, outAgentDetails.targetBunches);
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
    default void testSerializeAgentApplyingRuleWithMultipleTargetsWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int gerundBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerundBunch, "gerundios");

        final int secondBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, secondBunch, "mis palabras");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, intSetOf(gerundBunch, secondBunch), intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outGerundBunchConcept = bunchFinder.find("gerundios");
        final int outSecondBunch = bunchFinder.find("mis palabras");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outGerundBunchConcept, outSecondBunch, outAgentDetails.targetBunches);
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
        assertContainsOnly(processedMap.valueAt(0), outManager.getAcceptationsInBunch(outSecondBunch));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingBothStaticAndDynamicAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final int repeat = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, repeat, "repetición");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(), intSetOf(), alphabet, null, null, "ar", "arar", repeat);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outRepeatConcept = bunchFinder.find("repetición");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final int gerund = inManager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");
        addSimpleAcceptation(inManager, alphabet, concept, "cantar");

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outVerbConcept = bunchFinder.find("verbo");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
        assertEmpty(outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final int gerund = inManager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");
        final int acceptation = addSimpleAcceptation(inManager, alphabet, concept, "cantar");
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");
        inManager.addAcceptationInBunch(verbConcept, acceptation);

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(verbConcept), intSetOf(), alphabet, null, null, "er", "iendo", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outVerbConcept = bunchFinder.find("verbo");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
        assertContainsOnly(outVerbConcept, outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final int gerund = inManager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");
        final int acceptation = addSimpleAcceptation(inManager, alphabet, concept, "cantar");
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");
        inManager.addAcceptationInBunch(verbConcept, acceptation);

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");
        final int outVerbConcept = bunchFinder.find("verbo");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
        assertContainsOnly(outVerbConcept, outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(intSetOf(verbBunch), intSetOf(arVerbBunch), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outAgentDetails.targetBunches);
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
    default void testSerializeCopyFromSingleSourceToMultipleTargetsAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int transitiveVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, transitiveVerbBunch, "verbo transitivo");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(intSetOf(verbBunch, transitiveVerbBunch), intSetOf(arVerbBunch), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outTransitiveVerbConcept = bunchFinder.find("verbo transitivo");
        final int outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outTransitiveVerbConcept, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outTransitiveVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetAgentWithAMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final ImmutableIntSet sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(intSetOf(verbBunch), sourceBunches, noBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outAgentDetails.targetBunches);
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
    default void testSerializeCopyFromSingleSourceToMultipleTargetsAgentWithAMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int secondBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, secondBunch, "mis palabras");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final ImmutableIntSet sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(intSetOf(verbBunch, secondBunch), sourceBunches, noBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outSecondConcept = bunchFinder.find("mis palabras");
        final int outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outSecondConcept, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbConcept));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outSecondConcept));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int arEndedNounsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsBunch, "sustantivos acabados en ar");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final ImmutableIntSet sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableIntSet diffBunches = noBunches.add(arEndedNounsBunch);
        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(intSetOf(verbBunch), sourceBunches, diffBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outArEndedNoundConcept = bunchFinder.find("sustantivos acabados en ar");
        final int outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outAgentDetails.targetBunches);
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
    default void testSerializeCopyFromSingleSourceToMultipleTargetsWithDiffBunchAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int arEndedNounsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsBunch, "sustantivos acabados en ar");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int actionBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, actionBunch, "acción");

        final ImmutableIntSet targetBunches = intSetOf(verbBunch, actionBunch);
        final ImmutableIntSet sourceBunches = intSetOf(arVerbBunch);
        final ImmutableIntSet diffBunches = intSetOf(arEndedNounsBunch);
        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(targetBunches, sourceBunches, diffBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outArEndedNoundConcept = bunchFinder.find("sustantivos acabados en ar");
        final int outVerbConcept = bunchFinder.find("verbo");
        final int outActionConcept = bunchFinder.find("acción");

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outActionConcept, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNoundConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNoundConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outActionConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int arEndedNounsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsBunch, "sustantivos acabados en ar");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(intSetOf(verbBunch), intSetOf(arVerbBunch), intSetOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outArEndedNoundConcept = bunchFinder.find("sustantivos acabados en ar");
        final int outVerbConcept = bunchFinder.find("verbo");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outAgentDetails.targetBunches);
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
    default void testSerializeCopyFromSingleSourceToMultipleTargetsWithDiffBunchAgentWithMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int arEndedNounsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsBunch, "sustantivos acabados en ar");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int actionBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, actionBunch, "acción");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(intSetOf(verbBunch, actionBunch), intSetOf(arVerbBunch), intSetOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outArEndedNoundConcept = bunchFinder.find("sustantivos acabados en ar");
        final int outVerbConcept = bunchFinder.find("verbo");
        final int outActionConcept = bunchFinder.find("acción");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outActionConcept, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNoundConcept, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNoundConcept));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outActionConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptationsInBothSourceAndDiffBunches() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
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

        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(intSetOf(verbBunch), intSetOf(arVerbBunch), intSetOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outArEndedNoundConcept = bunchFinder.find("sustantivos acabados en ar");
        final int outVerbConcept = bunchFinder.find("verbo");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outAgentDetails.targetBunches);
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

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int arEndedNounsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsBunch, "sustantivos acabados en ar");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        inManager.addAcceptationInBunch(arEndedNounsBunch, inAcceptation);

        final ImmutableCorrelation emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(intSetOf(verbBunch), intSetOf(arVerbBunch), intSetOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final int outArEndedNoundConcept = bunchFinder.find("sustantivos acabados en ar");
        final int outVerbConcept = bunchFinder.find("verbo");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(outVerbConcept, outAgentDetails.targetBunches);
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

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final AlphabetId upperCaseAlphabet = new AlphabetId(inManager.getMaxConcept() + 1);
        final Conversion conversion = new Conversion(alphabet, upperCaseAlphabet, AcceptationsSerializerTest.upperCaseConversion);
        inManager.addAlphabetAsConversionTarget(conversion);

        final int gerund = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final int singConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = outManager.getConversionsMap();
        assertSize(1, conversionMap);
        final AlphabetId outAlphabet = conversionMap.valueAt(0);
        final AlphabetId outUpperCaseAlphabet = conversionMap.keyAt(0);

        final BunchFinder bunchFinder = new BunchFinder(outDb, outManager);
        final int outGerundConcept = bunchFinder.find("gerundio");

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
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

        final ImmutableCorrelation texts = outManager.getAcceptationTexts(processedMap.valueAt(0));
        assertSize(2, texts);
        assertEquals("cantando", texts.get(outAlphabet));
        assertEquals("CANTANDO", texts.get(outUpperCaseAlphabet));
    }

    @Test
    default void testSerializeAgentWithJustEndAdderForAcceptationFromOtherLanguage() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager manager = createManager(inDb);
        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

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

        addSingleAlphabetAgent(manager, intSetOf(), intSetOf(myBunch), intSetOf(), jaAlphabet, null, null, null, "する", verbalitationConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outStudyAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "べんきょう"));
        final int outAgentId = getSingleValue(outManager.getAgentIds());
        assertContainsOnly(outStudyAcceptation, manager.getAgentProcessedMap(outAgentId).keySet());
    }

    @Test
    default void testSerializeChainedAgentsApplyingRules() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager manager = createManager(inDb);
        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int studentConcept = manager.getMaxConcept() + 1;
        final int studentAcceptation = addSimpleAcceptation(manager, esAlphabet, studentConcept, "alumno");

        final int bunch1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, bunch1, "palabras masculinas");
        manager.addAcceptationInBunch(bunch1, studentAcceptation);

        final int bunch2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, bunch2, "palabras femeninas");

        final int femenineRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, femenineRule, "femenino");

        final int pluralRule = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, pluralRule, "plural");

        addSingleAlphabetAgent(manager, intSetOf(bunch2), intSetOf(bunch1), intSetOf(), esAlphabet, null, null, "o", "a", femenineRule);

        addSingleAlphabetAgent(manager, intSetOf(), intSetOf(bunch1, bunch2), intSetOf(), esAlphabet, null, null, null, "s", pluralRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outStudentAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "alumno"));
        final int outStudentConcept = outManager.conceptFromAcceptation(outStudentAcceptation);

        final int outFemaleStudentAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "alumna"));
        final int outFemaleStudentConcept = outManager.conceptFromAcceptation(outFemaleStudentAcceptation);

        final int outStudentsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "alumnos"));
        final int outStudentsConcept = outManager.conceptFromAcceptation(outStudentsAcceptation);

        final int outFemaleStudentsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "alumnas"));
        final int outFemaleStudentsConcept = outManager.conceptFromAcceptation(outFemaleStudentsAcceptation);

        final int outFemenineRuleAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "femenino"));
        final int outFemenineRule = outManager.conceptFromAcceptation(outFemenineRuleAcceptation);

        final int outPluralRuleAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "plural"));
        final int outPluralRule = outManager.conceptFromAcceptation(outPluralRuleAcceptation);

        assertSinglePair(outFemaleStudentConcept, outStudentConcept, outManager.findRuledConceptsByRule(outFemenineRule));

        final ImmutableIntPairMap pluralProcessedConcepts = outManager.findRuledConceptsByRule(outPluralRule);
        assertSize(2, pluralProcessedConcepts);
        assertEquals(outStudentConcept, pluralProcessedConcepts.get(outStudentsConcept));
        assertEquals(outFemaleStudentConcept, pluralProcessedConcepts.get(outFemaleStudentsConcept));

        final int outLanguage = outManager.findLanguageByCode("es").intValue();
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ImmutableSet<MorphologyResult> morphologies = outManager.readMorphologiesFromAcceptation(outStudentAcceptation, outAlphabet).morphologies.toSet();
        assertSize(3, morphologies);
        assertContainsOnly(outFemenineRule, morphologies.findFirst(result -> result.dynamicAcceptation == outFemaleStudentAcceptation, null).rules);
        assertContainsOnly(outPluralRule, morphologies.findFirst(result -> result.dynamicAcceptation == outStudentsAcceptation, null).rules);
        assertContainsOnly(outFemenineRule, outPluralRule, morphologies.findFirst(result -> result.dynamicAcceptation == outFemaleStudentsAcceptation, null).rules);
    }
}
