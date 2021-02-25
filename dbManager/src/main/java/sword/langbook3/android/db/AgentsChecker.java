package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
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

public interface AgentsChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> extends BunchesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> {

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
    ImmutableIntKeyMap<String> readAllMatchingBunches(ImmutableCorrelation<AlphabetId> texts, AlphabetId preferredAlphabet);
    ImmutableIntKeyMap<String> readAllRules(AlphabetId preferredAlphabet);
    ImmutableIntSet getAgentIds();
    ImmutableList<SearchResult<AcceptationId>> findAcceptationFromText(String queryText, int restrictionStringType, ImmutableIntRange range);
    AgentRegister<CorrelationId> getAgentRegister(int agentId);
    AgentDetails<AlphabetId> getAgentDetails(int agentId);
    ImmutableList<DisplayableItem<AcceptationId>> readBunchSetAcceptationsAndTexts(int bunchSet, AlphabetId preferredAlphabet);
    ImmutableList<SearchResult<AcceptationId>> findAcceptationAndRulesFromText(String queryText, int restrictionStringType, ImmutableIntRange range);
    AcceptationId getStaticAcceptationFromDynamic(AcceptationId dynamicAcceptation);
    Integer findRuledConcept(int rule, int concept);
    ImmutableIntPairMap findRuledConceptsByRule(int rule);
    AcceptationId findRuledAcceptationByAgentAndBaseAcceptation(int agentId, AcceptationId baseAcceptation);
    String readAcceptationMainText(AcceptationId acceptation);
    ImmutableIntSet findAllAgentsThatIncludedAcceptationInBunch(int bunch, AcceptationId acceptation);
    ImmutableMap<AcceptationId, AcceptationId> getAgentProcessedMap(int agentId);
    MorphologyReaderResult<AcceptationId> readMorphologiesFromAcceptation(AcceptationId acceptation, AlphabetId preferredAlphabet);
    ImmutableSet<AcceptationId> getAcceptationsInBunchByBunchAndAgent(int bunch, int agent);
    ImmutableIntSet getBunchSet(int setId);
    AcceptationId findRuledAcceptationByRuleAndBaseAcceptation(int rule, AcceptationId baseAcceptation);
}
