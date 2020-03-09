package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AgentsManagerTest.addSingleAlphabetAgent;
import static sword.langbook3.android.db.IntKeyMapTestUtils.assertSinglePair;
import static sword.langbook3.android.db.IntSetTestUtils.intSetOf;
import static sword.langbook3.android.db.LangbookReadableDatabase.findRuledAcceptationByRuleAndBaseAcceptation;
import static sword.langbook3.android.db.SentencesManagerTestUtils.newSpan;
import static sword.langbook3.android.db.SizableTestUtils.assertEmpty;
import static sword.langbook3.android.db.TraversableTestUtils.getSingleValue;

interface LangbookManagerTest extends QuizzesManagerTest, DefinitionsManagerTest, SentencesManagerTest {

    @Override
    LangbookManager createManager(MemoryDatabase db);

    @Test
    default void testAddDynamicAcceptationInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();

        final ImmutableIntKeyMap<String> adder = emptyCorrelation.put(esAlphabet, "s");

        final int pluralRule = manager.getMaxConcept() + 1;
        assertNotNull(manager.addAgent(0, intSetOf(substantiveConcept), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule));

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
        final LangbookManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int mineConcept = manager.getMaxConcept() + 1;
        final int mineAcc = addSimpleAcceptation(manager, esAlphabet, mineConcept, "mío");

        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        final ImmutableIntKeyMap<String> adder = emptyCorrelation.put(esAlphabet, "s");

        final int pluralRule = manager.getMaxConcept() + 1;
        assertNotNull(manager.addAgent(0, intSetOf(substantiveConcept), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule));

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
        final LangbookManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int mineConcept = manager.getMaxConcept() + 1;
        final int mineAcc = addSimpleAcceptation(manager, esAlphabet, mineConcept, "mío");

        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();

        final ImmutableIntKeyMap<String> adder = emptyCorrelation.put(esAlphabet, "s");

        final int pluralRule = manager.getMaxConcept() + 1;
        final int agentId = manager.addAgent(0, intSetOf(substantiveConcept), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule);

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
        final LangbookManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int brushConcept = manager.getMaxConcept() + 1;
        final int brushAcc = addSimpleAcceptation(manager, esAlphabet, brushConcept, "cepillar");

        final int toothConcept = manager.getMaxConcept() + 1;
        final int toothAcc = addSimpleAcceptation(manager, esAlphabet, toothConcept, "diente");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int arVerbAcc = addSimpleAcceptation(manager, esAlphabet, arVerbConcept, "verbo de primera conjugación");

        final int pluralableConcept = manager.getMaxConcept() + 1;
        final int pluralableAcc = addSimpleAcceptation(manager, esAlphabet, pluralableConcept, "pluralizable");

        final int firstPersonConcept = manager.getMaxConcept() + 1;
        final int firstPersonAcc = addSimpleAcceptation(manager, esAlphabet, firstPersonConcept, "primera persona");

        final int pluralConcept = manager.getMaxConcept() + 1;
        final int pluralAcc = addSimpleAcceptation(manager, esAlphabet, pluralConcept, "plural");

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final int headAgent = addSingleAlphabetAgent(manager, arVerbConcept, noBunches, noBunches, esAlphabet, null, null, "ar", "ar", 0);

        final int tailAgent = addSingleAlphabetAgent(manager, 0, intSetOf(arVerbConcept), noBunches, esAlphabet, null, null, "ar", "o", firstPersonConcept);

        final int pluralAgent = addSingleAlphabetAgent(manager, 0, intSetOf(pluralableConcept), noBunches, esAlphabet, null, null, "", "s", pluralConcept);

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
