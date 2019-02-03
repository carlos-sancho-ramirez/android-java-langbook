package sword.langbook3.android;

import org.junit.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.database.Database;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static sword.langbook3.android.LangbookDatabase.addAcceptation;
import static sword.langbook3.android.LangbookDatabase.obtainCorrelation;
import static sword.langbook3.android.LangbookDatabase.obtainCorrelationArray;
import static sword.langbook3.android.LangbookDatabase.obtainSymbolArray;
import static sword.langbook3.android.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.LangbookReadableDatabase.findAcceptationFromText;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxConceptInAcceptations;

public final class LangbookReadableDatabaseTest {

    private void addBunch(Database db, int id, int alphabet, String title) {
        final int verbBunchTitleId = LangbookDatabase.obtainSymbolArray(db, title);
        final int bunchCorrelation = LangbookDatabase.obtainCorrelation(db, new ImmutableIntPairMap.Builder().put(alphabet, verbBunchTitleId).build());
        final int bunchCorrelationArrayId = LangbookDatabase.obtainCorrelationArray(db, new ImmutableIntList.Builder().append(bunchCorrelation).build());
        addAcceptation(db, id, bunchCorrelationArrayId);
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

    @Test
    public void testFindAcceptationFromTextForOneAlphabetLanguageWord() {
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
            final int language = getMaxConceptInAcceptations(db) + 1;
            final int alphabet = language + 1;
            final int concept1 = alphabet + 1;
            final int concept2 = concept1 + 1;
            final int concept3 = concept2 + 1;

            final ImmutableIntList.Builder accListBuilder = new ImmutableIntList.Builder();
            for (int i = 0; i < textList.size(); i++) {
                final int symbolArrayId = obtainSymbolArray(db, textList.valueAt(i));
                final int correlationId = obtainCorrelation(db, new ImmutableIntPairMap.Builder().put(alphabet, symbolArrayId).build());
                final int corrArrayId = obtainCorrelationArray(db, new ImmutableIntList.Builder().add(correlationId).build());
                accListBuilder.append(addAcceptation(db, concept1 + i, corrArrayId));
            }
            final ImmutableIntList accList = accListBuilder.build();

            final int restrictionStringType = DbQuery.RestrictionStringTypes.STARTS_WITH;
            for (int length = 1; length <= text1.length(); length++) {
                final String queryText = text1.substring(0, length);
                final ImmutableSet<SearchResult> results = findAcceptationFromText(db, queryText, restrictionStringType).toSet();
                final ImmutableIntSetBuilder matchingIndexesBuilder = new ImmutableIntSetBuilder();
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
}
