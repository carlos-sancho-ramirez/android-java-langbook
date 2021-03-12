package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SearchResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AgentsManagerTest.setOf;

final class LangbookReadableDatabaseTest {

    private void addAgent(LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder> manager, BunchIdHolder sourceBunch, AlphabetIdHolder alphabet, String endMatcherText, String endAdderText, int rule) {
        final ImmutableSet<BunchIdHolder> emptyBunchSet = ImmutableHashSet.empty();
        final ImmutableSet<BunchIdHolder> verbBunchSet = emptyBunchSet.add(sourceBunch);

        final ImmutableCorrelation<AlphabetIdHolder> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelation<AlphabetIdHolder> endMatcher = (endMatcherText != null)? emptyCorrelation.put(alphabet, endMatcherText) : emptyCorrelation;
        final ImmutableCorrelation<AlphabetIdHolder> endAdder = (endAdderText != null)? emptyCorrelation.put(alphabet, endAdderText) : emptyCorrelation;

        manager.addAgent(setOf(), verbBunchSet, emptyBunchSet, emptyCorrelation, emptyCorrelation, endMatcher, endAdder, rule);
    }

    private AlphabetIdHolder getNextAvailableId(ConceptsChecker manager) {
        return new AlphabetIdHolder(manager.getMaxConcept() + 1);
    }

    private BunchIdHolder conceptAsBunchId(int conceptId) {
        return new BunchIdHolder(conceptId);
    }

    @Test
    void testReadAllMatchingBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final LanguageIdManager languageIdManager = new LanguageIdManager();
        final AlphabetIdManager alphabetIdManager = new AlphabetIdManager();
        final SymbolArrayIdManager symbolArrayIdManager = new SymbolArrayIdManager();
        final CorrelationIdManager correlationIdManager = new CorrelationIdManager();
        final CorrelationArrayIdManager correlationArrayIdManager = new CorrelationArrayIdManager();
        final AcceptationIdManager acceptationIdManager = new AcceptationIdManager();
        final BunchIdManager bunchIdManager = new BunchIdManager();
        final LangbookDatabaseManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder> manager = new LangbookDatabaseManager<>(db, languageIdManager, alphabetIdManager, symbolArrayIdManager, correlationIdManager, correlationArrayIdManager, acceptationIdManager, bunchIdManager);
        final AlphabetIdHolder alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerundRule = manager.getMaxConcept() + 1;
        final int pluralRule = gerundRule + 1;
        final int verbConceptId = pluralRule + 1;
        final int femaleNounConceptId = verbConceptId + 1;

        final String verbBunchTitle = "verbos (1a conjugación)";
        addSimpleAcceptation(manager, alphabet, verbConceptId, verbBunchTitle);

        final String femaleNounBunchTitle = "substantivos femeninos";
        addSimpleAcceptation(manager, alphabet, femaleNounConceptId, femaleNounBunchTitle);

        final BunchIdHolder verbBunchId = conceptAsBunchId(verbConceptId);
        addAgent(manager, verbBunchId, alphabet, "ar", "ando", gerundRule);

        final BunchIdHolder femaleNounBunchId = conceptAsBunchId(femaleNounConceptId);
        addAgent(manager, femaleNounBunchId, alphabet, null, "s", pluralRule);

        final ImmutableCorrelation<AlphabetIdHolder> texts = new ImmutableCorrelation.Builder<AlphabetIdHolder>().put(alphabet, "cantar").build();
        final ImmutableMap<BunchIdHolder, String> matchingBunches = manager.readAllMatchingBunches(texts, alphabet);
        assertEquals(ImmutableHashMap.<BunchIdHolder, String>empty().put(verbBunchId, verbBunchTitle), matchingBunches);

        final ImmutableCorrelation<AlphabetIdHolder> texts2 = new ImmutableCorrelation.Builder<AlphabetIdHolder>().put(alphabet, "comer").build();
        assertTrue(manager.readAllMatchingBunches(texts2, alphabet).isEmpty());
    }

    @Test
    void testFindAcceptationFromText() {
        final LanguageIdManager languageIdManager = new LanguageIdManager();
        final AlphabetIdManager alphabetIdManager = new AlphabetIdManager();
        final SymbolArrayIdManager symbolArrayIdManager = new SymbolArrayIdManager();
        final CorrelationIdManager correlationIdManager = new CorrelationIdManager();
        final CorrelationArrayIdManager correlationArrayIdManager = new CorrelationArrayIdManager();
        final AcceptationIdManager acceptationIdManager = new AcceptationIdManager();
        final BunchIdManager bunchIdManager = new BunchIdManager();

        final ImmutableSet<String> texts = new ImmutableHashSet.Builder<String>()
                .add("hello")
                .add("Hi")
                .add("hi")
                .add("bye")
                .add("byebye")
                .build();

        for (String text1 : texts) {
            for (String text2 : texts) {
                for (String text3 : texts) {
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
                    final LangbookDatabaseManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder> manager = new LangbookDatabaseManager<>(db, languageIdManager, alphabetIdManager, symbolArrayIdManager, correlationIdManager, correlationArrayIdManager, acceptationIdManager, bunchIdManager);
                    final AlphabetIdHolder alphabet1 = manager.addLanguage("xx").mainAlphabet;
                    final AlphabetIdHolder alphabet2 = getNextAvailableId(manager);
                    assertTrue(manager.addAlphabetCopyingFromOther(alphabet2, alphabet1));

                    final int concept1 = manager.getMaxConcept() + 1;
                    final int concept2 = concept1 + 1;
                    final int concept3 = concept2 + 1;

                    final ImmutableList.Builder<AcceptationIdHolder> accListBuilder = new ImmutableList.Builder<>();
                    for (int i = 0; i < textList.size(); i++) {
                        accListBuilder.append(addSimpleAcceptation(manager, alphabet1, concept1 + i, textList.valueAt(i)));
                    }
                    final ImmutableList<AcceptationIdHolder> accList = accListBuilder.build();

                    final int restrictionStringType = DbQuery.RestrictionStringTypes.STARTS_WITH;
                    for (int length = 1; length <= text1.length(); length++) {
                        final String queryText = text1.substring(0, length);
                        final ImmutableList<SearchResult<AcceptationIdHolder>> results = manager.findAcceptationFromText(queryText, restrictionStringType, new ImmutableIntRange(0, Integer.MAX_VALUE));
                        final ImmutableIntSet.Builder matchingIndexesBuilder = new ImmutableIntSetCreator();
                        for (int i = 0; i < textList.size(); i++) {
                            if (textList.valueAt(i).startsWith(queryText)) {
                                matchingIndexesBuilder.add(i);
                            }
                        }
                        final ImmutableIntSet matchingIndexes = matchingIndexesBuilder.build();
                        assertEquals(matchingIndexes.size(), results.size());
                        for (int textIndex : matchingIndexes) {
                            final SearchResult<AcceptationIdHolder> result = results.findFirst(r -> r.getId().equals(accList.valueAt(textIndex)), null);
                            assertNotNull(result);
                            assertEquals(textList.valueAt(textIndex), result.getStr());
                        }
                    }
                }
            }
        }
    }
}
