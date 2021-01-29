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
import static sword.langbook3.android.db.LangbookReadableDatabase.findRuledAcceptationByRuleAndBaseAcceptation;
import static sword.langbook3.android.db.SentencesManagerTestUtils.newSpan;

interface LangbookManagerTest<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> extends QuizzesManagerTest<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId>, DefinitionsManagerTest, SentencesManagerTest<LanguageId, AlphabetId, SymbolArrayId, CorrelationId> {

    @Override
    LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> createManager(MemoryDatabase db);

    @Test
    default void testAddDynamicAcceptationInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();

        final ImmutableCorrelation<AlphabetId> adder = emptyCorrelation.put(esAlphabet, "s");

        final int pluralRule = manager.getMaxConcept() + 1;
        assertNotNull(manager.addAgent(intSetOf(), intSetOf(substantiveConcept), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule));

        assertTrue(manager.addAcceptationInBunch(substantiveConcept, carAcc));
        final int carPluralAcc = findRuledAcceptationByRuleAndBaseAcceptation(db, pluralRule, carAcc);

        final String text = "Los coches son muy rápidos";
        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
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
        final LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int mineConcept = manager.getMaxConcept() + 1;
        final int mineAcc = addSimpleAcceptation(manager, esAlphabet, mineConcept, "mío");

        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelation<AlphabetId> adder = emptyCorrelation.put(esAlphabet, "s");

        final int pluralRule = manager.getMaxConcept() + 1;
        assertNotNull(manager.addAgent(intSetOf(), intSetOf(substantiveConcept), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule));

        assertTrue(manager.addAcceptationInBunch(substantiveConcept, carAcc));
        final int carPluralAcc = findRuledAcceptationByRuleAndBaseAcceptation(db, pluralRule, carAcc);

        final String text = "El mejor de los coches es el mío";
        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(newSpan(text, "coches", carPluralAcc))
                .add(newSpan(text, "mío", mineAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);
        assertTrue(manager.removeAcceptationFromBunch(substantiveConcept, carAcc));
        assertEmpty(manager.getSampleSentences(carAcc));

        assertSinglePair(sentence, text, manager.getSampleSentences(mineAcc));
        assertEquals(mineAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }

    @Test
    default void testRemoveAgentWhoseOutputIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int mineConcept = manager.getMaxConcept() + 1;
        final int mineAcc = addSimpleAcceptation(manager, esAlphabet, mineConcept, "mío");

        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();

        final ImmutableCorrelation<AlphabetId> adder = emptyCorrelation.put(esAlphabet, "s");

        final int pluralRule = manager.getMaxConcept() + 1;
        final int agentId = manager.addAgent(intSetOf(), intSetOf(substantiveConcept), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule);

        assertTrue(manager.addAcceptationInBunch(substantiveConcept, carAcc));
        final int carPluralAcc = findRuledAcceptationByRuleAndBaseAcceptation(db, pluralRule, carAcc);

        final String text = "El mejor de los coches es el mío";
        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
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
        final LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int brushConcept = manager.getMaxConcept() + 1;
        final int brushAcc = addSimpleAcceptation(manager, esAlphabet, brushConcept, "cepillar");

        final int toothConcept = manager.getMaxConcept() + 1;
        final int toothAcc = addSimpleAcceptation(manager, esAlphabet, toothConcept, "diente");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, arVerbConcept, "verbo de primera conjugación");

        final int pluralableConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, pluralableConcept, "pluralizable");

        final int firstPersonConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, firstPersonConcept, "primera persona");

        final int pluralConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, pluralConcept, "plural");

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final int headAgent = addSingleAlphabetAgent(manager, intSetOf(arVerbConcept), noBunches, noBunches, esAlphabet, null, null, "ar", "ar", 0);

        final int tailAgent = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(arVerbConcept), noBunches, esAlphabet, null, null, "ar", "o", firstPersonConcept);

        final int pluralAgent = addSingleAlphabetAgent(manager, intSetOf(), intSetOf(pluralableConcept), noBunches, esAlphabet, null, null, "", "s", pluralConcept);

        assertTrue(manager.addAcceptationInBunch(pluralableConcept, toothAcc));

        final int iBrushAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(tailAgent, brushAcc);
        final int teethAcc = manager.findRuledAcceptationByAgentAndBaseAcceptation(pluralAgent, toothAcc);

        final String text = "Me cepillo los dientes cada día";

        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
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
