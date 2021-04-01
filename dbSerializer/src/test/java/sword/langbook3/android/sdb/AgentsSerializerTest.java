package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.MutableHashSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.AgentsChecker;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.IntSetter;
import sword.langbook3.android.db.LanguageIdInterface;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.MorphologyResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static sword.collections.IntTraversableTestUtils.getSingleValue;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.cloneBySerializing;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.findAcceptationsMatchingText;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
interface AgentsSerializerTest<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId> extends BunchesSerializerTest<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> {

    static <T> ImmutableSet<T> setOf() {
        return ImmutableHashSet.empty();
    }

    static <T> ImmutableSet<T> setOf(T a) {
        return new ImmutableHashSet.Builder<T>().add(a).build();
    }

    static <T> ImmutableSet<T> setOf(T a, T b) {
        return new ImmutableHashSet.Builder<T>().add(a).add(b).build();
    }

    final class ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId> {
        private final MemoryDatabase db;
        private final IntSetter<AcceptationId> acceptationIdSetter;
        private final AgentsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> checker;
        private final MutableHashSet<ConceptId> found = MutableHashSet.empty();

        ConceptFinder(MemoryDatabase db, IntSetter<AcceptationId> acceptationIdSetter, AgentsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> checker) {
            this.db = db;
            this.acceptationIdSetter = acceptationIdSetter;
            this.checker = checker;
        }

        ConceptId find(String text) {
            final AcceptationId acceptation = getSingleValue(findAcceptationsMatchingText(db, acceptationIdSetter, text));
            final ConceptId concept = checker.conceptFromAcceptation(acceptation);
            assertTrue(found.add(concept));
            return concept;
        }

        void assertUnknown(ConceptId concept) {
            if (found.contains(concept)) {
                fail("Concept matching an already found bunch concept");
            }
        }
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId> AcceptationId addSimpleAcceptation(AcceptationsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId> manager, AlphabetId alphabet, ConceptId concept, String text) {
        final ImmutableCorrelation<AlphabetId> correlation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, text)
                .build();

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId> int addSingleAlphabetAgent(AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> manager, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
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

    AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> createManager(MemoryDatabase db);
    RuleId conceptAsRuleId(ConceptId conceptId);

    default ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> newConceptFinder(MemoryDatabase db, AgentsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> checker) {
        return new ConceptFinder<>(db, getAcceptationIdManager(), checker);
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptId outGerundConcept = newConceptFinder(outDb, outManager).find("gerundio");
        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptationWithEmptyDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId exceptionsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, exceptionsConcept, "excepciones");

        final BunchId exceptionsBunch = conceptAsBunchId(exceptionsConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(exceptionsBunch), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outExceptionsConcept = bunchFinder.find("excepciones");

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(conceptAsBunchId(outExceptionsConcept), outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingAcceptationWithEmptyDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId exceptionsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, exceptionsConcept, "excepciones");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final BunchId exceptionsBunch = conceptAsBunchId(exceptionsConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(exceptionsBunch), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outExceptionsConcept = bunchFinder.find("excepciones");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(conceptAsBunchId(outExceptionsConcept), outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptationWithMatchingAcceptationInDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId exceptionsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, exceptionsConcept, "excepciones");

        final ConceptId palate = inManager.getNextAvailableConceptId();
        final AcceptationId palateAcc = addSimpleAcceptation(inManager, alphabet, palate, "paladar");

        final BunchId exceptionsBunch = conceptAsBunchId(exceptionsConcept);
        inManager.addAcceptationInBunch(exceptionsBunch, palateAcc);

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(exceptionsBunch), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outExceptionsConcept = bunchFinder.find("excepciones");

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(conceptAsBunchId(outExceptionsConcept), outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingAcceptationWithMatchingAcceptationInDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId exceptionsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, exceptionsConcept, "excepciones");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final ConceptId palate = inManager.getNextAvailableConceptId();
        final AcceptationId palateAcc = addSimpleAcceptation(inManager, alphabet, palate, "paladar");

        final BunchId exceptionsBunch = conceptAsBunchId(exceptionsConcept);
        inManager.addAcceptationInBunch(exceptionsBunch, palateAcc);

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(exceptionsBunch), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outExceptionsConcept = bunchFinder.find("excepciones");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(conceptAsBunchId(outExceptionsConcept), outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithTargetWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId gerundConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerundConcept, "gerundios");

        final BunchId gerundBunch = conceptAsBunchId(gerundConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(gerundBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outGerundBunchConcept = bunchFinder.find("gerundios");

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(conceptAsBunchId(outGerundBunchConcept), outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
        assertEmpty(outManager.getAcceptationsInBunch(conceptAsBunchId(outGerundBunchConcept)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithMultipleTargetsWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId gerundConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerundConcept, "gerundios");

        final ConceptId secondConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, secondConcept, "mis palabras");

        final BunchId gerundBunch = conceptAsBunchId(gerundConcept);
        final BunchId secondBunch = conceptAsBunchId(secondConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(gerundBunch, secondBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outGerundBunchConcept = bunchFinder.find("gerundios");
        final ConceptId outSecondBunchConcept = bunchFinder.find("mis palabras");

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);

        final BunchId outGerundBunch = conceptAsBunchId(outGerundBunchConcept);
        final BunchId outSecondBunch = conceptAsBunchId(outSecondBunchConcept);
        assertContainsOnly(outGerundBunch, outSecondBunch, outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
        assertEmpty(outManager.getAcceptationsInBunch(outGerundBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outSecondBunch));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithTargetWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId gerundConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerundConcept, "gerundios");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final BunchId gerundBunch = conceptAsBunchId(gerundConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(gerundBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outGerundBunchConcept = bunchFinder.find("gerundios");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);

        final BunchId outGerundBunch = conceptAsBunchId(outGerundBunchConcept);
        assertContainsOnly(outGerundBunch, outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
        assertContainsOnly(processedMap.valueAt(0), outManager.getAcceptationsInBunch(outGerundBunch));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithMultipleTargetsWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId gerundConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerundConcept, "gerundios");

        final ConceptId secondConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, secondConcept, "mis palabras");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final BunchId gerundBunch = conceptAsBunchId(gerundConcept);
        final BunchId secondBunch = conceptAsBunchId(secondConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(gerundBunch, secondBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outGerundBunchConcept = bunchFinder.find("gerundios");
        final ConceptId outSecondBunchConcept = bunchFinder.find("mis palabras");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outGerundBunchBunch = conceptAsBunchId(outGerundBunchConcept);
        final BunchId outSecondBunch = conceptAsBunchId(outSecondBunchConcept);
        assertContainsOnly(outGerundBunchBunch, outSecondBunch, outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
        assertContainsOnly(processedMap.valueAt(0), outManager.getAcceptationsInBunch(outGerundBunchBunch));
        assertContainsOnly(processedMap.valueAt(0), outManager.getAcceptationsInBunch(outSecondBunch));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingBothStaticAndDynamicAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId repeat = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, repeat, "repetición");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final RuleId repeatRule = conceptAsRuleId(repeat);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(), alphabet, null, null, "ar", "arar", repeatRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outRepeatConcept = bunchFinder.find("repetición");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "arar", outAgentDetails.endAdder);

        final RuleId outRepeatRule = conceptAsRuleId(outRepeatConcept);
        assertEquals(outRepeatRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outRepeatRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithEmptySourceBunch() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");

        final ConceptId concept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, concept, "cantar");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
        assertEmpty(outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertContainsOnly(outVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertNull(outManager.findRuledConcept(outGerundRule, outSingConcept));
        assertNull(outManager.findRuledAcceptationByAgentAndBaseAcceptation(outAgentId, outSingAcceptation));
    }

    @Test
    default void testSerializeAgentApplyingRuleNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId concept = inManager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSimpleAcceptation(inManager, alphabet, concept, "cantar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAcceptationInBunch(verbBunch, acceptation);

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(verbBunch), setOf(), alphabet, null, null, "er", "iendo", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
        assertContainsOnly(outVerbBunch, outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertContainsOnly(outVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "er", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "iendo", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
        assertNull(outManager.findRuledAcceptationByAgentAndBaseAcceptation(outAgentId, outSingAcceptation));
    }

    @Test
    default void testSerializeAgentApplyingRuleMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId concept = inManager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSimpleAcceptation(inManager, alphabet, concept, "cantar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAcceptationInBunch(verbBunch, acceptation);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
        assertContainsOnly(outVerbBunch, outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertContainsOnly(outVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);
        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ConceptId outSingingConcept = outManager.findRuledConcept(outGerundRule, outSingConcept);
        final AcceptationId outSingingAcceptation = outManager.findRuledAcceptationByAgentAndBaseAcceptation(outAgentId, outSingAcceptation);
        assertNotEquals(outSingAcceptation, outSingingAcceptation);

        assertEquals(outSingingConcept, outManager.conceptFromAcceptation(outSingingAcceptation));
        assertSinglePair(outAlphabet, "cantando", outManager.getAcceptationTexts(outSingingAcceptation));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(setOf(verbBunch), setOf(arVerbBunch), setOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToMultipleTargetsAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId transitiveVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, transitiveVerbConcept, "verbo transitivo");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId transitiveVerbBunch = conceptAsBunchId(transitiveVerbConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(setOf(verbBunch, transitiveVerbBunch), setOf(arVerbBunch), setOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outTransitiveVerbConcept = bunchFinder.find("verbo transitivo");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outTransitiveVerbBunch = conceptAsBunchId(outTransitiveVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        assertContainsOnly(outVerbBunch, outTransitiveVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outTransitiveVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetAgentWithAMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        final ImmutableSet<BunchId> sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAgent(setOf(verbBunch), sourceBunches, noBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToMultipleTargetsAgentWithAMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId secondConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, secondConcept, "mis palabras");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        final ImmutableSet<BunchId> sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId secondBunch = conceptAsBunchId(secondConcept);
        inManager.addAgent(setOf(verbBunch, secondBunch), sourceBunches, noBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outSecondConcept = bunchFinder.find("mis palabras");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outSecondBunch = conceptAsBunchId(outSecondConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        assertContainsOnly(outVerbBunch, outSecondBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outSecondBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        final ImmutableSet<BunchId> sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableSet<BunchId> diffBunches = noBunches.add(arEndedNounsBunch);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAgent(setOf(verbBunch), sourceBunches, diffBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToMultipleTargetsWithDiffBunchAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId actionConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, actionConcept, "acción");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId actionBunch = conceptAsBunchId(actionConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        final ImmutableSet<BunchId> targetBunches = setOf(verbBunch, actionBunch);
        final ImmutableSet<BunchId> sourceBunches = setOf(arVerbBunch);
        final ImmutableSet<BunchId> diffBunches = setOf(arEndedNounsBunch);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        inManager.addAgent(targetBunches, sourceBunches, diffBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");
        final ConceptId outActionConcept = bunchFinder.find("acción");

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outActionBunch = conceptAsBunchId(outActionConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outActionBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outActionBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        inManager.addAgent(setOf(verbBunch), setOf(arVerbBunch), setOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToMultipleTargetsWithDiffBunchAgentWithMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId actionConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, actionConcept, "acción");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId actionBunch = conceptAsBunchId(actionConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        inManager.addAgent(setOf(verbBunch, actionBunch), setOf(arVerbBunch), setOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNoundConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");
        final ConceptId outActionConcept = bunchFinder.find("acción");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outActionBunch = conceptAsBunchId(outActionConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNoundBunch = conceptAsBunchId(outArEndedNoundConcept);
        assertContainsOnly(outVerbBunch, outActionBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNoundBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNoundBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outActionBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptationsInBothSourceAndDiffBunches() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);
        inManager.addAcceptationInBunch(arEndedNounsBunch, inAcceptation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAgent(setOf(verbBunch), setOf(arVerbBunch), setOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptationsOnlyInDiffBunch() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        inManager.addAcceptationInBunch(arEndedNounsBunch, inAcceptation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAgent(setOf(verbBunch), setOf(arVerbBunch), setOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithConversionPresent() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> inManager = createManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final AlphabetId upperCaseAlphabet = getNextAvailableId(inManager);
        final Conversion<AlphabetId> conversion = new Conversion<>(alphabet, upperCaseAlphabet, AcceptationsSerializerTest.upperCaseConversion);
        inManager.addAlphabetAsConversionTarget(conversion);

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = outManager.getConversionsMap();
        assertSize(1, conversionMap);
        final AlphabetId outAlphabet = conversionMap.valueAt(0);
        final AlphabetId outUpperCaseAlphabet = conversionMap.keyAt(0);

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final int outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder);

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));

        final ImmutableCorrelation<AlphabetId> texts = outManager.getAcceptationTexts(processedMap.valueAt(0));
        assertSize(2, texts);
        assertEquals("cantando", texts.get(outAlphabet));
        assertEquals("CANTANDO", texts.get(outUpperCaseAlphabet));
    }

    @Test
    default void testSerializeAgentWithJustEndAdderForAcceptationFromOtherLanguage() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> manager = createManager(inDb);
        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final ConceptId singConcept = manager.getNextAvailableConceptId();
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, esAlphabet, singConcept, "cantar");

        final ConceptId myConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, myConcept, "palabras");
        final BunchId myBunch = conceptAsBunchId(myConcept);
        manager.addAcceptationInBunch(myBunch, singAcceptation);

        final ConceptId studyConcept = manager.getNextAvailableConceptId();
        final AcceptationId studyAcceptation = addSimpleAcceptation(manager, jaAlphabet, studyConcept, "べんきょう");
        manager.addAcceptationInBunch(myBunch, studyAcceptation);

        final ConceptId verbalitationConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, verbalitationConcept, "verbalización");

        final RuleId verbalitationRule = conceptAsRuleId(verbalitationConcept);
        addSingleAlphabetAgent(manager, setOf(), setOf(myBunch), setOf(), jaAlphabet, null, null, null, "する", verbalitationRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final AcceptationId outStudyAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "べんきょう"));
        final int outAgentId = getSingleValue(outManager.getAgentIds());
        assertContainsOnly(outStudyAcceptation, manager.getAgentProcessedMap(outAgentId).keySet());
    }

    @Test
    default void testSerializeChainedAgentsApplyingRules() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> manager = createManager(inDb);
        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final ConceptId studentConcept = manager.getNextAvailableConceptId();
        final AcceptationId studentAcceptation = addSimpleAcceptation(manager, esAlphabet, studentConcept, "alumno");

        final ConceptId concept1 = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, concept1, "palabras masculinas");
        final BunchId bunch1 = conceptAsBunchId(concept1);
        manager.addAcceptationInBunch(bunch1, studentAcceptation);

        final ConceptId concept2 = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, concept2, "palabras femeninas");

        final ConceptId femenineConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, femenineConcept, "femenino");

        final ConceptId pluralConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, pluralConcept, "plural");

        final BunchId bunch2 = conceptAsBunchId(concept2);
        final RuleId femenineRule = conceptAsRuleId(femenineConcept);
        addSingleAlphabetAgent(manager, setOf(bunch2), setOf(bunch1), setOf(), esAlphabet, null, null, "o", "a", femenineRule);

        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        addSingleAlphabetAgent(manager, setOf(), setOf(bunch1, bunch2), setOf(), esAlphabet, null, null, null, "s", pluralRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> outManager = createManager(outDb);

        final AcceptationId outStudentAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "alumno"));
        final ConceptId outStudentConcept = outManager.conceptFromAcceptation(outStudentAcceptation);

        final AcceptationId outFemaleStudentAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "alumna"));
        final ConceptId outFemaleStudentConcept = outManager.conceptFromAcceptation(outFemaleStudentAcceptation);

        final AcceptationId outStudentsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "alumnos"));
        final ConceptId outStudentsConcept = outManager.conceptFromAcceptation(outStudentsAcceptation);

        final AcceptationId outFemaleStudentsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "alumnas"));
        final ConceptId outFemaleStudentsConcept = outManager.conceptFromAcceptation(outFemaleStudentsAcceptation);

        final AcceptationId outFemenineRuleAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "femenino"));
        final ConceptId outFemenineConcept = outManager.conceptFromAcceptation(outFemenineRuleAcceptation);

        final AcceptationId outPluralRuleAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "plural"));
        final ConceptId outPluralConcept = outManager.conceptFromAcceptation(outPluralRuleAcceptation);

        final RuleId outFemenineRule = conceptAsRuleId(outFemenineConcept);
        assertSinglePair(outFemaleStudentConcept, outStudentConcept, outManager.findRuledConceptsByRule(outFemenineRule));

        final RuleId outPluralRule = conceptAsRuleId(outPluralConcept);
        final ImmutableMap<ConceptId, ConceptId> pluralProcessedConcepts = outManager.findRuledConceptsByRule(outPluralRule);
        assertSize(2, pluralProcessedConcepts);
        assertEquals(outStudentConcept, pluralProcessedConcepts.get(outStudentsConcept));
        assertEquals(outFemaleStudentConcept, pluralProcessedConcepts.get(outFemaleStudentsConcept));

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ImmutableSet<MorphologyResult<AcceptationId, RuleId>> morphologies = outManager.readMorphologiesFromAcceptation(outStudentAcceptation, outAlphabet).morphologies.toSet();
        assertSize(3, morphologies);
        assertContainsOnly(outFemenineRule, morphologies.findFirst(result -> result.dynamicAcceptation.equals(outFemaleStudentAcceptation), null).rules);
        assertContainsOnly(outPluralRule, morphologies.findFirst(result -> result.dynamicAcceptation.equals(outStudentsAcceptation), null).rules);
        assertContainsOnly(outFemenineRule, outPluralRule, morphologies.findFirst(result -> result.dynamicAcceptation.equals(outFemaleStudentsAcceptation), null).rules);
    }
}
