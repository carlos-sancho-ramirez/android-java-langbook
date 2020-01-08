package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.List;
import sword.database.DbQuery;
import sword.database.DbValue;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.QuestionFieldDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.updateAcceptationSimpleCorrelationArray;
import static sword.langbook3.android.db.BunchesManagerTest.addSpanishSingAcceptation;
import static sword.langbook3.android.db.IntSetTestUtils.intSetOf;
import static sword.langbook3.android.db.LangbookReadableDatabase.selectSingleRow;

/**
 * Include all test related to all responsibilities of a QuizzesManager.
 *
 * QuizzesManager responsibilities include all responsibilities from AgentsManager, and include the following ones:
 * <li>Quizzes</li>
 * <li>Knowledge</li>
 */
interface QuizzesManagerTest extends AgentsManagerTest {

    @Override
    QuizzesManager createManager(MemoryDatabase db);

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

    @Test
    default void testAddAcceptationInBunchAndQuiz() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int kanjiAlphabet = manager.addLanguage("ja").mainAlphabet;
        final int kanaAlphabet = kanjiAlphabet + 1;
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

        final ImmutableList<QuestionFieldDetails> fields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(kanjiAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.IS_ANSWER | LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final int quizId = manager.obtainQuiz(myVocabularyConcept, fields);

        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        final DbQuery query = new DbQuery.Builder(knowledge)
                .select(knowledge.getAcceptationColumnIndex(), knowledge.getQuizDefinitionColumnIndex());
        final List<DbValue> row = selectSingleRow(db, query);
        assertEquals(esAcceptation, row.get(0).toInt());
        assertEquals(quizId, row.get(1).toInt());
    }

    @Test
    default void testAddQuizAndAcceptationInBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int kanjiAlphabet = manager.addLanguage("ja").mainAlphabet;
        final int kanaAlphabet = kanjiAlphabet + 1;
        assertTrue(manager.addAlphabetCopyingFromOther(kanaAlphabet, kanjiAlphabet));

        final int myVocabularyConcept = manager.getMaxConcept() + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int esAcceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        addJapaneseSingAcceptation(manager, kanjiAlphabet, kanaAlphabet, singConcept);

        final ImmutableList<QuestionFieldDetails> fields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(kanjiAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.IS_ANSWER | LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final int quizId = manager.obtainQuiz(myVocabularyConcept, fields);
        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        final DbQuery knowledgeQuery = new DbQuery.Builder(knowledge)
                .select(knowledge.getAcceptationColumnIndex(), knowledge.getQuizDefinitionColumnIndex());
        assertFalse(db.select(knowledgeQuery).hasNext());

        manager.addAcceptationInBunch(myVocabularyConcept, esAcceptation);

        final List<DbValue> row = selectSingleRow(db, knowledgeQuery);
        assertEquals(esAcceptation, row.get(0).toInt());
        assertEquals(quizId, row.get(1).toInt());

        manager.removeAcceptationFromBunch(myVocabularyConcept, esAcceptation);
        assertFalse(db.select(knowledgeQuery).hasNext());
    }

    @Test
    default void testUpdateAcceptationCorrelationArray() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int concept = manager.getMaxConcept() + 1;
        final int secondConjugationVerbBunch = concept + 1;
        final int upperCaseAlphabet = secondConjugationVerbBunch + 1;
        final Conversion conversion = new Conversion(alphabet, upperCaseAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final int acceptationId = addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "er")
                .build();

        manager.addAgent(secondConjugationVerbBunch, noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);

        final ImmutableList<QuestionFieldDetails> quizFields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(upperCaseAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC | LangbookDbSchema.QuestionFieldFlags.IS_ANSWER))
                .build();
        final int quizId = manager.obtainQuiz(secondConjugationVerbBunch, quizFields);

        updateAcceptationSimpleCorrelationArray(manager, alphabet, acceptationId, "beber");

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        List<DbValue> row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals("beber", row.get(1).toText());

        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), upperCaseAlphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals("BEBER", row.get(1).toText());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        query = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), secondConjugationVerbBunch)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());

        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        query = new DbQuery.Builder(knowledge)
                .where(knowledge.getQuizDefinitionColumnIndex(), quizId)
                .select(knowledge.getAcceptationColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayFromMatching() {
        final MemoryDatabase db = new MemoryDatabase();
        final QuizzesManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int concept = manager.getMaxConcept() + 1;
        final int firstConjugationVerbBunch = concept + 1;
        final int upperCaseAlphabet = firstConjugationVerbBunch + 1;
        final Conversion conversion = new Conversion(alphabet, upperCaseAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final int acceptationId = addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();

        manager.addAgent(firstConjugationVerbBunch, noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);

        final ImmutableList<QuestionFieldDetails> quizFields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(upperCaseAlphabet, 0, LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC | LangbookDbSchema.QuestionFieldFlags.IS_ANSWER))
                .build();
        final int quizId = manager.obtainQuiz(firstConjugationVerbBunch, quizFields);

        updateAcceptationSimpleCorrelationArray(manager, alphabet, acceptationId, "beber");

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        List<DbValue> row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals("beber", row.get(1).toText());

        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), upperCaseAlphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals("BEBER", row.get(1).toText());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        query = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), firstConjugationVerbBunch)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        assertFalse(db.select(query).hasNext());

        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        query = new DbQuery.Builder(knowledge)
                .where(knowledge.getQuizDefinitionColumnIndex(), quizId)
                .select(knowledge.getAcceptationColumnIndex());
        assertFalse(db.select(query).hasNext());
    }
}
