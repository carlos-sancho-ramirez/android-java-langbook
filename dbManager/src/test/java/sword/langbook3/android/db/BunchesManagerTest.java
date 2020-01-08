package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.database.MemoryDatabase;
import sword.langbook3.android.models.LanguageCreationResult;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.IntTraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.db.SizableTestUtils.assertEmpty;

/**
 * Include all test related to all responsibilities of a BunchesManager.
 *
 * BunchesManager responsibilities include all responsibilities from AcceptationsManager, and include the following ones:
 * <li>Bunches</li>
 */
interface BunchesManagerTest extends AcceptationsManagerTest {

    @Override
    BunchesManager createManager(MemoryDatabase db);

    static int addSpanishSingAcceptation(AcceptationsManager manager, int alphabet, int concept) {
        return addSimpleAcceptation(manager, alphabet, concept, "cantar");
    }

    @Test
    default void testRemoveLanguageThatIncludeAcceptationsAsBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager manager = createManager(db);

        final LanguageCreationResult langPair = manager.addLanguage("es");
        final int language = langPair.language;
        final int alphabet = langPair.mainAlphabet;
        final int verbConcept = manager.getMaxConcept() + 1;
        final int singConcept = verbConcept + 1;

        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        final int verbAcceptation = addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");
        assertTrue(manager.addAcceptationInBunch(verbConcept, singAcceptation));

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("es"));
        assertEmpty(manager.getAcceptationTexts(singAcceptation));
        assertEmpty(manager.getAcceptationTexts(verbAcceptation));
    }

    @Test
    default void testCallingTwiceAddAcceptationInBunchDoesNotDuplicate() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int kanjiAlphabet = manager.getMaxConcept() + 1;
        final int kanaAlphabet = kanjiAlphabet + 1;
        final int myVocabularyConcept = kanaAlphabet + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int esAcceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        manager.addAcceptationInBunch(myVocabularyConcept, esAcceptation);
        manager.addAcceptationInBunch(myVocabularyConcept, esAcceptation);

        assertContainsOnly(esAcceptation, manager.getAcceptationsInBunch(myVocabularyConcept));
    }

    @Test
    default void testRemoveAcceptationForBunchWithAcceptationsInside() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int animalConcept = manager.getMaxConcept() + 1;
        final int catConcept = animalConcept + 1;

        final int animalAcc = addSimpleAcceptation(manager, alphabet, animalConcept, "animal");
        final int catAcc = addSimpleAcceptation(manager, alphabet, catConcept, "gato");

        assertTrue(manager.addAcceptationInBunch(animalConcept, catAcc));
        assertTrue(manager.removeAcceptation(animalAcc));
        assertEmpty(manager.getAcceptationsInBunch(animalConcept));
    }
}