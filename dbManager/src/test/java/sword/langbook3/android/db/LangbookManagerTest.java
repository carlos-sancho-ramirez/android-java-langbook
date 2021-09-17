package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.db.AcceptationsManagerTest.obtainNewAcceptation;
import static sword.langbook3.android.db.AgentsManagerTest.addSingleAlphabetAgent;
import static sword.langbook3.android.db.AgentsManagerTest.composeSingleElementArray;
import static sword.langbook3.android.db.AgentsManagerTest.setOf;
import static sword.langbook3.android.db.SentencesManagerTestUtils.newSpan;

interface LangbookManagerTest<ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId extends AcceptationIdInterface, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId extends AgentIdInterface, QuizId, SentenceId extends SentenceIdInterface> extends QuizzesManagerTest<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId>, DefinitionsManagerTest<ConceptId>, RuledSentencesManagerTest<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> {

    @Override
    LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> createManager(MemoryDatabase db);

    @Test
    default void testAddDynamicAcceptationInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AcceptationId carAcc = obtainNewAcceptation(manager, esAlphabet, "coche");
        final BunchId substantiveBunch = obtainNewBunch(manager, esAlphabet, "sustantivo");

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final ImmutableCorrelationArray<AlphabetId> adder = composeSingleElementArray(emptyCorrelation.put(esAlphabet, "s"));

        final RuleId pluralRule = obtainNewRule(manager, esAlphabet, "plural");
        assertNotNull(manager.addAgent(setOf(), setOf(substantiveBunch), setOf(), emptyCorrelation, emptyCorrelationArray, emptyCorrelation, adder, pluralRule));

        assertTrue(manager.addAcceptationInBunch(substantiveBunch, carAcc));
        final AcceptationId carPluralAcc = manager.findRuledAcceptationByRuleAndBaseAcceptation(pluralRule, carAcc);

        final String text = "Los coches son muy rápidos";
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "coches", carPluralAcc))
                .build();

        final ConceptId concept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(concept, text, spans);

        assertSinglePair(sentence, text, manager.getSampleSentences(carAcc));
        assertEquals(carPluralAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }

    @Test
    default void testRemoveDynamicAcceptationFromBunchUsedAsSourceForAgentWhoseOutputIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId carAcc = obtainNewAcceptation(manager, esAlphabet, "coche");
        final AcceptationId mineAcc = obtainNewAcceptation(manager, esAlphabet, "mío");

        final BunchId substantiveBunch = obtainNewBunch(manager, esAlphabet, "sustantivo");

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final ImmutableCorrelationArray<AlphabetId> adder = composeSingleElementArray(emptyCorrelation.put(esAlphabet, "s"));

        final RuleId pluralRule = obtainNewRule(manager, esAlphabet, "plural");
        assertNotNull(manager.addAgent(setOf(), setOf(substantiveBunch), setOf(), emptyCorrelation, emptyCorrelationArray, emptyCorrelation, adder, pluralRule));

        assertTrue(manager.addAcceptationInBunch(substantiveBunch, carAcc));
        final AcceptationId carPluralAcc = manager.findRuledAcceptationByRuleAndBaseAcceptation(pluralRule, carAcc);

        final String text = "El mejor de los coches es el mío";
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "coches", carPluralAcc))
                .add(newSpan(text, "mío", mineAcc))
                .build();

        final ConceptId concept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(concept, text, spans);
        assertTrue(manager.removeAcceptationFromBunch(substantiveBunch, carAcc));
        assertEmpty(manager.getSampleSentences(carAcc));

        assertSinglePair(sentence, text, manager.getSampleSentences(mineAcc));
        assertEquals(mineAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }

    @Test
    default void testRemoveAgentWhoseOutputIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId carAcc = obtainNewAcceptation(manager, esAlphabet, "coche");
        final AcceptationId mineAcc = obtainNewAcceptation(manager, esAlphabet, "mío");
        final BunchId substantiveBunch = obtainNewBunch(manager, esAlphabet, "sustantivo");

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final ImmutableCorrelationArray<AlphabetId> adder = composeSingleElementArray(emptyCorrelation.put(esAlphabet, "s"));

        final RuleId pluralRule = obtainNewRule(manager, esAlphabet, "plural");
        final AgentId agentId = manager.addAgent(setOf(), setOf(substantiveBunch), setOf(), emptyCorrelation, emptyCorrelationArray, emptyCorrelation, adder, pluralRule);

        assertTrue(manager.addAcceptationInBunch(substantiveBunch, carAcc));
        final AcceptationId carPluralAcc = manager.findRuledAcceptationByRuleAndBaseAcceptation(pluralRule, carAcc);

        final String text = "El mejor de los coches es el mío";
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "coches", carPluralAcc))
                .add(newSpan(text, "mío", mineAcc))
                .build();

        final ConceptId concept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(concept, text, spans);
        manager.removeAgent(agentId);
        assertTrue(manager.getSampleSentences(carAcc).isEmpty());

        assertSinglePair(sentence, text, manager.getSampleSentences(mineAcc));
        assertEquals(mineAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }

    @Test
    default void testRemoveHeadChainedAgentWhereRuledAcceptationOfTheTailChainedAgentIsUsedAsSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId brushAcc = obtainNewAcceptation(manager, esAlphabet, "cepillar");
        final AcceptationId toothAcc = obtainNewAcceptation(manager, esAlphabet, "diente");

        final BunchId arVerbBunch = obtainNewBunch(manager, esAlphabet, "verbo de primera conjugación");
        final BunchId pluralableBunch = obtainNewBunch(manager, esAlphabet, "pluralizable");
        final RuleId firstPersonRule = obtainNewRule(manager, esAlphabet, "primera persona");
        final RuleId pluralRule = obtainNewRule(manager, esAlphabet, "plural");

        final AgentId headAgent = addSingleAlphabetAgent(manager, setOf(arVerbBunch), setOf(), setOf(), esAlphabet, null, null, "ar", "ar", null);
        final AgentId tailAgent = addSingleAlphabetAgent(manager, setOf(), setOf(arVerbBunch), setOf(), esAlphabet, null, null, "ar", "o", firstPersonRule);
        final AgentId pluralAgent = addSingleAlphabetAgent(manager, setOf(), setOf(pluralableBunch), setOf(), esAlphabet, null, null, "", "s", pluralRule);

        assertTrue(manager.addAcceptationInBunch(pluralableBunch, toothAcc));

        final AcceptationId iBrushAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(tailAgent, brushAcc);
        final AcceptationId teethAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(pluralAgent, toothAcc);

        final String text = "Me cepillo los dientes cada día";

        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "cepillo", iBrushAcc))
                .add(newSpan(text, "dientes", teethAcc))
                .build();

        final ConceptId concept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(concept, text, spans);

        manager.removeAgent(headAgent);

        assertEmpty(manager.getSampleSentences(brushAcc));
        assertSinglePair(sentence, text, manager.getSampleSentences(toothAcc));
        assertEquals(teethAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }
}
