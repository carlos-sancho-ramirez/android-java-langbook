package sword.langbook3.android;

import org.junit.Test;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.database.Database;
import sword.database.MemoryDatabase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sword.langbook3.android.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxConceptInAcceptations;

public final class LangbookReadableDatabaseTest {

    private void addBunch(Database db, int id, int alphabet, String title) {
        final int verbBunchTitleId = LangbookDatabase.obtainSymbolArray(db, title);
        final int bunchCorrelation = LangbookDatabase.obtainCorrelation(db, new ImmutableIntPairMap.Builder().put(alphabet, verbBunchTitleId).build());
        final int bunchCorrelationArrayId = LangbookDatabase.obtainCorrelationArray(db, new ImmutableIntList.Builder().append(bunchCorrelation).build());
        LangbookDatabase.addAcceptation(db, id, bunchCorrelationArrayId);
    }

    private void addAgent(Database db, int sourceBunch, int alphabet, String endMatcherText, String endAdderText, int rule) {
        final ImmutableIntSet emptyBunchSet = new ImmutableIntSetBuilder().build();
        final ImmutableIntSet verbBunchSet = emptyBunchSet.add(sourceBunch);

        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        final ImmutableIntKeyMap<String> endMatcher = (endMatcherText != null)? emptyCorrelation.put(alphabet, endMatcherText) : emptyCorrelation;
        final ImmutableIntKeyMap<String> endAdder = (endAdderText != null)? emptyCorrelation.put(alphabet, endAdderText) : emptyCorrelation;

        LangbookDatabase.addAgent(db, NO_BUNCH, verbBunchSet, emptyBunchSet, emptyCorrelation, emptyCorrelation, endMatcher, endAdder, rule);
    }

    @Test
    public void testReadAllMatchingBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int gerundRule = alphabet + 1;
        final int pluralRule = gerundRule + 1;
        final int verbBunchId = pluralRule + 1;
        final int femaleNounBunchId = verbBunchId + 1;

        final String verbBunchTitle = "verbos (1a conjugación)";
        addBunch(db, verbBunchId, alphabet, verbBunchTitle);

        final String femaleNounBunchTitle = "substantivos femeninos";
        addBunch(db, femaleNounBunchId, alphabet, femaleNounBunchTitle);

        addAgent(db, verbBunchId, alphabet, "ar", "ando", gerundRule);
        addAgent(db, femaleNounBunchId, alphabet, null, "s", pluralRule);

        final ImmutableIntKeyMap<String> texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "cantar").build();
        final ImmutableIntKeyMap<String> matchingBunches = LangbookReadableDatabase.readAllMatchingBunches(db, texts, alphabet);
        assertEquals(ImmutableIntKeyMap.empty().put(verbBunchId, verbBunchTitle), matchingBunches);

        final ImmutableIntKeyMap<String> texts2 = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "comer").build();
        assertTrue(LangbookReadableDatabase.readAllMatchingBunches(db, texts2, alphabet).isEmpty());
    }
}
