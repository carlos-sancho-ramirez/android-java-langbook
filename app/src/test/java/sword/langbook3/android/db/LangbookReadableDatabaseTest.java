package sword.langbook3.android.db;

import org.junit.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.database.Database;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SearchResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.LangbookDatabase.addAlphabetCopyingFromOther;
import static sword.langbook3.android.db.LangbookDatabase.addLanguage;
import static sword.langbook3.android.db.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAcceptationFromText;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxConcept;

public final class LangbookReadableDatabaseTest {

    private void addAgent(Database db, int sourceBunch, int alphabet, String endMatcherText, String endAdderText, int rule) {
        final ImmutableIntSet emptyBunchSet = new ImmutableIntSetCreator().build();
        final ImmutableIntSet verbBunchSet = emptyBunchSet.add(sourceBunch);

        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        final ImmutableIntKeyMap<String> endMatcher = (endMatcherText != null)? emptyCorrelation.put(alphabet, endMatcherText) : emptyCorrelation;
        final ImmutableIntKeyMap<String> endAdder = (endAdderText != null)? emptyCorrelation.put(alphabet, endAdderText) : emptyCorrelation;

        LangbookDatabase.addAgent(db, NO_BUNCH, verbBunchSet, emptyBunchSet, emptyCorrelation, emptyCorrelation, endMatcher, endAdder, rule);
    }

    @Test
    public void testReadAllMatchingBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final LangbookDatabaseManager manager = new LangbookDatabaseManager(db);
        final int alphabet = addLanguage(db, "es").mainAlphabet;
        final int gerundRule = getMaxConcept(db) + 1;
        final int pluralRule = gerundRule + 1;
        final int verbBunchId = pluralRule + 1;
        final int femaleNounBunchId = verbBunchId + 1;

        final String verbBunchTitle = "verbos (1a conjugación)";
        addSimpleAcceptation(manager, alphabet, verbBunchId, verbBunchTitle);

        final String femaleNounBunchTitle = "substantivos femeninos";
        addSimpleAcceptation(manager, alphabet, femaleNounBunchId, femaleNounBunchTitle);

        addAgent(db, verbBunchId, alphabet, "ar", "ando", gerundRule);
        addAgent(db, femaleNounBunchId, alphabet, null, "s", pluralRule);

        final ImmutableIntKeyMap<String> texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "cantar").build();
        final ImmutableIntKeyMap<String> matchingBunches = LangbookReadableDatabase
                .readAllMatchingBunches(db, texts, alphabet);
        assertEquals(ImmutableIntKeyMap.empty().put(verbBunchId, verbBunchTitle), matchingBunches);

        final ImmutableIntKeyMap<String> texts2 = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "comer").build();
        assertTrue(LangbookReadableDatabase.readAllMatchingBunches(db, texts2, alphabet).isEmpty());
    }

    private interface CorrelationObtainer {
        ImmutableIntKeyMap<String> obtain(int firstAlphabet, String text);
    }

    public void checkFindAcceptationFromText(CorrelationObtainer corrObtainer) {
        final ImmutableSet<String> texts = new ImmutableHashSet.Builder<String>()
                .add("hello")
                .add("Hi")
                .add("hi")
                .add("bye")
                .add("byebye")
                .build();

        for (String text1 : texts) for (String text2 : texts) for (String text3 : texts) {
            final ImmutableList.Builder<String> textListBuilder = new ImmutableList.Builder<>();
            textListBuilder.append(text1);

            if (text2 != text1) {
                textListBuilder.append(text2);
            }

            if (text3 != text1 && text3 != text2) {
                textListBuilder.append(text3);
            }

            final ImmutableList<String> textList = textListBuilder.build();

            final MemoryDatabase db = new MemoryDatabase();
            final LangbookDatabaseManager manager = new LangbookDatabaseManager(db);
            final int alphabet1 = addLanguage(db, "xx").mainAlphabet;
            final int alphabet2 = addAlphabetCopyingFromOther(db, alphabet1);
            final int concept1 = getMaxConcept(db) + 1;
            final int concept2 = concept1 + 1;
            final int concept3 = concept2 + 1;

            final ImmutableIntList.Builder accListBuilder = new ImmutableIntList.Builder();
            for (int i = 0; i < textList.size(); i++) {
                accListBuilder.append(addSimpleAcceptation(manager, alphabet1, concept1 + i, textList.valueAt(i)));
            }
            final ImmutableIntList accList = accListBuilder.build();

            final int restrictionStringType = DbQuery.RestrictionStringTypes.STARTS_WITH;
            for (int length = 1; length <= text1.length(); length++) {
                final String queryText = text1.substring(0, length);
                final ImmutableList<SearchResult> results = findAcceptationFromText(db, queryText, restrictionStringType);
                final ImmutableIntSet.Builder matchingIndexesBuilder = new ImmutableIntSetCreator();
                for (int i = 0; i < textList.size(); i++) {
                    if (textList.valueAt(i).startsWith(queryText)) {
                        matchingIndexesBuilder.add(i);
                    }
                }
                final ImmutableIntSet matchingIndexes = matchingIndexesBuilder.build();
                assertEquals(matchingIndexes.size(), results.size());
                for (int textIndex : matchingIndexes) {
                    final SearchResult result = results.findFirst(r -> r.getId() == accList.valueAt(textIndex), null);
                    assertNotNull(result);
                    assertEquals(textList.valueAt(textIndex), result.getStr());
                    assertEquals(SearchResult.Types.ACCEPTATION, result.getType());
                }
            }
        }
    }

    @Test
    public void testFindAcceptationFromTextForOneAlphabetLanguageWord() {
        checkFindAcceptationFromText((firstAlphabet, text) ->
                new ImmutableIntKeyMap.Builder<String>().put(firstAlphabet, text).build()
        );
    }

    @Test
    public void testFindAcceptationFromTextForTwoAlphabetLanguageWord() {
        checkFindAcceptationFromText((firstAlphabet, text) ->
                new ImmutableIntKeyMap.Builder<String>()
                        .put(firstAlphabet, text)
                        .put(firstAlphabet + 1, text)
                        .build()
        );
    }
}
