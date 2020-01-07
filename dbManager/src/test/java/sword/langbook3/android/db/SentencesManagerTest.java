package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.IntKeyMapTestUtils.assertEqualMap;
import static sword.langbook3.android.db.IntKeyMapTestUtils.assertSinglePair;
import static sword.langbook3.android.db.SizableTestUtils.assertEmpty;
import static sword.langbook3.android.db.TraversableTestUtils.getSingleValue;

interface SentencesManagerTest extends AcceptationsManagerTest {

    @Override
    SentencesManager createManager(MemoryDatabase db);

    @Test
    default void testAddSentences() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;
        final int enAlphabet = manager.addLanguage("en").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int greatConcept = manager.getMaxConcept() + 1;
        final int greatAcc = addSimpleAcceptation(manager, esAlphabet, greatConcept, "genial");

        final int redConcept = manager.getMaxConcept() + 1;
        final int redEsAcc = addSimpleAcceptation(manager, esAlphabet, redConcept, "rojo");
        final int redEnAcc = addSimpleAcceptation(manager, enAlphabet, redConcept, "red");

        final String text1 = "El coche es genial";
        final String text2 = "El coche es rojo";
        final String text3 = "The car is red";

        final int carStart = text1.indexOf("coche");
        final int carEnd = carStart + "coche".length();
        final int greatStart = text1.indexOf("genial");
        final int greatEnd = greatStart + "genial".length();
        final int redEsStart = text2.indexOf("rojo");
        final int redEsEnd = redEsStart + "rojo".length();
        final int redEnStart = text3.indexOf("red");
        final int redEnEnd = redEnStart + "red".length();

        final ImmutableSet<SentenceSpan> spans1 = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .add(new SentenceSpan(new ImmutableIntRange(greatStart, greatEnd - 1), greatAcc))
                .build();

        final ImmutableSet<SentenceSpan> spans2 = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .add(new SentenceSpan(new ImmutableIntRange(redEsStart, redEsEnd - 1), redEsAcc))
                .build();

        final ImmutableSet<SentenceSpan> spans3 = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(redEnStart, redEnEnd - 1), redEnAcc))
                .build();

        final int concept1 = manager.getMaxConcept() + 1;
        final int sentence1 = manager.addSentence(concept1, text1, spans1);

        final int concept2 = manager.getMaxConcept() + 1;
        final int sentence2 = manager.addSentence(concept2, text2, spans2);
        final int sentence3 = manager.addSentence(concept2, text3, spans3);

        assertSinglePair(sentence1, text1, manager.getSampleSentences(greatAcc));
        assertSinglePair(sentence2, text2, manager.getSampleSentences(redEsAcc));
        assertSinglePair(sentence3, text3, manager.getSampleSentences(redEnAcc));

        final IntKeyMap<String> expectedCarMatchingSentences = new ImmutableIntKeyMap.Builder<String>()
                .put(sentence1, text1)
                .put(sentence2, text2)
                .build();
        assertEqualMap(expectedCarMatchingSentences, manager.getSampleSentences(carAcc));

        final SentenceDetailsModel sentenceDetails1 = manager.getSentenceDetails(sentence1);
        final SentenceDetailsModel sentenceDetails2 = manager.getSentenceDetails(sentence2);
        final SentenceDetailsModel sentenceDetails3 = manager.getSentenceDetails(sentence3);

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
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;
        final int enAlphabet = manager.addLanguage("en").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int greatConcept = manager.getMaxConcept() + 1;
        final int greatAcc = addSimpleAcceptation(manager, esAlphabet, greatConcept, "genial");

        final int redConcept = manager.getMaxConcept() + 1;
        final int redEsAcc = addSimpleAcceptation(manager, esAlphabet, redConcept, "rojo");

        final int redEnAcc = addSimpleAcceptation(manager, enAlphabet, redConcept, "red");

        final String text1a = "El coche es genial";
        final String text1b = "El coche es rojo";
        final String text2 = "The car is red";

        final int carStart = text1a.indexOf("coche");
        final int carEnd = carStart + "coche".length();
        final int greatStart = text1a.indexOf("genial");
        final int greatEnd = greatStart + "genial".length();
        final int redEsStart = text1b.indexOf("rojo");
        final int redEsEnd = redEsStart + "rojo".length();
        final int redEnStart = text2.indexOf("red");
        final int redEnEnd = redEnStart + "red".length();

        final ImmutableSet<SentenceSpan> spans1a = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .add(new SentenceSpan(new ImmutableIntRange(greatStart, greatEnd - 1), greatAcc))
                .build();

        final ImmutableSet<SentenceSpan> spans1b = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .add(new SentenceSpan(new ImmutableIntRange(redEsStart, redEsEnd - 1), redEsAcc))
                .build();

        final ImmutableSet<SentenceSpan> spans2 = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(redEnStart, redEnEnd - 1), redEnAcc))
                .build();

        final int concept1 = manager.getMaxConcept() + 1;
        final int sentence1 = manager.addSentence(concept1, text1a, spans1a);
        assertTrue(manager.updateSentenceTextAndSpans(sentence1, text1b, spans1b));

        final int sentence2 = manager.addSentence(concept1, text2, spans2);
        assertEmpty(manager.getSampleSentences(greatAcc));

        assertSinglePair(sentence1, text1b, manager.getSampleSentences(redEsAcc));
        assertSinglePair(sentence2, text2, manager.getSampleSentences(redEnAcc));
        assertSinglePair(sentence1, text1b, manager.getSampleSentences(carAcc));

        final SentenceDetailsModel sentenceDetails1 = manager.getSentenceDetails(sentence1);
        final SentenceDetailsModel sentenceDetails2 = manager.getSentenceDetails(sentence2);

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
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final String text = "Mi coche es genial";

        final int carStart = text.indexOf("coche");
        final int carEnd = carStart + "coche".length();
        final int greatStart = text.indexOf("genial");
        final int greatEnd = greatStart + "genial".length();

        final ImmutableSet<SentenceSpan> spans1 = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans1);

        final int greatConcept = manager.getMaxConcept() + 1;
        final int greatAcc = addSimpleAcceptation(manager, esAlphabet, greatConcept, "genial");

        final ImmutableSet<SentenceSpan> spans2 = spans1
                .add(new SentenceSpan(new ImmutableIntRange(greatStart, greatEnd - 1), greatAcc));

        assertTrue(manager.updateSentenceTextAndSpans(sentence, text, spans2));
        assertSinglePair(sentence, text, manager.getSampleSentences(carAcc));
        assertSinglePair(sentence, text, manager.getSampleSentences(greatAcc));

        final SentenceDetailsModel sentenceDetails1 = manager.getSentenceDetails(sentence);

        assertEquals(concept, sentenceDetails1.concept);
        assertEquals(text, sentenceDetails1.text);
        assertTrue(spans2.equalSet(sentenceDetails1.spans));
    }

    @Test
    default void testRemoveSentence() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int greatConcept = manager.getMaxConcept() + 1;
        final int greatAcc = addSimpleAcceptation(manager, esAlphabet, greatConcept, "genial");

        final String text = "Mi coche es genial";

        final int carStart = text.indexOf("coche");
        final int carEnd = carStart + "coche".length();
        final int greatStart = text.indexOf("genial");
        final int greatEnd = greatStart + "genial".length();

        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .add(new SentenceSpan(new ImmutableIntRange(greatStart, greatEnd - 1), greatAcc))
                .build();

        final int concept1 = manager.getMaxConcept() + 1;
        final int sentence1 = manager.addSentence(concept1, text, spans);
        manager.removeSentence(sentence1);

        assertEmpty(manager.getSampleSentences(carAcc));
        assertEmpty(manager.getSampleSentences(greatAcc));
    }

    @Test
    default void testRemoveAcceptationIncludedInASpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

        final int redConcept = manager.getMaxConcept() + 1;
        final int redAcc = addSimpleAcceptation(manager, esAlphabet, redConcept, "rojo");

        final String text = "El coche es rojo";

        final int carStart = text.indexOf("coche");
        final int carEnd = carStart + "coche".length();
        final int redStart = text.indexOf("rojo");
        final int redEnd = redStart + "rojo".length();

        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .add(new SentenceSpan(new ImmutableIntRange(redStart, redEnd - 1), redAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);
        assertTrue(manager.removeAcceptation(carAcc));

        assertEmpty(manager.getSampleSentences(carAcc));
        assertSinglePair(sentence, text, manager.getSampleSentences(redAcc));
        assertEquals(redAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
    }
}
