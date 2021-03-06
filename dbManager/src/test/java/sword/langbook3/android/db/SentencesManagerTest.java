package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.MapTestUtils.assertEqualMap;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.obtainNewAcceptation;
import static sword.langbook3.android.db.SentencesManagerTestUtils.newSpan;

interface SentencesManagerTest<ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> extends AcceptationsManagerTest<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> {

    @Override
    SentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> createManager(MemoryDatabase db);

    @Test
    default void testAddSentences() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;

        final AcceptationId carAcc = obtainNewAcceptation(manager, esAlphabet, "coche");
        final AcceptationId greatAcc = obtainNewAcceptation(manager, esAlphabet, "genial");

        final ConceptId redConcept = manager.getNextAvailableConceptId();
        final AcceptationId redEsAcc = addSimpleAcceptation(manager, esAlphabet, redConcept, "rojo");
        final AcceptationId redEnAcc = addSimpleAcceptation(manager, enAlphabet, redConcept, "red");

        final String text1 = "El coche es genial";
        final String text2 = "El coche es rojo";
        final String text3 = "The car is red";

        final ImmutableSet<SentenceSpan<AcceptationId>> spans1 = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text1, "coche", carAcc))
                .add(newSpan(text1, "genial", greatAcc))
                .build();

        final ImmutableSet<SentenceSpan<AcceptationId>> spans2 = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text2, "coche", carAcc))
                .add(newSpan(text2, "rojo", redEsAcc))
                .build();

        final ImmutableSet<SentenceSpan<AcceptationId>> spans3 = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text3, "red", redEnAcc))
                .build();

        final ConceptId concept1 = manager.getNextAvailableConceptId();
        final SentenceId sentence1 = manager.addSentence(concept1, text1, spans1);

        final ConceptId concept2 = manager.getNextAvailableConceptId();
        final SentenceId sentence2 = manager.addSentence(concept2, text2, spans2);
        final SentenceId sentence3 = manager.addSentence(concept2, text3, spans3);

        assertSinglePair(sentence1, text1, manager.getSampleSentences(greatAcc));
        assertSinglePair(sentence2, text2, manager.getSampleSentences(redEsAcc));
        assertSinglePair(sentence3, text3, manager.getSampleSentences(redEnAcc));

        final Map<SentenceId, String> expectedCarMatchingSentences = new ImmutableHashMap.Builder<SentenceId, String>()
                .put(sentence1, text1)
                .put(sentence2, text2)
                .build();
        assertEqualMap(expectedCarMatchingSentences, manager.getSampleSentences(carAcc));

        final SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> sentenceDetails1 = manager.getSentenceDetails(sentence1);
        final SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> sentenceDetails2 = manager.getSentenceDetails(sentence2);
        final SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> sentenceDetails3 = manager.getSentenceDetails(sentence3);

        assertEquals(text1, sentenceDetails1.text);
        assertEquals(spans1, sentenceDetails1.spans);
        assertEmpty(sentenceDetails1.sameMeaningSentences);

        assertEquals(text2, sentenceDetails2.text);
        assertEquals(spans2, sentenceDetails2.spans);
        assertSinglePair(sentence3, text3, sentenceDetails2.sameMeaningSentences);

        assertEquals(text3, sentenceDetails3.text);
        assertEquals(spans3, sentenceDetails3.spans);
        assertSinglePair(sentence2, text2, sentenceDetails3.sameMeaningSentences);
    }

    @Test
    default void testReplaceSentence() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;

        final AcceptationId carAcc = obtainNewAcceptation(manager, esAlphabet, "coche");
        final AcceptationId greatAcc = obtainNewAcceptation(manager, esAlphabet, "genial");

        final ConceptId redConcept = manager.getNextAvailableConceptId();
        final AcceptationId redEsAcc = addSimpleAcceptation(manager, esAlphabet, redConcept, "rojo");
        final AcceptationId redEnAcc = addSimpleAcceptation(manager, enAlphabet, redConcept, "red");

        final String text1a = "El coche es genial";
        final String text1b = "El coche es rojo";
        final String text2 = "The car is red";

        final ImmutableSet<SentenceSpan<AcceptationId>> spans1a = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text1a, "coche", carAcc))
                .add(newSpan(text1a, "genial", greatAcc))
                .build();

        final ImmutableSet<SentenceSpan<AcceptationId>> spans1b = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text1b, "coche", carAcc))
                .add(newSpan(text1b, "rojo", redEsAcc))
                .build();

        final ImmutableSet<SentenceSpan<AcceptationId>> spans2 = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text2, "red", redEnAcc))
                .build();

        final ConceptId concept1 = manager.getNextAvailableConceptId();
        final SentenceId sentence1 = manager.addSentence(concept1, text1a, spans1a);
        assertTrue(manager.updateSentenceTextAndSpans(sentence1, text1b, spans1b));

        final SentenceId sentence2 = manager.addSentence(concept1, text2, spans2);
        assertEmpty(manager.getSampleSentences(greatAcc));

        assertSinglePair(sentence1, text1b, manager.getSampleSentences(redEsAcc));
        assertSinglePair(sentence2, text2, manager.getSampleSentences(redEnAcc));
        assertSinglePair(sentence1, text1b, manager.getSampleSentences(carAcc));

        final SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> sentenceDetails1 = manager.getSentenceDetails(sentence1);
        final SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> sentenceDetails2 = manager.getSentenceDetails(sentence2);

        assertEquals(text1b, sentenceDetails1.text);
        assertEquals(spans1b, sentenceDetails1.spans);
        assertSinglePair(sentence2, text2, sentenceDetails1.sameMeaningSentences);

        assertEquals(text2, sentenceDetails2.text);
        assertEquals(spans2, sentenceDetails2.spans);
        assertSinglePair(sentence1, text1b, sentenceDetails2.sameMeaningSentences);
    }

    @Test
    default void testReplaceSentenceWithSameText() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId carAcc = obtainNewAcceptation(manager, esAlphabet, "coche");

        final String text = "Mi coche es genial";

        final ImmutableSet<SentenceSpan<AcceptationId>> spans1 = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "coche", carAcc))
                .build();

        final ConceptId concept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(concept, text, spans1);

        final AcceptationId greatAcc = obtainNewAcceptation(manager, esAlphabet, "genial");

        final ImmutableSet<SentenceSpan<AcceptationId>> spans2 = spans1.add(newSpan(text, "genial", greatAcc));

        assertTrue(manager.updateSentenceTextAndSpans(sentence, text, spans2));
        assertSinglePair(sentence, text, manager.getSampleSentences(carAcc));
        assertSinglePair(sentence, text, manager.getSampleSentences(greatAcc));

        final SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> sentenceDetails1 = manager.getSentenceDetails(sentence);

        assertEquals(concept, sentenceDetails1.concept);
        assertEquals(text, sentenceDetails1.text);
        assertTrue(spans2.equalSet(sentenceDetails1.spans));
    }

    @Test
    default void testRemoveSentence() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId carAcc = obtainNewAcceptation(manager, esAlphabet, "coche");
        final AcceptationId greatAcc = obtainNewAcceptation(manager, esAlphabet, "genial");

        final String text = "Mi coche es genial";

        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "coche", carAcc))
                .add(newSpan(text, "genial", greatAcc))
                .build();

        final ConceptId concept1 = manager.getNextAvailableConceptId();
        manager.removeSentence(manager.addSentence(concept1, text, spans));

        assertEmpty(manager.getSampleSentences(carAcc));
        assertEmpty(manager.getSampleSentences(greatAcc));
    }

    @Test
    default void testRemoveAcceptationIncludedInASpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final AcceptationId carAcc = obtainNewAcceptation(manager, esAlphabet, "coche");
        final AcceptationId redAcc = obtainNewAcceptation(manager, esAlphabet, "rojo");

        final String text = "El coche es rojo";

        final ImmutableSet<SentenceSpan<AcceptationId>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(newSpan(text, "coche", carAcc))
                .add(newSpan(text, "rojo", redAcc))
                .build();

        final ConceptId concept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(concept, text, spans);
        assertTrue(manager.removeAcceptation(carAcc));

        assertEmpty(manager.getSampleSentences(carAcc));
        assertSinglePair(sentence, text, manager.getSampleSentences(redAcc));
        assertEquals(redAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }
}
