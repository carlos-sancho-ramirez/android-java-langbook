package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.QuestionFieldDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.IntPairMapTestUtils.assertSinglePair;
import static sword.collections.IntSetTestUtils.assertEqualSet;
import static sword.collections.IntSetTestUtils.intSetOf;
import static sword.collections.IntTraversableTestUtils.assertContainsOnly;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.updateAcceptationSimpleCorrelationArray;
import static sword.langbook3.android.db.BunchesManagerTest.addSpanishSingAcceptation;
import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;

/**
 * Include all test related to all responsibilities of a QuizzesManager.
 *
 * QuizzesManager responsibilities include all responsibilities from AgentsManager, and include the following ones:
 * <li>Quizzes</li>
 * <li>Knowledge</li>
 */
interface QuizzesManagerTest<LanguageId, AlphabetId> extends AgentsManagerTest<LanguageId, AlphabetId> {

    @Override
    QuizzesManager<LanguageId, AlphabetId> createManager(MemoryDatabase db);

    static <LanguageId, AlphabetId> void addJapaneseSingAcceptation(AcceptationsManager<LanguageId, AlphabetId> manager, AlphabetId kanjiAlphabet, AlphabetId kanaAlphabet, int concept) {
        final ImmutableCorrelation<AlphabetId> correlation1 = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(kanjiAlphabet, "歌")
                .put(kanaAlphabet, "うた")
                .build();

        final ImmutableCorrelation<AlphabetId> correlation2 = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(kanjiAlphabet, "う")
                .put(kanaAlphabet, "う")
                .build();

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlation1)
                .append(correlation2)
                .build();

        manager.addAcceptation(concept, correlationArray);
    }

    static <LanguageId, AlphabetId> Integer addAgent(AgentsManager<LanguageId, AlphabetId> manager, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches) {
        final ImmutableCorrelation<AlphabetId> empty = ImmutableCorrelation.empty();
        return manager.addAgent(targetBunches, sourceBunches, ImmutableIntArraySet.empty(), empty, empty, empty, empty, 0);
    }

    static <LanguageId, AlphabetId> boolean updateAgent(AgentsManager<LanguageId, AlphabetId> manager, int agentId, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches) {
        final ImmutableCorrelation<AlphabetId> empty = ImmutableCorrelation.empty();
        return manager.updateAgent(agentId, targetBunches, sourceBunches, ImmutableIntArraySet.empty(), empty, empty, empty, empty, 0);
    }

    @Test
    default void testAddAcceptationInBunchAndQuiz() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager<LanguageId, AlphabetId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AlphabetId kanjiAlphabet = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kanaAlphabet = getNextAvailableId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(kanaAlphabet, kanjiAlphabet));

        final int myVocabularyConcept = manager.getMaxConcept() + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int esAcceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        manager.addAcceptationInBunch(myVocabularyConcept, esAcceptation);

        addJapaneseSingAcceptation(manager, kanjiAlphabet, kanaAlphabet, singConcept);

        final ImmutableList<QuestionFieldDetails<AlphabetId>> fields = new ImmutableList.Builder<QuestionFieldDetails<AlphabetId>>()
                .add(new QuestionFieldDetails<>(alphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails<>(kanjiAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.IS_ANSWER | LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final int quizId = manager.obtainQuiz(myVocabularyConcept, fields);
        assertSinglePair(esAcceptation, NO_SCORE, manager.getCurrentKnowledge(quizId));
    }

    @Test
    default void testAddQuizAndAcceptationInBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager<LanguageId, AlphabetId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId kanjiAlphabet = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kanaAlphabet = getNextAvailableId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(kanaAlphabet, kanjiAlphabet));

        final int myVocabularyConcept = manager.getMaxConcept() + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int esAcceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        addJapaneseSingAcceptation(manager, kanjiAlphabet, kanaAlphabet, singConcept);

        final ImmutableList<QuestionFieldDetails<AlphabetId>> fields = new ImmutableList.Builder<QuestionFieldDetails<AlphabetId>>()
                .add(new QuestionFieldDetails<>(alphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails<>(kanjiAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.IS_ANSWER | LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final int quizId = manager.obtainQuiz(myVocabularyConcept, fields);
        assertEmpty(manager.getCurrentKnowledge(quizId));

        manager.addAcceptationInBunch(myVocabularyConcept, esAcceptation);
        assertSinglePair(esAcceptation, NO_SCORE, manager.getCurrentKnowledge(quizId));

        manager.removeAcceptationFromBunch(myVocabularyConcept, esAcceptation);
        assertEmpty(manager.getCurrentKnowledge(quizId));
    }

    @Test
    default void testUpdateAcceptationCorrelationArray() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager<LanguageId, AlphabetId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId upperCaseAlphabet = getNextAvailableId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(alphabet, upperCaseAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final int concept = manager.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableCorrelation<AlphabetId> nullCorrelation = new ImmutableCorrelation.Builder<AlphabetId>().build();
        final ImmutableCorrelation<AlphabetId> matcher = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "er")
                .build();

        final int secondConjugationVerbBunch = manager.getMaxConcept() + 1;
        manager.addAgent(intSetOf(secondConjugationVerbBunch), noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);

        final ImmutableList<QuestionFieldDetails<AlphabetId>> quizFields = new ImmutableList.Builder<QuestionFieldDetails<AlphabetId>>()
                .add(new QuestionFieldDetails<>(alphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails<>(upperCaseAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC | LangbookDbSchema.QuestionFieldFlags.IS_ANSWER))
                .build();
        final int quizId = manager.obtainQuiz(secondConjugationVerbBunch, quizFields);

        updateAcceptationSimpleCorrelationArray(manager, alphabet, acceptationId, "beber");

        assertEquals(acceptationId, manager.getStaticAcceptationFromDynamic(acceptationId));
        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptationId);
        assertEquals("beber", texts.get(alphabet));
        assertEquals("BEBER", texts.get(upperCaseAlphabet));

        assertContainsOnly(acceptationId, manager.getAcceptationsInBunch(secondConjugationVerbBunch));
        assertSinglePair(acceptationId, NO_SCORE, manager.getCurrentKnowledge(quizId));
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayFromMatching() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager<LanguageId, AlphabetId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId upperCaseAlphabet = getNextAvailableId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(alphabet, upperCaseAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final int concept = manager.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableCorrelation<AlphabetId> nullCorrelation = new ImmutableCorrelation.Builder<AlphabetId>().build();
        final ImmutableCorrelation<AlphabetId> matcher = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ar")
                .build();

        final int firstConjugationVerbBunch = manager.getMaxConcept() + 1;
        manager.addAgent(intSetOf(firstConjugationVerbBunch), noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);

        final ImmutableList<QuestionFieldDetails<AlphabetId>> quizFields = new ImmutableList.Builder<QuestionFieldDetails<AlphabetId>>()
                .add(new QuestionFieldDetails<>(alphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails<>(upperCaseAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC | LangbookDbSchema.QuestionFieldFlags.IS_ANSWER))
                .build();
        final int quizId = manager.obtainQuiz(firstConjugationVerbBunch, quizFields);

        updateAcceptationSimpleCorrelationArray(manager, alphabet, acceptationId, "beber");

        assertEquals(acceptationId, manager.getStaticAcceptationFromDynamic(acceptationId));
        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptationId);
        assertEquals("beber", texts.get(alphabet));
        assertEquals("BEBER", texts.get(upperCaseAlphabet));

        assertEmpty(manager.getAcceptationsInBunch(firstConjugationVerbBunch));
        assertEmpty(manager.getCurrentKnowledge(quizId));
    }

    @Test
    default void testIncludeExtraTargetBunchInAgentFillingBunchForQuiz() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager<LanguageId, AlphabetId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int esSingAcc = addSimpleAcceptation(manager, esAlphabet, singConcept, "cantar");
        addSimpleAcceptation(manager, enAlphabet, singConcept, "sing");

        final int eatConcept = manager.getMaxConcept() + 1;
        final int esEatAcc = addSimpleAcceptation(manager, esAlphabet, eatConcept, "comer");
        addSimpleAcceptation(manager, enAlphabet, eatConcept, "eat");

        final int bigConcept = manager.getMaxConcept() + 1;
        final int esBigAcc = addSimpleAcceptation(manager, esAlphabet, bigConcept, "grande");
        addSimpleAcceptation(manager, enAlphabet, bigConcept, "big");

        final int smallConcept = manager.getMaxConcept() + 1;
        final int esSmallAcc = addSimpleAcceptation(manager, esAlphabet, smallConcept, "pequeño");
        addSimpleAcceptation(manager, enAlphabet, smallConcept, "small");

        final int verbsBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, verbsBunch, "verbos");
        manager.addAcceptationInBunch(verbsBunch, esSingAcc);
        manager.addAcceptationInBunch(verbsBunch, esEatAcc);

        final int adjBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, adjBunch, "adjetivos");
        manager.addAcceptationInBunch(adjBunch, esBigAcc);
        manager.addAcceptationInBunch(adjBunch, esSmallAcc);

        final int targetBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, esAlphabet, targetBunch, "palabras");

        final int agent = addAgent(manager, intSetOf(targetBunch), intSetOf(verbsBunch));
        final int quiz = manager.obtainQuiz(targetBunch, ImmutableList.<QuestionFieldDetails<AlphabetId>>empty()
                .append(new QuestionFieldDetails<>(esAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .append(new QuestionFieldDetails<>(enAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT | LangbookDbSchema.QuestionFieldFlags.IS_ANSWER)));

        assertTrue(updateAgent(manager, agent, intSetOf(targetBunch), intSetOf(verbsBunch, adjBunch)));
        final ImmutableIntPairMap knowledge = manager.getCurrentKnowledge(quiz);
        assertEqualSet(intSetOf(esSingAcc, esEatAcc, esBigAcc, esSmallAcc), knowledge.keySet());
        assertContainsOnly(NO_SCORE, knowledge.toSet());
    }
}
