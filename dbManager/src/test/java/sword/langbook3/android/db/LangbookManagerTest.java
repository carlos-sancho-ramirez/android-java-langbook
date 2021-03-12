package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.IntKeyMapTestUtils.assertSinglePair;
import static sword.collections.IntSetTestUtils.intSetOf;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AgentsManagerTest.addSingleAlphabetAgent;
import static sword.langbook3.android.db.AgentsManagerTest.setOf;
import static sword.langbook3.android.db.SentencesManagerTestUtils.newSpan;

interface LangbookManagerTest<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId extends AcceptationIdInterface, BunchId, RuleId> extends QuizzesManagerTest<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId>, DefinitionsManagerTest, SentencesManagerTest<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId> {

    @Override
    LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, RuleId> createManager(MemoryDatabase db);

    @Test
    default void testAddDynamicAcceptationInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final AcceptationId carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();

        final ImmutableCorrelation<AlphabetId> adder = emptyCorrelation.put(esAlphabet, "s");

        final int pluralConcept = manager.getMaxConcept() + 1;
        final BunchId substantiveBunch = conceptAsBunchId(substantiveConcept);
        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        assertNotNull(manager.addAgent(setOf(), setOf(substantiveBunch), setOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule));

        assertTrue(manager.addAcceptationInBunch(substantiveBunch, carAcc));
        final AcceptationId carPluralAcc = manager.findRuledAcceptationByRuleAndBaseAcceptation(pluralRule, carAcc);

        final String text = "Los coches son muy rápidos";
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "coches", carPluralAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);

        assertSinglePair(sentence, text, manager.getSampleSentences(carAcc));
        assertEquals(carPluralAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }

    @Test
    default void testRemoveDynamicAcceptationFromBunchUsedAsSourceForAgentWhoseOutputIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final AcceptationId carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int mineConcept = manager.getMaxConcept() + 1;
        final AcceptationId mineAcc = addSimpleAcceptation(manager, esAlphabet, mineConcept, "mío");

        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelation<AlphabetId> adder = emptyCorrelation.put(esAlphabet, "s");

        final int pluralConcept = manager.getMaxConcept() + 1;
        final BunchId substantiveBunch = conceptAsBunchId(substantiveConcept);
        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        assertNotNull(manager.addAgent(setOf(), setOf(substantiveBunch), setOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule));

        assertTrue(manager.addAcceptationInBunch(substantiveBunch, carAcc));
        final AcceptationId carPluralAcc = manager.findRuledAcceptationByRuleAndBaseAcceptation(pluralRule, carAcc);

        final String text = "El mejor de los coches es el mío";
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "coches", carPluralAcc))
                .add(newSpan(text, "mío", mineAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);
        assertTrue(manager.removeAcceptationFromBunch(substantiveBunch, carAcc));
        assertEmpty(manager.getSampleSentences(carAcc));

        assertSinglePair(sentence, text, manager.getSampleSentences(mineAcc));
        assertEquals(mineAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }

    @Test
    default void testRemoveAgentWhoseOutputIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final AcceptationId carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int mineConcept = manager.getMaxConcept() + 1;
        final AcceptationId mineAcc = addSimpleAcceptation(manager, esAlphabet, mineConcept, "mío");

        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();

        final ImmutableCorrelation<AlphabetId> adder = emptyCorrelation.put(esAlphabet, "s");

        final int pluralConcept = manager.getMaxConcept() + 1;
        final BunchId substantiveBunch = conceptAsBunchId(substantiveConcept);
        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        final int agentId = manager.addAgent(setOf(), setOf(substantiveBunch), setOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule);

        assertTrue(manager.addAcceptationInBunch(substantiveBunch, carAcc));
        final AcceptationId carPluralAcc = manager.findRuledAcceptationByRuleAndBaseAcceptation(pluralRule, carAcc);

        final String text = "El mejor de los coches es el mío";
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "coches", carPluralAcc))
                .add(newSpan(text, "mío", mineAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);
        manager.removeAgent(agentId);
        assertTrue(manager.getSampleSentences(carAcc).isEmpty());

        assertSinglePair(sentence, text, manager.getSampleSentences(mineAcc));
        assertEquals(mineAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }

    @Test
    default void testRemoveHeadChainedAgentWhereRuledAcceptationOfTheTailChainedAgentIsUsedAsSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int brushConcept = manager.getMaxConcept() + 1;
        final AcceptationId brushAcc = addSimpleAcceptation(manager, esAlphabet, brushConcept, "cepillar");

        final int toothConcept = manager.getMaxConcept() + 1;
        final AcceptationId toothAcc = addSimpleAcceptation(manager, esAlphabet, toothConcept, "diente");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, arVerbConcept, "verbo de primera conjugación");

        final int pluralableConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, pluralableConcept, "pluralizable");

        final int firstPersonConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, firstPersonConcept, "primera persona");

        final int pluralConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, pluralConcept, "plural");

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final int headAgent = addSingleAlphabetAgent(manager, setOf(arVerbBunch), setOf(), setOf(), esAlphabet, null, null, "ar", "ar", null);

        final RuleId firstPersonRule = conceptAsRuleId(firstPersonConcept);
        final int tailAgent = addSingleAlphabetAgent(manager, setOf(), setOf(arVerbBunch), setOf(), esAlphabet, null, null, "ar", "o", firstPersonRule);

        final BunchId pluralableBunch = conceptAsBunchId(pluralableConcept);
        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        final int pluralAgent = addSingleAlphabetAgent(manager, setOf(), setOf(pluralableBunch), setOf(), esAlphabet, null, null, "", "s", pluralRule);

        assertTrue(manager.addAcceptationInBunch(pluralableBunch, toothAcc));

        final AcceptationId iBrushAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(tailAgent, brushAcc);
        final AcceptationId teethAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(pluralAgent, toothAcc);

        final String text = "Me cepillo los dientes cada día";

        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "cepillo", iBrushAcc))
                .add(newSpan(text, "dientes", teethAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);

        manager.removeAgent(headAgent);

        assertEmpty(manager.getSampleSentences(brushAcc));
        assertSinglePair(sentence, text, manager.getSampleSentences(toothAcc));
        assertEquals(teethAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }
}
