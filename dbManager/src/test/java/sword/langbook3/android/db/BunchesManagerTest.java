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
interface BunchesManagerTest<ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CorrelationId, AcceptationId, BunchId> extends AcceptationsManagerTest<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId> {

    @Override
    BunchesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> createManager(MemoryDatabase db);
    BunchId conceptAsBunchId(ConceptId conceptId);

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId> AcceptationId addSpanishSingAcceptation(AcceptationsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId> manager, AlphabetId alphabet, ConceptId concept) {
        return addSimpleAcceptation(manager, alphabet, concept, "cantar");
    }

    @Test
    default void testRemoveLanguageThatIncludeAcceptationsAsBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");
        final LanguageId language = langPair.language;
        final AlphabetId alphabet = langPair.mainAlphabet;

        final ConceptId singConcept = manager.getNextAvailableConceptId();
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final ConceptId verbConcept = manager.getNextAvailableConceptId();
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
        final BunchesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final ConceptId singConcept = manager.getNextAvailableConceptId();
        final AcceptationId esAcceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);

        final ConceptId myVocabularyConcept = manager.getNextAvailableConceptId();
        final BunchId myVocabularyBunch = conceptAsBunchId(myVocabularyConcept);
        manager.addAcceptationInBunch(myVocabularyBunch, esAcceptation);
        manager.addAcceptationInBunch(myVocabularyBunch, esAcceptation);

        assertContainsOnly(esAcceptation, manager.getAcceptationsInBunch(myVocabularyBunch));
    }

    @Test
    default void testRemoveAcceptationForBunchWithAcceptationsInside() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;

        final ConceptId animalConcept = manager.getNextAvailableConceptId();
        final AcceptationId animalAcc = addSimpleAcceptation(manager, alphabet, animalConcept, "animal");

        final ConceptId catConcept = manager.getNextAvailableConceptId();
        final AcceptationId catAcc = addSimpleAcceptation(manager, alphabet, catConcept, "gato");

        final BunchId animalBunch = conceptAsBunchId(animalConcept);
        assertTrue(manager.addAcceptationInBunch(animalBunch, catAcc));
        assertTrue(manager.removeAcceptation(animalAcc));
        assertEmpty(manager.getAcceptationsInBunch(animalBunch));
    }

    @Test
    default void testShareConceptRemovesDuplicatedBunchAcceptations() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId guyConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final ConceptId personConcept = manager.getNextAvailableConceptId();
        final AcceptationId personAcc = addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final ConceptId johnConcept = manager.getNextAvailableConceptId();
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
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getBunchColumnIndex(), personConcept)
                .select(table.getAcceptationColumnIndex());

        assertEquals(1, db.select(query).size());
    }

    @Test
    default void testShareConceptKeepAcceptationsInBunchWhenRemovingDuplicatedAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId guyConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, alphabet, guyConcept, "individuo");

        final ConceptId personConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, alphabet, personConcept, "persona");

        final ConceptId johnConcept = manager.getNextAvailableConceptId();
        final AcceptationId johnAcc = addSimpleAcceptation(manager, alphabet, johnConcept, "John");
        final BunchId guyBunch = conceptAsBunchId(guyConcept);
        manager.addAcceptationInBunch(guyBunch, johnAcc);

        final ConceptId johnConcept2 = manager.getNextAvailableConceptId();
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
