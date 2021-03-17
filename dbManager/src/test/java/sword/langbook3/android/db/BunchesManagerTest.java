package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.LanguageCreationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;

/**
 * Include all test related to all responsibilities of a BunchesManager.
 *
 * BunchesManager responsibilities include all responsibilities from AcceptationsManager, and include the following ones:
 * <li>Bunches</li>
 */
interface BunchesManagerTest<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> extends AcceptationsManagerTest<LanguageId, AlphabetId, CorrelationId, AcceptationId> {

    @Override
    BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> createManager(MemoryDatabase db);
    BunchId conceptAsBunchId(int conceptId);

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId> AcceptationId addSpanishSingAcceptation(AcceptationsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> manager, AlphabetId alphabet, int concept) {
        return addSimpleAcceptation(manager, alphabet, concept, "cantar");
    }

    @Test
    default void testRemoveLanguageThatIncludeAcceptationsAsBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");
        final LanguageId language = langPair.language;
        final AlphabetId alphabet = langPair.mainAlphabet;
        final int verbConcept = manager.getNextAvailableConceptId();
        final int singConcept = verbConcept + 1;

        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        final AcceptationId verbAcceptation = addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");
        assertTrue(manager.addAcceptationInBunch(conceptAsBunchId(verbConcept), singAcceptation));

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("es"));
        assertEmpty(manager.getAcceptationTexts(singAcceptation));
        assertEmpty(manager.getAcceptationTexts(verbAcceptation));
    }

    @Test
    default void testCallingTwiceAddAcceptationInBunchDoesNotDuplicate() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int kanjiAlphabet = manager.getNextAvailableConceptId();
        final int kanaAlphabet = kanjiAlphabet + 1;
        final int myVocabularyConcept = kanaAlphabet + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final AcceptationId esAcceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final BunchId myVocabularyBunch = conceptAsBunchId(myVocabularyConcept);
        manager.addAcceptationInBunch(myVocabularyBunch, esAcceptation);
        manager.addAcceptationInBunch(myVocabularyBunch, esAcceptation);

        assertContainsOnly(esAcceptation, manager.getAcceptationsInBunch(myVocabularyBunch));
    }

    @Test
    default void testRemoveAcceptationForBunchWithAcceptationsInside() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int animalConcept = manager.getNextAvailableConceptId();
        final int catConcept = animalConcept + 1;

        final AcceptationId animalAcc = addSimpleAcceptation(manager, alphabet, animalConcept, "animal");
        final AcceptationId catAcc = addSimpleAcceptation(manager, alphabet, catConcept, "gato");

        final BunchId animalBunch = conceptAsBunchId(animalConcept);
        assertTrue(manager.addAcceptationInBunch(animalBunch, catAcc));
        assertTrue(manager.removeAcceptation(animalAcc));
        assertEmpty(manager.getAcceptationsInBunch(animalBunch));
    }

    @Test
    default void testShareConceptRemovesDuplicatedBunchAcceptations() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int guyConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final int personConcept = manager.getNextAvailableConceptId();
        final AcceptationId personAcc = addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final int johnConcept = manager.getNextAvailableConceptId();
        final AcceptationId johnAcc = addSimpleAcceptation(manager, alphabet, johnConcept, "John");
        final BunchId guyBunch = conceptAsBunchId(guyConcept);
        final BunchId personBunch = conceptAsBunchId(personConcept);
        manager.addAcceptationInBunch(guyBunch, johnAcc);
        manager.addAcceptationInBunch(personBunch, johnAcc);

        assertTrue(manager.shareConcept(personAcc, guyConcept));
        assertEmpty(manager.findAcceptationsByConcept(guyConcept));
        assertEmpty(manager.getAcceptationsInBunch(guyBunch));
        assertContainsOnly(johnAcc, manager.getAcceptationsInBunch(personBunch));

        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), personConcept)
                .select(table.getAcceptationColumnIndex());

        assertEquals(1, db.select(query).size());
    }

    @Test
    default void testShareConceptKeepAcceptationsInBunchWhenRemovingDuplicatedAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int guyConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final int personConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final int johnConcept = manager.getNextAvailableConceptId();
        final AcceptationId johnAcc = addSimpleAcceptation(manager, alphabet, johnConcept, "John");
        final BunchId guyBunch = conceptAsBunchId(guyConcept);
        manager.addAcceptationInBunch(guyBunch, johnAcc);

        final int johnConcept2 = manager.getNextAvailableConceptId();
        final AcceptationId johnAcc2 = addSimpleAcceptation(manager, alphabet, johnConcept2, "John");
        final BunchId personBunch = conceptAsBunchId(personConcept);
        manager.addAcceptationInBunch(personBunch, johnAcc2);

        assertTrue(manager.shareConcept(johnAcc, johnConcept2));
        assertContainsOnly(johnAcc, manager.findAcceptationsByConcept(johnConcept));
        assertEmpty(manager.findAcceptationsByConcept(johnConcept2));

        assertContainsOnly(johnAcc, manager.getAcceptationsInBunch(guyBunch));
        assertContainsOnly(johnAcc, manager.getAcceptationsInBunch(personBunch));
    }
}
