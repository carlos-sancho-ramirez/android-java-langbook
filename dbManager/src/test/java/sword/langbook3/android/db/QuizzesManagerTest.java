package sword.langbook3.android.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.QuestionFieldDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.updateAcceptationSimpleCorrelationArray;
import static sword.langbook3.android.db.IntPairMapTestUtils.assertSinglePair;
import static sword.langbook3.android.db.IntSetTestUtils.intSetOf;
import static sword.langbook3.android.db.IntTraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;
import static sword.langbook3.android.db.SizableTestUtils.assertEmpty;

/**
 * Include all test related to all responsibilities of a QuizzesManager.
 *
 * QuizzesManager responsibilities include all responsibilities from AgentsManager, and include the following ones:
 * <li>Quizzes</li>
 * <li>Knowledge</li>
 */
abstract class QuizzesManagerTest implements AgentsManagerTest {

    @Override
    public abstract QuizzesManager createManager(MemoryDatabase db);

    static int addJapaneseSingAcceptation(AcceptationsManager manager, int kanjiAlphabet, int kanaAlphabet, int concept) {
        final ImmutableIntKeyMap<String> correlation1 = new ImmutableIntKeyMap.Builder<String>()
                .put(kanjiAlphabet, "歌")
                .put(kanaAlphabet, "うた")
                .build();

        final ImmutableIntKeyMap<String> correlation2 = new ImmutableIntKeyMap.Builder<String>()
                .put(kanjiAlphabet, "う")
                .put(kanaAlphabet, "う")
                .build();

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .append(correlation1)
                .append(correlation2)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    private class State {
        MemoryDatabase db;
        QuizzesManager manager;
        int esAlphabet;
        int kanjiAlphabet;
        int myVocabularyConcept;
        int esAcceptation;
        int quizId;

        void setSingAcceptationForBothSpanishAndJapanese() {
            db = new MemoryDatabase();
            manager = createManager(db);
            esAlphabet = manager.addLanguage("es").mainAlphabet;
            kanjiAlphabet = manager.addLanguage("ja").mainAlphabet;
            final int kanaAlphabet = kanjiAlphabet + 1;
            assertTrue(manager.addAlphabetCopyingFromOther(kanaAlphabet, kanjiAlphabet));

            myVocabularyConcept = manager.getMaxConcept() + 1;
            final int singConcept = myVocabularyConcept + 1;
            esAcceptation = addSimpleAcceptation(manager, esAlphabet, singConcept, "cantar");
            addJapaneseSingAcceptation(manager, kanjiAlphabet, kanaAlphabet, singConcept);
        }

        void addSingAcceptationInTheQuizBunch() {
            manager.addAcceptationInBunch(myVocabularyConcept, esAcceptation);
        }

        void addTheQuiz() {
            final ImmutableList<QuestionFieldDetails> fields = new ImmutableList.Builder<QuestionFieldDetails>()
                    .add(new QuestionFieldDetails(esAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                    .add(new QuestionFieldDetails(kanjiAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.IS_ANSWER | LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT))
                    .build();

            quizId = manager.obtainQuiz(myVocabularyConcept, fields);
        }
    }

    @Nested
    class GivenTheSingAcceptationForBothSpanishAndJapanese extends State {
        @BeforeEach
        void setUp() {
            setSingAcceptationForBothSpanishAndJapanese();
        }

        @Nested
        class WhenCreatingTheQuiz {
            @BeforeEach
            void performAction() {
                addTheQuiz();
            }

            @Test
            void thenNoKnowledgeShouldBePresent() {
                assertEmpty(manager.getCurrentKnowledge(quizId));
            }
        }
    }

    @Nested
    class GivenTheSingAcceptationIncludedInTheQuizBunch extends State {
        @BeforeEach
        void setUp() {
            setSingAcceptationForBothSpanishAndJapanese();
            addSingAcceptationInTheQuizBunch();
        }

        @Nested
        class WhenCreatingTheQuiz {
            @BeforeEach
            void performAction() {
                addTheQuiz();
            }

            @Test
            void thenKnowledgeShouldBePresent() {
                assertSinglePair(esAcceptation, NO_SCORE, manager.getCurrentKnowledge(quizId));
            }
        }
    }

    @Nested
    class GivenTheSingAcceptationAndTheQuiz extends State {
        @BeforeEach
        void setUp() {
            setSingAcceptationForBothSpanishAndJapanese();
            addTheQuiz();
        }

        @Nested
        class WhenTheSingAcceptationIsIncludedInTheQuizBunch {
            @BeforeEach
            void performAction() {
                addSingAcceptationInTheQuizBunch();
            }

            @Test
            void thenKnowledgeShouldBePresent() {
                assertSinglePair(esAcceptation, NO_SCORE, manager.getCurrentKnowledge(quizId));
            }
        }
    }

    @Nested
    class GivenTheSingAcceptationIncludedInTheQuizBunchAndTheQuiz extends State {
        @BeforeEach
        void setUp() {
            setSingAcceptationForBothSpanishAndJapanese();
            addTheQuiz();
            addSingAcceptationInTheQuizBunch();
        }

        @Nested
        class WhenRemovingTheQuiz {
            @BeforeEach
            void performAction() {
                assertTrue(manager.removeAcceptationFromBunch(myVocabularyConcept, esAcceptation));
            }

            @Test
            void thenNoKnowledgeShouldBePresent() {
                assertEmpty(manager.getCurrentKnowledge(quizId));
            }
        }
    }

    class State2 {
        QuizzesManager manager;
        MemoryDatabase db;

        int alphabet;
        int upperCaseAlphabet;
        int conjugationVerbBunch;
        int acceptation;
        int quizId;

        void setSingAcceptationAndUpperCaseConversion() {
            db = new MemoryDatabase();
            manager = createManager(db);
            alphabet = manager.addLanguage("es").mainAlphabet;

            final int concept = manager.getMaxConcept() + 1;
            conjugationVerbBunch = concept + 1;
            upperCaseAlphabet = conjugationVerbBunch + 1;
            final Conversion conversion = new Conversion(alphabet, upperCaseAlphabet, upperCaseConversion);
            assertTrue(manager.addAlphabetAsConversionTarget(conversion));

            acceptation = addSimpleAcceptation(manager, alphabet, concept, "cantar");
        }

        private void addAgentForConjugationVerbs(String matcherText) {
            final ImmutableIntSet noBunches = intSetOf();
            final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
            final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                    .put(alphabet, matcherText)
                    .build();

            manager.addAgent(conjugationVerbBunch, noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);
        }

        void addAgentForFirstConjugationVerbs() {
            addAgentForConjugationVerbs("ar");
        }

        void addAgentForSecondConjugationVerbs() {
            addAgentForConjugationVerbs("er");
        }

        void addTheQuiz() {
            final ImmutableList<QuestionFieldDetails> quizFields = new ImmutableList.Builder<QuestionFieldDetails>()
                    .add(new QuestionFieldDetails(alphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                    .add(new QuestionFieldDetails(upperCaseAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC | LangbookDbSchema.QuestionFieldFlags.IS_ANSWER))
                    .build();
            quizId = manager.obtainQuiz(conjugationVerbBunch, quizFields);
        }

        void updateCorrelationArray() {
            updateAcceptationSimpleCorrelationArray(manager, alphabet, acceptation, "beber");
        }
    }

    @Nested
    class GivenTheSingAcceptationAndTheQuizForSecondConjugationVerbs extends State2 {
        @BeforeEach
        void setUp() {
            setSingAcceptationAndUpperCaseConversion();
            addAgentForSecondConjugationVerbs();
            addTheQuiz();
        }

        @Nested
        class WhenUpdatingTheCorrelationArray {
            @BeforeEach
            void performAction() {
                updateCorrelationArray();
            }

            @Test
            void thenStaticAndDynamicAcceptationMatches() {
                assertEquals(acceptation, manager.getStaticAcceptationFromDynamic(acceptation).intValue());
            }

            @Test
            void thenTextIsUpdated() {
                assertEquals("beber", manager.getAcceptationTexts(acceptation).get(alphabet));
            }

            @Test
            void thenUpperCaseTextIsUpdated() {
                assertEquals("BEBER", manager.getAcceptationTexts(acceptation).get(upperCaseAlphabet));
            }

            @Test
            void thenSecondConjugationVerbBunchOnlyContainsTheAcceptation() {
                assertContainsOnly(acceptation, manager.getAcceptationsInBunch(conjugationVerbBunch));
            }

            @Test
            void thenOnlyKnowledgeForTheAcceptationIsPresent() {
                assertContainsOnly(acceptation, manager.getCurrentKnowledge(quizId).keySet());
            }
        }
    }

    @Nested
    class GivenTheSingAcceptationAndTheQuizForFirstConjugationVerbs extends State2 {
        @BeforeEach
        void setUp() {
            setSingAcceptationAndUpperCaseConversion();
            addAgentForFirstConjugationVerbs();
            addTheQuiz();
        }

        @Nested
        class WhenUpdatingTheCorrelationArray {
            @BeforeEach
            void performAction() {
                updateCorrelationArray();
            }

            @Test
            void thenStaticAndDynamicAcceptationMatches() {
                assertEquals(acceptation, manager.getStaticAcceptationFromDynamic(acceptation).intValue());
            }

            @Test
            void thenTextIsUpdated() {
                assertEquals("beber", manager.getAcceptationTexts(acceptation).get(alphabet));
            }

            @Test
            void thenUpperCaseTextIsUpdated() {
                assertEquals("BEBER", manager.getAcceptationTexts(acceptation).get(upperCaseAlphabet));
            }

            @Test
            void thenFirstConjugationVerbBunchIsEmpty() {
                assertEmpty(manager.getAcceptationsInBunch(conjugationVerbBunch));
            }

            @Test
            void thenNoKnowledgeIsPresent() {
                assertEmpty(manager.getCurrentKnowledge(quizId));
            }
        }
    }
}
