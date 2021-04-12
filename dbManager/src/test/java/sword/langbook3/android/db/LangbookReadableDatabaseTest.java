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

    private void addAgent(LangbookManager<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder> manager, BunchIdHolder sourceBunch, AlphabetIdHolder alphabet, String endMatcherText, String endAdderText, RuleIdHolder rule) {
        final ImmutableSet<BunchIdHolder> emptyBunchSet = ImmutableHashSet.empty();
        final ImmutableSet<BunchIdHolder> verbBunchSet = emptyBunchSet.add(sourceBunch);

        final ImmutableCorrelation<AlphabetIdHolder> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelation<AlphabetIdHolder> endMatcher = (endMatcherText != null)? emptyCorrelation.put(alphabet, endMatcherText) : emptyCorrelation;
        final ImmutableCorrelation<AlphabetIdHolder> endAdder = (endAdderText != null)? emptyCorrelation.put(alphabet, endAdderText) : emptyCorrelation;

        manager.addAgent(setOf(), verbBunchSet, emptyBunchSet, emptyCorrelation, emptyCorrelation, endMatcher, endAdder, rule);
    }

    private AlphabetIdHolder getNextAvailableId(ConceptsChecker<ConceptIdHolder> manager) {
        return new AlphabetIdHolder(manager.getNextAvailableConceptId().key);
    }

    private BunchIdHolder conceptAsBunchId(ConceptIdHolder conceptId) {
        return new BunchIdHolder(conceptId.key);
    }

    private RuleIdHolder conceptAsRuleId(ConceptIdHolder conceptId) {
        return (conceptId == null)? null : new RuleIdHolder(conceptId.key);
    }

    @Test
    void testReadAllMatchingBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final ConceptIdManager conceptIdManager = new ConceptIdManager();
        final LanguageIdManager languageIdManager = new LanguageIdManager();
        final AlphabetIdManager alphabetIdManager = new AlphabetIdManager();
        final SymbolArrayIdManager symbolArrayIdManager = new SymbolArrayIdManager();
        final CorrelationIdManager correlationIdManager = new CorrelationIdManager();
        final CorrelationArrayIdManager correlationArrayIdManager = new CorrelationArrayIdManager();
        final AcceptationIdManager acceptationIdManager = new AcceptationIdManager();
        final BunchIdManager bunchIdManager = new BunchIdManager();
        final BunchSetIdManager bunchSetIdManager = new BunchSetIdManager();
        final RuleIdManager ruleIdManager = new RuleIdManager();
        final AgentIdManager agentIdManager = new AgentIdManager();
        final LangbookDatabaseManager<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder> manager = new LangbookDatabaseManager<>(db, conceptIdManager, languageIdManager, alphabetIdManager, symbolArrayIdManager, correlationIdManager, correlationArrayIdManager, acceptationIdManager, bunchIdManager, bunchSetIdManager, ruleIdManager, agentIdManager);
        final AlphabetIdHolder alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptIdHolder gerundConcept = manager.getNextAvailableConceptId();
        final ConceptIdHolder pluralConcept = conceptIdManager.recheckAvailability(gerundConcept, gerundConcept);
        final ConceptIdHolder verbConceptId = conceptIdManager.recheckAvailability(pluralConcept, pluralConcept);
        final ConceptIdHolder femaleNounConceptId = conceptIdManager.recheckAvailability(verbConceptId, verbConceptId);

        final String verbBunchTitle = "verbos (1a conjugación)";
        addSimpleAcceptation(manager, alphabet, verbConceptId, verbBunchTitle);

        final String femaleNounBunchTitle = "substantivos femeninos";
        addSimpleAcceptation(manager, alphabet, femaleNounConceptId, femaleNounBunchTitle);

        final BunchIdHolder verbBunchId = conceptAsBunchId(verbConceptId);
        final RuleIdHolder gerundRule = conceptAsRuleId(gerundConcept);
        addAgent(manager, verbBunchId, alphabet, "ar", "ando", gerundRule);

        final BunchIdHolder femaleNounBunchId = conceptAsBunchId(femaleNounConceptId);
        final RuleIdHolder pluralRule = conceptAsRuleId(pluralConcept);
        addAgent(manager, femaleNounBunchId, alphabet, null, "s", pluralRule);

        final ImmutableCorrelation<AlphabetIdHolder> texts = new ImmutableCorrelation.Builder<AlphabetIdHolder>().put(alphabet, "cantar").build();
        final ImmutableMap<BunchIdHolder, String> matchingBunches = manager.readAllMatchingBunches(texts, alphabet);
        assertEquals(ImmutableHashMap.<BunchIdHolder, String>empty().put(verbBunchId, verbBunchTitle), matchingBunches);

        final ImmutableCorrelation<AlphabetIdHolder> texts2 = new ImmutableCorrelation.Builder<AlphabetIdHolder>().put(alphabet, "comer").build();
        assertTrue(manager.readAllMatchingBunches(texts2, alphabet).isEmpty());
    }

    @Test
    void testFindAcceptationFromText() {
        final ConceptIdManager conceptIdManager = new ConceptIdManager();
        final LanguageIdManager languageIdManager = new LanguageIdManager();
        final AlphabetIdManager alphabetIdManager = new AlphabetIdManager();
        final SymbolArrayIdManager symbolArrayIdManager = new SymbolArrayIdManager();
        final CorrelationIdManager correlationIdManager = new CorrelationIdManager();
        final CorrelationArrayIdManager correlationArrayIdManager = new CorrelationArrayIdManager();
        final AcceptationIdManager acceptationIdManager = new AcceptationIdManager();
        final BunchIdManager bunchIdManager = new BunchIdManager();
        final BunchSetIdManager bunchSetIdManager = new BunchSetIdManager();
        final RuleIdManager ruleIdManager = new RuleIdManager();
        final AgentIdManager agentIdManager = new AgentIdManager();

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
                    final LangbookDatabaseManager<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder> manager = new LangbookDatabaseManager<>(db, conceptIdManager, languageIdManager, alphabetIdManager, symbolArrayIdManager, correlationIdManager, correlationArrayIdManager, acceptationIdManager, bunchIdManager, bunchSetIdManager, ruleIdManager, agentIdManager);
                    final AlphabetIdHolder alphabet1 = manager.addLanguage("xx").mainAlphabet;
                    final AlphabetIdHolder alphabet2 = getNextAvailableId(manager);
                    assertTrue(manager.addAlphabetCopyingFromOther(alphabet2, alphabet1));

                    final ConceptIdHolder concept1 = manager.getNextAvailableConceptId();
                    final ConceptIdHolder concept2 = conceptIdManager.recheckAvailability(concept1, concept1);

                    final ImmutableList.Builder<AcceptationIdHolder> accListBuilder = new ImmutableList.Builder<>();
                    accListBuilder.append(addSimpleAcceptation(manager, alphabet1, concept1, textList.valueAt(0)));
                    if (textList.size() >= 2) {
                        accListBuilder.append(addSimpleAcceptation(manager, alphabet1, concept2, textList.valueAt(1)));
                    }

                    if (textList.size() == 3) {
                        final ConceptIdHolder concept3 = conceptIdManager.recheckAvailability(concept2, concept2);
                        accListBuilder.append(addSimpleAcceptation(manager, alphabet1, concept3, textList.valueAt(2)));
                    }

                    final ImmutableList<AcceptationIdHolder> accList = accListBuilder.build();

                    final int restrictionStringType = DbQuery.RestrictionStringTypes.STARTS_WITH;
                    for (int length = 1; length <= text1.length(); length++) {
                        final String queryText = text1.substring(0, length);
                        final ImmutableList<SearchResult<AcceptationIdHolder, RuleIdHolder>> results = manager.findAcceptationFromText(queryText, restrictionStringType, new ImmutableIntRange(0, Integer.MAX_VALUE));
                        final ImmutableIntSet.Builder matchingIndexesBuilder = new ImmutableIntSetCreator();
                        for (int i = 0; i < textList.size(); i++) {
                            if (textList.valueAt(i).startsWith(queryText)) {
                                matchingIndexesBuilder.add(i);
                            }
                        }
                        final ImmutableIntSet matchingIndexes = matchingIndexesBuilder.build();
                        assertEquals(matchingIndexes.size(), results.size());
                        for (int textIndex : matchingIndexes) {
                            final SearchResult<AcceptationIdHolder, RuleIdHolder> result = results.findFirst(r -> r.getId().equals(accList.valueAt(textIndex)), null);
                            assertNotNull(result);
                            assertEquals(textList.valueAt(textIndex), result.getStr());
                        }
                    }
                }
            }
        }
    }
}
