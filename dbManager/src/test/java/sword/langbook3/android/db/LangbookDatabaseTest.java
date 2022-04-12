package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableList;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SearchResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertSearchHistoryEntry;

/**
 * Include all test related to all responsibilities of the LangbookDatabase.
 *
 * LangbookDatabase is responsible of ensuring consistency within the database for all its features.
 * This class include tests for mixed features that cannot be added in QuizManagerTest.
 */
final class LangbookDatabaseTest {

    @Test
    void testSearchHistory() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookDatabaseManager<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, CharacterIdHolder, CharacterCompositionTypeIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder, QuizIdHolder, SentenceIdHolder> manager = new LangbookDatabaseManager<>(db, new ConceptIdManager(), new LanguageIdManager(), new AlphabetIdManager(), new CharacterIdManager(), new CharacterCompositionTypeIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), new AcceptationIdManager(), new BunchIdManager(), new BunchSetIdManager(), new RuleIdManager(), new AgentIdManager(), new QuizIdManager(), new SentenceIdManager());

        final AlphabetIdHolder alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptIdHolder concept = manager.getNextAvailableConceptId();

        final String text = "cantar";
        final AcceptationIdHolder acceptation = addSimpleAcceptation(manager, alphabet, concept, text);
        assertTrue(manager.getSearchHistory().isEmpty());

        insertSearchHistoryEntry(db, acceptation);
        final ImmutableList<SearchResult<AcceptationIdHolder, RuleIdHolder>> history = manager.getSearchHistory();
        assertEquals(1, history.size());

        final SearchResult<AcceptationIdHolder, RuleIdHolder> expectedEntry = new SearchResult<>(text, text, acceptation, false);
        assertEquals(expectedEntry, history.get(0));

        manager.removeAcceptation(acceptation);

        final LangbookDbSchema.SearchHistoryTable table = LangbookDbSchema.Tables.searchHistory;
        assertFalse(db.select(new DbQuery.Builder(table).select(table.getIdColumnIndex())).hasNext());
    }
}
