package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.LanguageIdInterface;
import sword.langbook3.android.db.RuledSentencesChecker2;
import sword.langbook3.android.db.RuledSentencesManager;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.StringTestUtils.rangeOf;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.AcceptationsSerializer0Test.findAcceptationsMatchingText;
import static sword.langbook3.android.sdb.AgentsSerializer0Test.setOf;

interface RuledSentencesSerializer0Test<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, SentenceId> extends AgentsSerializer0Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> {

    @Override
    RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> createInManager(MemoryDatabase db);

    @Override
    RuledSentencesChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> createOutChecker(MemoryDatabase db);

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> AcceptationId addSimpleAcceptation(
            AcceptationsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, ConceptId concept, String text) {
        return AcceptationsSerializer0Test.addSimpleAcceptation(manager, alphabet, concept, text);
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> ConceptId obtainNewConcept(
            AcceptationsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, String text) {
        final ConceptId newConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, newConcept, text));
        return newConcept;
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> AcceptationId obtainNewAcceptation(
            AcceptationsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, String text) {
        final ConceptId newConcept = manager.getNextAvailableConceptId();
        final AcceptationId newAcceptation = addSimpleAcceptation(manager, alphabet, newConcept, text);
        assertNotNull(newAcceptation);
        return newAcceptation;
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> AgentId addSingleAlphabetAgent(
            AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
            ImmutableSet<BunchId> diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, RuleId rule) {
        return AgentsSerializer0Test.addSingleAlphabetAgent(manager, targetBunches, sourceBunches, diffBunches, alphabet, startMatcherText, startAdderText, endMatcherText, endAdderText, rule);
    }

    static <AcceptationId> SentenceSpan<AcceptationId> newSpan(String text, String segment, AcceptationId acceptation) {
        return new SentenceSpan<>(rangeOf(text, segment), acceptation);
    }

    default RuleId obtainNewRule(AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, AlphabetId alphabet, String text) {
        return conceptAsRuleId(obtainNewConcept(manager, alphabet, text));
    }

    @Test
    default void testSerializeSentenceWithDynamicAcceptationAsSpan() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> inManager = createInManager(inDb);
        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId feminableWordsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, feminableWordsConcept, "feminizable");

        final ConceptId pluralableWordsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, pluralableWordsConcept, "pluralizable");

        final AcceptationId boyAcc = obtainNewAcceptation(inManager, alphabet, "chico");
        final BunchId feminableWordsBunch = conceptAsBunchId(feminableWordsConcept);
        inManager.addAcceptationInBunch(feminableWordsBunch, boyAcc);

        final BunchId pluralableWordsBunch = conceptAsBunchId(pluralableWordsConcept);
        final RuleId femenineRule = obtainNewRule(inManager, alphabet, "femenino");
        final AgentId agent1 = addSingleAlphabetAgent(inManager, setOf(pluralableWordsBunch), setOf(feminableWordsBunch), setOf(), alphabet, null, null, "o", "a", femenineRule);

        final RuleId pluralRule = obtainNewRule(inManager, alphabet, "plural");
        final AgentId agent2 = addSingleAlphabetAgent(inManager, setOf(), setOf(pluralableWordsBunch), setOf(), alphabet, null, null, null, "s", pluralRule);

        final AcceptationId girlAcc = inManager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, boyAcc);
        final AcceptationId girlsAcc = inManager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, girlAcc);

        final String sentenceText = "las chicas salieron juntas";
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(sentenceText, "chicas", girlsAcc))
                .build();

        final ConceptId sentenceConcept = inManager.getNextAvailableConceptId();
        inManager.addSentence(sentenceConcept, sentenceText, spans);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final RuledSentencesChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> outManager = createOutChecker(outDb);

        final AcceptationId outBoyAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "chico"));
        final ImmutableMap<SentenceId, String> outSentences = outManager.getSampleSentences(outBoyAcceptation);
        assertContainsOnly(sentenceText, outSentences);

        final AcceptationId outGirlsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "chicas"));
        final SentenceId outSentenceId = outSentences.keyAt(0);
        final SentenceSpan<AcceptationId> outSpan = getSingleValue(outManager.getSentenceSpans(outSentenceId));
        assertEquals(rangeOf(sentenceText, "chicas"), outSpan.range);
        assertEquals(outGirlsAcceptation, outSpan.acceptation);

        final AcceptationId outFemenineAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "femenino"));
        final RuleId outFemenineRule = conceptAsRuleId(outManager.conceptFromAcceptation(outFemenineAcceptation));
        assertSinglePair(outSentenceId, sentenceText, outManager.getSampleSentencesApplyingRule(outFemenineRule));

        final AcceptationId outPluralAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "plural"));
        final RuleId outPluralRule = conceptAsRuleId(outManager.conceptFromAcceptation(outPluralAcceptation));
        assertSinglePair(outSentenceId, sentenceText, outManager.getSampleSentencesApplyingRule(outPluralRule));

        final RuleId outBoyRule = conceptAsRuleId(outManager.conceptFromAcceptation(outBoyAcceptation));
        assertEmpty(outManager.getSampleSentencesApplyingRule(outBoyRule));

        final RuleId outGirlsRule = conceptAsRuleId(outManager.conceptFromAcceptation(outGirlsAcceptation));
        assertEmpty(outManager.getSampleSentencesApplyingRule(outGirlsRule));
    }
}
