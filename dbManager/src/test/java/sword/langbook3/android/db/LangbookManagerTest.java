package sword.langbook3.android.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.IntKeyMapTestUtils.assertSinglePair;
import static sword.langbook3.android.db.IntSetTestUtils.intSetOf;
import static sword.langbook3.android.db.LangbookReadableDatabase.findRuledAcceptationByRuleAndBaseAcceptation;
import static sword.langbook3.android.db.SentencesManagerTestUtils.newSpan;
import static sword.langbook3.android.db.SizableTestUtils.assertEmpty;
import static sword.langbook3.android.db.TraversableTestUtils.getSingleValue;

abstract class LangbookManagerTest implements QuizzesManagerTest, DefinitionsManagerTest, SentencesManagerTest {

    @Override
    public abstract LangbookManager createManager(MemoryDatabase db);

    private static class State {
        MemoryDatabase db;
        LangbookManager manager;
        String text;
        int substantiveConcept;
        int carAcc;
        int sentence;
        int mineAcc;
        int agentId;
        int pluralRule;
    }

    interface ThereIsNoSampleSentencesForTheRemovedAcceptationAssertion {
        State getState();

        @Test
        default void thenThereIsNoSampleSentencesForTheRemovedAcceptation() {
            final State s = getState();
            assertEmpty(s.manager.getSampleSentences(s.carAcc));
        }
    }

    interface ThereIsAUniqueSampleSentenceForTheOtherAcceptationAssertion {
        State getState();

        @Test
        default void thenThereIsAUniqueSampleSentenceForTheOtherAcceptation() {
            final State s = getState();
            assertSinglePair(s.sentence, s.text, s.manager.getSampleSentences(s.mineAcc));
        }
    }

    interface ThereIsAUniqueSpanInTheSentenceAssertion {
        State getState();

        @Test
        default void thenThereIsAUniqueSpanInTheSentence() {
            final State s = getState();
            assertEquals(s.mineAcc, getSingleValue(s.manager.getSentenceSpans(s.sentence)).acceptation);
        }
    }

    @Nested
    class GivenAnAgentThatCreatesDynamicAcceptationsFromABunch extends State {
        @BeforeEach
        void setUp() {
            db = new MemoryDatabase();
            manager = createManager(db);

            final int esAlphabet = manager.addLanguage("es").mainAlphabet;
            final int carConcept = manager.getMaxConcept() + 1;
            carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

            substantiveConcept = manager.getMaxConcept() + 1;
            addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

            final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
            final ImmutableIntKeyMap<String> adder = emptyCorrelation.put(esAlphabet, "s");

            pluralRule = manager.getMaxConcept() + 1;
            agentId = manager.addAgent(0, intSetOf(substantiveConcept), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule);

            assertTrue(manager.addAcceptationInBunch(substantiveConcept, carAcc));
        }

        @Nested
        class WhenAddingASentence {
            @BeforeEach
            void performAction() {
                final int carPluralAcc = findRuledAcceptationByRuleAndBaseAcceptation(db, pluralRule, carAcc);

                text = "Los coches son muy rápidos";
                final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                        .add(newSpan(text, "coches", carPluralAcc))
                        .build();

                final int concept = manager.getMaxConcept() + 1;
                sentence = manager.addSentence(concept, text, spans);
            }

            @Test
            void thenThereIsAUniqueSampleSentenceForTheAcceptation() {
                assertSinglePair(sentence, text, manager.getSampleSentences(carAcc));
            }

            @Test
            void thenThereIsAUniqueSpanInTheSentence() {
                final int carPluralAcc = findRuledAcceptationByRuleAndBaseAcceptation(db, pluralRule, carAcc);
                assertEquals(carPluralAcc, getSingleValue(manager.getSentenceSpans(sentence)).acceptation);
            }
        }
    }

    @Nested
    class GivenASentenceThatUsesAsASpanWithDynamicAcceptation extends State {
        @BeforeEach
        void setUp() {
            final MemoryDatabase db = new MemoryDatabase();
            manager = createManager(db);

            final int esAlphabet = manager.addLanguage("es").mainAlphabet;

            final int carConcept = manager.getMaxConcept() + 1;
            carAcc = addSimpleAcceptation(manager, esAlphabet, carConcept, "coche");

            final int mineConcept = manager.getMaxConcept() + 1;
            mineAcc = addSimpleAcceptation(manager, esAlphabet, mineConcept, "mío");

            substantiveConcept = manager.getMaxConcept() + 1;
            addSimpleAcceptation(manager, esAlphabet, substantiveConcept, "sustantivo");

            final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
            final ImmutableIntKeyMap<String> adder = emptyCorrelation.put(esAlphabet, "s");

            final int pluralRule = manager.getMaxConcept() + 1;
            agentId = manager.addAgent(0, intSetOf(substantiveConcept), intSetOf(), emptyCorrelation, emptyCorrelation, emptyCorrelation, adder, pluralRule);

            assertTrue(manager.addAcceptationInBunch(substantiveConcept, carAcc));
            final int carPluralAcc = findRuledAcceptationByRuleAndBaseAcceptation(db, pluralRule, carAcc);

            text = "El mejor de los coches es el mío";
            final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                    .add(newSpan(text, "coches", carPluralAcc))
                    .add(newSpan(text, "mío", mineAcc))
                    .build();

            final int concept = manager.getMaxConcept() + 1;
            sentence = manager.addSentence(concept, text, spans);
        }

        @Nested
        class WhenRemovingAcceptationFromAgentSourceBunch implements
                ThereIsAUniqueSampleSentenceForTheOtherAcceptationAssertion,
                ThereIsAUniqueSpanInTheSentenceAssertion,
                ThereIsNoSampleSentencesForTheRemovedAcceptationAssertion {

            @BeforeEach
            void performAction() {
                assertTrue(manager.removeAcceptationFromBunch(substantiveConcept, carAcc));
            }

            @Override
            public State getState() {
                return GivenASentenceThatUsesAsASpanWithDynamicAcceptation.this;
            }
        }

        @Nested
        class WhenRemovingAgent implements
                ThereIsAUniqueSampleSentenceForTheOtherAcceptationAssertion,
                ThereIsAUniqueSpanInTheSentenceAssertion,
                ThereIsNoSampleSentencesForTheRemovedAcceptationAssertion {
            @BeforeEach
            void performAction() {
                manager.removeAgent(agentId);
            }

            @Override
            public State getState() {
                return GivenASentenceThatUsesAsASpanWithDynamicAcceptation.this;
            }
        }
    }
}
