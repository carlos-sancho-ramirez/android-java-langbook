package sword.langbook3.android.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.collections.IntValueMap;
import sword.collections.SizableTestUtils;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.QuestionFieldDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.IntTraversableTestUtils.assertContainsOnly;
import static sword.collections.SetTestUtils.assertEqualSet;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.collections.EqualUtils.equal;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.updateAcceptationSimpleCorrelationArray;
import static sword.langbook3.android.db.AgentsManagerTest.setOf;
import static sword.langbook3.android.db.BunchesManagerTest.addSpanishSingAcceptation;
import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;

/**
 * Include all test related to all responsibilities of a QuizzesManager.
 *
 * QuizzesManager responsibilities include all responsibilities from AgentsManager, and include the following ones:
 * <li>Quizzes</li>
 * <li>Knowledge</li>
 */
interface QuizzesManagerTest<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> extends AgentsManagerTest<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> {

    @Override
    QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> createManager(MemoryDatabase db);

    static <K> void assertSinglePair(K expectedKey, int expectedValue, IntValueMap<K> map) {
        SizableTestUtils.assertSize(1, map);
        K actualKey = map.keyAt(0);
        if (!equal(expectedKey, actualKey)) {
            Assertions.fail("Single key in map was expected to be " + expectedKey + ", but it was " + actualKey);
        }

        int actualValue = map.valueAt(0);
        if (expectedValue != actualValue) {
            Assertions.fail("Single value in map was expected to be " + expectedValue + ", but it was " + actualValue);
        }

    }

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId> void addJapaneseSingAcceptation(AcceptationsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> manager, AlphabetId kanjiAlphabet, AlphabetId kanaAlphabet, int concept) {
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

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> Integer addAgent(AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches) {
        final ImmutableCorrelation<AlphabetId> empty = ImmutableCorrelation.empty();
        return manager.addAgent(targetBunches, sourceBunches, ImmutableHashSet.empty(), empty, empty, empty, empty, null);
    }

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> boolean updateAgent(AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager, int agentId, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches) {
        final ImmutableCorrelation<AlphabetId> empty = ImmutableCorrelation.empty();
        return manager.updateAgent(agentId, targetBunches, sourceBunches, ImmutableHashSet.empty(), empty, empty, empty, empty, null);
    }

    @Test
    default void testAddAcceptationInBunchAndQuiz() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final AlphabetId kanjiAlphabet = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kanaAlphabet = getNextAvailableAlphabetId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(kanaAlphabet, kanjiAlphabet));

        final int myVocabularyConcept = manager.getNextAvailableConceptId();
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final AcceptationId esAcceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);

        final BunchId myVocabularyBunch = conceptAsBunchId(myVocabularyConcept);
        manager.addAcceptationInBunch(myVocabularyBunch, esAcceptation);

        addJapaneseSingAcceptation(manager, kanjiAlphabet, kanaAlphabet, singConcept);

        final ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields = new ImmutableList.Builder<QuestionFieldDetails<AlphabetId, RuleId>>()
                .add(new QuestionFieldDetails<>(alphabet, null, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails<>(kanjiAlphabet, null, LangbookDbSchema.QuestionFieldFlags.IS_ANSWER | LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final int quizId = manager.obtainQuiz(myVocabularyBunch, fields);
        assertSinglePair(esAcceptation, NO_SCORE, manager.getCurrentKnowledge(quizId));
    }

    @Test
    default void testAddQuizAndAcceptationInBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId kanjiAlphabet = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kanaAlphabet = getNextAvailableAlphabetId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(kanaAlphabet, kanjiAlphabet));

        final int myVocabularyConcept = manager.getNextAvailableConceptId();
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final AcceptationId esAcceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        addJapaneseSingAcceptation(manager, kanjiAlphabet, kanaAlphabet, singConcept);

        final ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields = new ImmutableList.Builder<QuestionFieldDetails<AlphabetId, RuleId>>()
                .add(new QuestionFieldDetails<>(alphabet, null, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails<>(kanjiAlphabet, null, LangbookDbSchema.QuestionFieldFlags.IS_ANSWER | LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final BunchId myVocabularyBunch = conceptAsBunchId(myVocabularyConcept);
        final int quizId = manager.obtainQuiz(myVocabularyBunch, fields);
        assertEmpty(manager.getCurrentKnowledge(quizId));

        manager.addAcceptationInBunch(myVocabularyBunch, esAcceptation);
        assertSinglePair(esAcceptation, NO_SCORE, manager.getCurrentKnowledge(quizId));

        manager.removeAcceptationFromBunch(myVocabularyBunch, esAcceptation);
        assertEmpty(manager.getCurrentKnowledge(quizId));
    }

    @Test
    default void testUpdateAcceptationCorrelationArray() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId upperCaseAlphabet = getNextAvailableAlphabetId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(alphabet, upperCaseAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final int concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptationId = addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableCorrelation<AlphabetId> nullCorrelation = new ImmutableCorrelation.Builder<AlphabetId>().build();
        final ImmutableCorrelation<AlphabetId> matcher = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "er")
                .build();

        final int secondConjugationVerbConcept = manager.getNextAvailableConceptId();
        final BunchId secondConjugationVerbBunch = conceptAsBunchId(secondConjugationVerbConcept);
        manager.addAgent(setOf(secondConjugationVerbBunch), noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, null);

        final ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> quizFields = new ImmutableList.Builder<QuestionFieldDetails<AlphabetId, RuleId>>()
                .add(new QuestionFieldDetails<>(alphabet, null, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails<>(upperCaseAlphabet, null, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC | LangbookDbSchema.QuestionFieldFlags.IS_ANSWER))
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
        final QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId upperCaseAlphabet = getNextAvailableAlphabetId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(alphabet, upperCaseAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final int concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptationId = addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final ImmutableSet<BunchId> noBunches = setOf();
        final ImmutableCorrelation<AlphabetId> nullCorrelation = new ImmutableCorrelation.Builder<AlphabetId>().build();
        final ImmutableCorrelation<AlphabetId> matcher = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, "ar")
                .build();

        final int firstConjugationVerbConcept = manager.getNextAvailableConceptId();
        final BunchId firstConjugationVerbBunch = conceptAsBunchId(firstConjugationVerbConcept);
        manager.addAgent(setOf(firstConjugationVerbBunch), noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, null);

        final ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> quizFields = new ImmutableList.Builder<QuestionFieldDetails<AlphabetId, RuleId>>()
                .add(new QuestionFieldDetails<>(alphabet, null, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails<>(upperCaseAlphabet, null, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC | LangbookDbSchema.QuestionFieldFlags.IS_ANSWER))
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
        final QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager = createManager(db);

        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;

        final int singConcept = manager.getNextAvailableConceptId();
        final AcceptationId esSingAcc = addSimpleAcceptation(manager, esAlphabet, singConcept, "cantar");
        addSimpleAcceptation(manager, enAlphabet, singConcept, "sing");

        final int eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId esEatAcc = addSimpleAcceptation(manager, esAlphabet, eatConcept, "comer");
        addSimpleAcceptation(manager, enAlphabet, eatConcept, "eat");

        final int bigConcept = manager.getNextAvailableConceptId();
        final AcceptationId esBigAcc = addSimpleAcceptation(manager, esAlphabet, bigConcept, "grande");
        addSimpleAcceptation(manager, enAlphabet, bigConcept, "big");

        final int smallConcept = manager.getNextAvailableConceptId();
        final AcceptationId esSmallAcc = addSimpleAcceptation(manager, esAlphabet, smallConcept, "pequeño");
        addSimpleAcceptation(manager, enAlphabet, smallConcept, "small");

        final int verbsConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, verbsConcept, "verbos");

        final BunchId verbsBunch = conceptAsBunchId(verbsConcept);
        manager.addAcceptationInBunch(verbsBunch, esSingAcc);
        manager.addAcceptationInBunch(verbsBunch, esEatAcc);

        final int adjConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, adjConcept, "adjetivos");

        final BunchId adjBunch = conceptAsBunchId(adjConcept);
        manager.addAcceptationInBunch(adjBunch, esBigAcc);
        manager.addAcceptationInBunch(adjBunch, esSmallAcc);

        final int targetConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, targetConcept, "palabras");

        final BunchId targetBunch = conceptAsBunchId(targetConcept);
        final int agent = addAgent(manager, setOf(targetBunch), setOf(verbsBunch));
        final int quiz = manager.obtainQuiz(targetBunch, ImmutableList.<QuestionFieldDetails<AlphabetId, RuleId>>empty()
                .append(new QuestionFieldDetails<>(esAlphabet, null, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .append(new QuestionFieldDetails<>(enAlphabet, null, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT | LangbookDbSchema.QuestionFieldFlags.IS_ANSWER)));

        assertTrue(updateAgent(manager, agent, setOf(targetBunch), setOf(verbsBunch, adjBunch)));
        final ImmutableIntValueMap<AcceptationId> knowledge = manager.getCurrentKnowledge(quiz);
        assertEqualSet(setOf(esSingAcc, esEatAcc, esBigAcc, esSmallAcc), knowledge.keySet());
        assertContainsOnly(NO_SCORE, knowledge.toSet());
    }
}
