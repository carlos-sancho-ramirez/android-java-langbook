package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AgentsChecker2;
import sword.langbook3.android.db.AgentsManager2;
import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.LanguageIdInterface;
import sword.langbook3.android.models.AgentDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.getSingleValue;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
interface AgentsSerializerTest<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> extends BunchesSerializerTest<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId>, AgentsSerializer1Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> {

    static <T> ImmutableSet<T> setOf() {
        return ImmutableHashSet.empty();
    }

    @Test
    default void testSerializeAgentApplyingRuleWithSameMatcherAndAdders() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("de").mainAlphabet;

        final ConceptId firstPersonOfPlural = inManager.getNextAvailableConceptId();
        AgentsSerializer0Test.addSimpleAcceptation(inManager, alphabet, firstPersonOfPlural, "erste Person Plural");

        final RuleId firstPersonOfPluralRule = conceptAsRuleId(firstPersonOfPlural);
        AgentsSerializer0Test.addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(), alphabet, null, null, "en", "en", firstPersonOfPluralRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("de");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptId outFirstPersonOfPluralConcept = newConceptFinder(outDb, outManager).find("erste Person Plural");
        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "en", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "en", outAgentDetails.endAdder.valueAt(0));

        final RuleId outFirstPersonOfPluralRule = conceptAsRuleId(outFirstPersonOfPluralConcept);
        assertEquals(outFirstPersonOfPluralRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outFirstPersonOfPluralRule));
    }
}
