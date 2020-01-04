package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableSet;
import sword.database.Database;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;

final class SentencesManagerTest {

    private SentencesManager createManager(Database db) {
        return new LangbookDatabaseManager(db);
    }

    @Test
    void testAddSentences() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;
        final int enAlphabet = manager.addLanguage("en").mainAlphabet;

        final String carText = "coche";
        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, carText);

        final String greatText = "genial";
        final int greatConcept = manager.getMaxConcept() + 1;
        final int greatAcc = addSimpleAcceptation(manager, esAlphabet, greatConcept, greatText);

        final String redEsText = "rojo";
        final int redConcept = manager.getMaxConcept() + 1;
        final int redEsAcc = addSimpleAcceptation(manager, esAlphabet, redConcept, redEsText);

        final String redEnText = "red";
        final int redEnAcc = addSimpleAcceptation(manager, enAlphabet, redConcept, redEnText);

        final String text1 = "El " + carText + " es " + greatText;
        final String text2 = "El " + carText + " es " + redEsText;
        final String text3 = "The car is " + redEnText;

        final int carStart = text1.indexOf(carText);
        final int carEnd = carStart + carText.length();
        final int greatStart = text1.indexOf(greatText);
        final int greatEnd = greatStart + greatText.length();
        final int redEsStart = text2.indexOf(redEsText);
        final int redEsEnd = redEsStart + redEsText.length();
        final int redEnStart = text3.indexOf(redEnText);
        final int redEnEnd = redEnStart + redEnText.length();

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

        final ImmutableIntKeyMap<String> greatMatchingSentences = manager.getSampleSentences(greatAcc);
        assertEquals(1, greatMatchingSentences.size());
        assertEquals(text1, greatMatchingSentences.get(sentence1));

        final ImmutableIntKeyMap<String> redEsMatchingSentences = manager.getSampleSentences(redEsAcc);
        assertEquals(1, redEsMatchingSentences.size());
        assertEquals(text2, redEsMatchingSentences.get(sentence2));

        final ImmutableIntKeyMap<String> redEnMatchingSentences = manager.getSampleSentences(redEnAcc);
        assertEquals(1, redEnMatchingSentences.size());
        assertEquals(text3, redEnMatchingSentences.get(sentence3));

        final ImmutableIntKeyMap<String> carMatchingSentences = manager.getSampleSentences(carAcc);
        assertEquals(2, carMatchingSentences.size());
        assertEquals(text1, carMatchingSentences.get(sentence1));
        assertEquals(text2, carMatchingSentences.get(sentence2));

        final SentenceDetailsModel sentenceDetails1 = manager.getSentenceDetails(sentence1);
        final SentenceDetailsModel sentenceDetails2 = manager.getSentenceDetails(sentence2);
        final SentenceDetailsModel sentenceDetails3 = manager.getSentenceDetails(sentence3);

        assertEquals(text1, sentenceDetails1.text);
        assertEquals(spans1, sentenceDetails1.spans);
        assertTrue(sentenceDetails1.sameMeaningSentences.isEmpty());

        assertEquals(text2, sentenceDetails2.text);
        assertEquals(spans2, sentenceDetails2.spans);
        assertEquals(1, sentenceDetails2.sameMeaningSentences.size());
        assertEquals(sentence3, sentenceDetails2.sameMeaningSentences.keyAt(0));
        assertEquals(text3, sentenceDetails2.sameMeaningSentences.valueAt(0));

        assertEquals(text3, sentenceDetails3.text);
        assertEquals(spans3, sentenceDetails3.spans);
        assertEquals(1, sentenceDetails3.sameMeaningSentences.size());
        assertEquals(sentence2, sentenceDetails3.sameMeaningSentences.keyAt(0));
        assertEquals(text2, sentenceDetails3.sameMeaningSentences.valueAt(0));
    }

    @Test
    void testReplaceSentence() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;
        final int enAlphabet = manager.addLanguage("en").mainAlphabet;

        final String carText = "coche";
        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, carText);

        final String greatText = "genial";
        final int greatConcept = manager.getMaxConcept() + 1;
        final int greatAcc = addSimpleAcceptation(manager, esAlphabet, greatConcept, greatText);

        final String redEsText = "rojo";
        final int redConcept = manager.getMaxConcept() + 1;
        final int redEsAcc = addSimpleAcceptation(manager, esAlphabet, redConcept, redEsText);

        final String redEnText = "red";
        final int redEnAcc = addSimpleAcceptation(manager, enAlphabet, redConcept, redEnText);

        final String text1a = "El " + carText + " es " + greatText;
        final String text1b = "El " + carText + " es " + redEsText;
        final String text2 = "The car is " + redEnText;

        final int carStart = text1a.indexOf(carText);
        final int carEnd = carStart + carText.length();
        final int greatStart = text1a.indexOf(greatText);
        final int greatEnd = greatStart + greatText.length();
        final int redEsStart = text1b.indexOf(redEsText);
        final int redEsEnd = redEsStart + redEsText.length();
        final int redEnStart = text2.indexOf(redEnText);
        final int redEnEnd = redEnStart + redEnText.length();

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
        assertTrue(manager.getSampleSentences(greatAcc).isEmpty());

        final ImmutableIntKeyMap<String> redEsMatchingSentences = manager.getSampleSentences(redEsAcc);
        assertEquals(1, redEsMatchingSentences.size());
        assertEquals(text1b, redEsMatchingSentences.get(sentence1));

        final ImmutableIntKeyMap<String> redEnMatchingSentences = manager.getSampleSentences(redEnAcc);
        assertEquals(1, redEnMatchingSentences.size());
        assertEquals(text2, redEnMatchingSentences.get(sentence2));

        final ImmutableIntKeyMap<String> carMatchingSentences = manager.getSampleSentences(carAcc);
        assertEquals(1, carMatchingSentences.size());
        assertEquals(text1b, carMatchingSentences.get(sentence1));

        final SentenceDetailsModel sentenceDetails1 = manager.getSentenceDetails(sentence1);
        final SentenceDetailsModel sentenceDetails2 = manager.getSentenceDetails(sentence2);

        assertEquals(text1b, sentenceDetails1.text);
        assertEquals(spans1b, sentenceDetails1.spans);
        assertEquals(1, sentenceDetails1.sameMeaningSentences.size());
        assertEquals(sentence2, sentenceDetails1.sameMeaningSentences.keyAt(0));
        assertEquals(text2, sentenceDetails1.sameMeaningSentences.valueAt(0));

        assertEquals(text2, sentenceDetails2.text);
        assertEquals(spans2, sentenceDetails2.spans);
        assertEquals(1, sentenceDetails2.sameMeaningSentences.size());
        assertEquals(sentence1, sentenceDetails2.sameMeaningSentences.keyAt(0));
        assertEquals(text1b, sentenceDetails2.sameMeaningSentences.valueAt(0));
    }

    @Test
    void testReplaceSentenceWithSameText() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final String carText = "coche";
        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, carText);

        final String greatText = "genial";
        final String text = "Mi " + carText + " es " + greatText;

        final int carStart = text.indexOf(carText);
        final int carEnd = carStart + carText.length();
        final int greatStart = text.indexOf(greatText);
        final int greatEnd = greatStart + greatText.length();

        final ImmutableSet<SentenceSpan> spans1 = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans1);

        final int greatConcept = manager.getMaxConcept() + 1;
        final int greatAcc = addSimpleAcceptation(manager, esAlphabet, greatConcept, greatText);

        final ImmutableSet<SentenceSpan> spans2 = spans1
                .add(new SentenceSpan(new ImmutableIntRange(greatStart, greatEnd - 1), greatAcc));

        assertTrue(manager.updateSentenceTextAndSpans(sentence, text, spans2));

        final ImmutableIntKeyMap<String> carMatchingSentences = manager.getSampleSentences(carAcc);
        assertEquals(1, carMatchingSentences.size());
        assertEquals(text, carMatchingSentences.get(sentence));

        final ImmutableIntKeyMap<String> greatMatchingSentences = manager.getSampleSentences(greatAcc);
        assertEquals(1, greatMatchingSentences.size());
        assertEquals(text, greatMatchingSentences.get(sentence));

        final SentenceDetailsModel sentenceDetails1 = manager.getSentenceDetails(sentence);

        assertEquals(concept, sentenceDetails1.concept);
        assertEquals(text, sentenceDetails1.text);
        assertTrue(spans2.equalSet(sentenceDetails1.spans));
    }

    @Test
    void testRemoveSentence() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final String carText = "coche";
        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, carText);

        final String greatText = "genial";
        final int greatConcept = manager.getMaxConcept() + 1;
        final int greatAcc = addSimpleAcceptation(manager, esAlphabet, greatConcept, greatText);

        final String text = "Mi " + carText + " es " + greatText;

        final int carStart = text.indexOf(carText);
        final int carEnd = carStart + carText.length();
        final int greatStart = text.indexOf(greatText);
        final int greatEnd = greatStart + greatText.length();

        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .add(new SentenceSpan(new ImmutableIntRange(greatStart, greatEnd - 1), greatAcc))
                .build();

        final int concept1 = manager.getMaxConcept() + 1;
        final int sentence1 = manager.addSentence(concept1, text, spans);
        manager.removeSentence(sentence1);

        assertTrue(manager.getSampleSentences(carAcc).isEmpty());
        assertTrue(manager.getSampleSentences(greatAcc).isEmpty());
    }

    @Test
    void testRemoveAcceptationIncludedInASpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final SentencesManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final String carText = "coche";
        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, carText);

        final String redText = "rojo";
        final int redConcept = manager.getMaxConcept() + 1;
        final int redAcc = addSimpleAcceptation(manager, esAlphabet, redConcept, redText);

        final String text = "El " + carText + " es " + redText;

        final int carStart = text.indexOf(carText);
        final int carEnd = carStart + carText.length();
        final int redStart = text.indexOf(redText);
        final int redEnd = redStart + redText.length();

        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carStart, carEnd - 1), carAcc))
                .add(new SentenceSpan(new ImmutableIntRange(redStart, redEnd - 1), redAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);
        assertTrue(manager.removeAcceptation(carAcc));

        assertTrue(manager.getSampleSentences(carAcc).isEmpty());

        final ImmutableIntKeyMap<String> redEsMatchingSentences = manager.getSampleSentences(redAcc);
        assertEquals(1, redEsMatchingSentences.size());
        assertEquals(text, redEsMatchingSentences.get(sentence));

        final ImmutableSet<SentenceSpan> foundSpans = manager.getSentenceSpans(sentence);
        assertEquals(1, foundSpans.size());
        assertEquals(redAcc, foundSpans.valueAt(0).acceptation);
    }
}
