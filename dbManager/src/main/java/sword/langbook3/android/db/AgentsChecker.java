package sword.langbook3.android.db;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.DisplayableItem;
import sword.langbook3.android.models.MorphologyReaderResult;
import sword.langbook3.android.models.SearchResult;

public interface AgentsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> extends BunchesChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> {

    /**
     * Check all bunches including agents that may match the given texts.
     *
     * For simplicity, this will only pick bunches declared as source bunches
     * in agents that are applying a rule and has no diff bunches.
     *
     * Required conditions are:
     * <li>The agent's matchers must match the given string</li>
     * <li>Start or end matcher must not be empty</li>
     * <li>There must be an adder different from the matcher</li>
     *
     * @param texts Map containing the word to be matched. Keys in the map are alphabets and values are the text on those alphabets.
     * @param preferredAlphabet User's defined alphabet.
     * @return A map whose keys are bunches (concepts) and value are the suitable way to represent that bunch, according to the given preferred alphabet.
     */
    ImmutableMap<BunchId, String> readAllMatchingBunches(ImmutableCorrelation<AlphabetId> texts, AlphabetId preferredAlphabet);
    ImmutableMap<RuleId, String> readAllRules(AlphabetId preferredAlphabet);
    ImmutableIntSet getAgentIds();
    ImmutableList<SearchResult<AcceptationId, RuleId>> findAcceptationFromText(String queryText, int restrictionStringType, ImmutableIntRange range);
    AgentRegister<CorrelationId, RuleId> getAgentRegister(int agentId);
    AgentDetails<AlphabetId, BunchId, RuleId> getAgentDetails(int agentId);
    ImmutableList<DisplayableItem<AcceptationId>> readBunchSetAcceptationsAndTexts(int bunchSet, AlphabetId preferredAlphabet);
    ImmutableList<SearchResult<AcceptationId, RuleId>> findAcceptationAndRulesFromText(String queryText, int restrictionStringType, ImmutableIntRange range);
    AcceptationId getStaticAcceptationFromDynamic(AcceptationId dynamicAcceptation);
    ConceptId findRuledConcept(RuleId rule, ConceptId concept);
    ImmutableMap<ConceptId, ConceptId> findRuledConceptsByRule(RuleId rule);
    AcceptationId findRuledAcceptationByAgentAndBaseAcceptation(int agentId, AcceptationId baseAcceptation);
    String readAcceptationMainText(AcceptationId acceptation);
    ImmutableIntSet findAllAgentsThatIncludedAcceptationInBunch(BunchId bunch, AcceptationId acceptation);
    ImmutableMap<AcceptationId, AcceptationId> getAgentProcessedMap(int agentId);
    MorphologyReaderResult<AcceptationId, RuleId> readMorphologiesFromAcceptation(AcceptationId acceptation, AlphabetId preferredAlphabet);
    ImmutableSet<AcceptationId> getAcceptationsInBunchByBunchAndAgent(BunchId bunch, int agent);
    ImmutableSet<BunchId> getBunchSet(int setId);
    AcceptationId findRuledAcceptationByRuleAndBaseAcceptation(RuleId rule, AcceptationId baseAcceptation);
}
