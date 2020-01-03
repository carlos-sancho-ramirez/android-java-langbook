package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableSet;
import sword.database.Database;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.findRuledAcceptationByRuleAndBaseAcceptation;

final class LangbookManagerTest {

    private LangbookManager createManager(Database db) {
        return new LangbookDatabaseManager(db);
    }

    @Test
    void testAddDynamicAcceptationInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final String carText = "coche";
        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, carText);

        final String substantiveText = "sustantivo";
        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, substantiveText);

        final ImmutableIntSet sourceBunches = new ImmutableIntSetCreator().add(substantiveConcept).build();
        final ImmutableIntSet diffBunches = ImmutableIntArraySet.empty();
        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();

        final String pluralSuffix = "s";
        final ImmutableIntKeyMap<String> adder = emptyCorrelation.put(esAlphabet, pluralSuffix);

        final int pluralRule = manager.getMaxConcept() + 1;
        assertNotNull(manager.addAgent(0, sourceBunches, diffBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule));

        assertTrue(manager.addAcceptationInBunch(substantiveConcept, carAcc));
        final int carPluralAcc = findRuledAcceptationByRuleAndBaseAcceptation(db, pluralRule, carAcc);

        final String carPluralText = carText + pluralSuffix;
        final String text = "Los " + carPluralText + " son muy rápidos";

        final int carPluralStart = text.indexOf(carPluralText);
        final int carPluralEnd = carPluralStart + carPluralText.length();

        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carPluralStart, carPluralEnd - 1), carPluralAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);

        final ImmutableIntKeyMap<String> matchingSentences = manager.getSampleSentences(carAcc);
        assertEquals(1, matchingSentences.size());
        assertEquals(text, matchingSentences.get(sentence));

        final ImmutableSet<SentenceSpan> foundSpans = manager.getSentenceSpans(sentence);
        assertEquals(1, foundSpans.size());
        assertEquals(carPluralAcc, foundSpans.valueAt(0).acceptation);
    }

    @Test
    void testRemoveDynamicAcceptationFromBunchUsedAsSourceForAgentWhoseOutputIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final String carText = "coche";
        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, carText);

        final String mineText = "mío";
        final int mineConcept = manager.getMaxConcept() + 1;
        final int mineAcc = addSimpleAcceptation(manager, esAlphabet, mineConcept, mineText);

        final String substantiveText = "sustantivo";
        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, substantiveText);

        final ImmutableIntSet sourceBunches = new ImmutableIntSetCreator().add(substantiveConcept).build();
        final ImmutableIntSet diffBunches = ImmutableIntArraySet.empty();
        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();

        final String pluralSuffix = "s";
        final ImmutableIntKeyMap<String> adder = emptyCorrelation.put(esAlphabet, pluralSuffix);

        final int pluralRule = manager.getMaxConcept() + 1;
        assertNotNull(manager.addAgent(0, sourceBunches, diffBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule));

        assertTrue(manager.addAcceptationInBunch(substantiveConcept, carAcc));
        final int carPluralAcc = findRuledAcceptationByRuleAndBaseAcceptation(db, pluralRule, carAcc);

        final String carPluralText = carText + pluralSuffix;
        final String text = "El mejor de los " + carPluralText + " es el " + mineText;

        final int carPluralStart = text.indexOf(carPluralText);
        final int carPluralEnd = carPluralStart + carPluralText.length();
        final int mineStart = text.indexOf(mineText);
        final int mineEnd = mineStart + mineText.length();

        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carPluralStart, carPluralEnd - 1), carPluralAcc))
                .add(new SentenceSpan(new ImmutableIntRange(mineStart, mineEnd - 1), mineAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);
        assertTrue(manager.removeAcceptationFromBunch(substantiveConcept, carAcc));
        assertTrue(manager.getSampleSentences(carAcc).isEmpty());

        final ImmutableIntKeyMap<String> matchingSentences = manager.getSampleSentences(mineAcc);
        assertEquals(1, matchingSentences.size());
        assertEquals(text, matchingSentences.get(sentence));

        final ImmutableSet<SentenceSpan> foundSpans = manager.getSentenceSpans(sentence);
        assertEquals(1, foundSpans.size());
        assertEquals(mineAcc, foundSpans.valueAt(0).acceptation);
    }

    @Test
    void testRemoveAgentWhoseOutputIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookManager manager = createManager(db);

        final int esAlphabet = manager.addLanguage("es").mainAlphabet;

        final String carText = "coche";
        final int carConcept = manager.getMaxConcept() + 1;
        final int carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, carText);

        final String mineText = "mío";
        final int mineConcept = manager.getMaxConcept() + 1;
        final int mineAcc = addSimpleAcceptation(manager, esAlphabet, mineConcept, mineText);

        final String substantiveText = "sustantivo";
        final int substantiveConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, substantiveConcept, substantiveText);

        final ImmutableIntSet sourceBunches = new ImmutableIntSetCreator().add(substantiveConcept).build();
        final ImmutableIntSet diffBunches = ImmutableIntArraySet.empty();
        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();

        final String pluralSuffix = "s";
        final ImmutableIntKeyMap<String> adder = emptyCorrelation.put(esAlphabet, pluralSuffix);

        final int pluralRule = manager.getMaxConcept() + 1;
        final int agentId = manager.addAgent(0, sourceBunches, diffBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule);

        assertTrue(manager.addAcceptationInBunch(substantiveConcept, carAcc));
        final int carPluralAcc = findRuledAcceptationByRuleAndBaseAcceptation(db, pluralRule, carAcc);

        final String carPluralText = carText + pluralSuffix;
        final String text = "El mejor de los " + carPluralText + " es el " + mineText;

        final int carPluralStart = text.indexOf(carPluralText);
        final int carPluralEnd = carPluralStart + carPluralText.length();
        final int mineStart = text.indexOf(mineText);
        final int mineEnd = mineStart + mineText.length();

        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(new SentenceSpan(new ImmutableIntRange(carPluralStart, carPluralEnd - 1), carPluralAcc))
                .add(new SentenceSpan(new ImmutableIntRange(mineStart, mineEnd - 1), mineAcc))
                .build();

        final int concept = manager.getMaxConcept() + 1;
        final int sentence = manager.addSentence(concept, text, spans);
        manager.removeAgent(agentId);
        assertTrue(manager.getSampleSentences(carAcc).isEmpty());

        final ImmutableIntKeyMap<String> matchingSentences = manager.getSampleSentences(mineAcc);
        assertEquals(1, matchingSentences.size());
        assertEquals(text, matchingSentences.get(sentence));

        final ImmutableSet<SentenceSpan> foundSpans = manager.getSentenceSpans(sentence);
        assertEquals(1, foundSpans.size());
        assertEquals(mineAcc, foundSpans.valueAt(0).acceptation);
    }
}
