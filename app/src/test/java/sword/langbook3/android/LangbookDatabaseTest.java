package sword.langbook3.android;

import org.junit.Test;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.MemoryDatabase;

import static org.junit.Assert.assertEquals;
import static sword.langbook3.android.LangbookDatabase.addAcceptation;
import static sword.langbook3.android.LangbookDatabase.addAgent;
import static sword.langbook3.android.LangbookDatabase.insertCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxConceptInAcceptations;
import static sword.langbook3.android.LangbookReadableDatabase.selectSingleRow;

public final class LangbookDatabaseTest {

    @Test
    public void testAddSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int concept = alphabet + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final String text = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();
        final int correlationId = insertCorrelation(db, correlation);
        final int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationId);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals(text, stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals(text, stringRow.get(3).toText());
    }

    @Test
    public void testAddAgent() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int gerund = alphabet + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final String verbText = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbText)
                .build();
        final int correlationId = insertCorrelation(db, correlation);
        final int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationId);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);
        LangbookDbInserter.insertBunchAcceptation(db, verbConcept, acceptation, 0);

        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetBuilder().add(verbConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetBuilder().build();
        final int agentId = addAgent(db, 0, sourceBunches, diffBunches, matcher, adder, gerund, 0);

        final LangbookDbSchema.RuledConceptsTable ruledConcepts = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery ruledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), gerund)
                .where(ruledConcepts.getConceptColumnIndex(), concept)
                .select(ruledConcepts.getIdColumnIndex());
        final int ruledConcept = selectSingleRow(db, ruledConceptQuery).get(0).toInt();

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), agentId)
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(ruledAcceptations.getIdColumnIndex());
        final int ruledAcceptation = selectSingleRow(db, ruledAcceptationsQuery).get(0).toInt();

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .where(acceptations.getIdColumnIndex(), ruledAcceptation)
                .select(acceptations.getConceptColumnIndex());
        assertEquals(ruledConcept, selectSingleRow(db, acceptationQuery).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), ruledAcceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("cantando", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("cantando", stringRow.get(3).toText());
    }
}
