package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntSet;
import sword.database.Database;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.LanguageCreationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.selectSingleRow;

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

    static ImmutableIntSet findBunchesWhereAcceptationIsIncluded(Database db, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .select(table.getBunchColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static ImmutableIntSet findAcceptationsIncludedInBunch(Database db, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .select(table.getAcceptationColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    @Test
    default void testRemoveLanguageThatIncludeAcceptationsAsBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final BunchesManager manager = createManager(db);

        final String langCode = "es";
        final LanguageCreationResult langPair = manager.addLanguage(langCode);
        final int language = langPair.language;
        final int alphabet = langPair.mainAlphabet;
        final int verbConcept = manager.getMaxConcept() + 1;
        final int singConcept = verbConcept + 1;

        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        final int verbAcceptation = addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");
        assertTrue(manager.addAcceptationInBunch(verbConcept, singAcceptation));

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode(langCode));
        assertTrue(manager.getAcceptationTexts(singAcceptation).isEmpty());
        assertTrue(manager.getAcceptationTexts(verbAcceptation).isEmpty());
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

        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), myVocabularyConcept)
                .select(table.getAcceptationColumnIndex());
        assertEquals(esAcceptation, selectSingleRow(db, query).get(0).toInt());
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
        assertTrue(manager.getAcceptationsInBunch(animalConcept).isEmpty());
    }
}
