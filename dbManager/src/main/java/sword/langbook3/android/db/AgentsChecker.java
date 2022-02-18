package sword.langbook3.android.db;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.DisplayableItem;
import sword.langbook3.android.models.MorphologyReaderResult;
import sword.langbook3.android.models.SearchResult;

public interface AgentsChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> extends BunchesChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId> {

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
    ImmutableSet<AgentId> getAgentIds();
    ImmutableList<SearchResult<AcceptationId, RuleId>> findAcceptationFromText(String queryText, int restrictionStringType, ImmutableIntRange range);
    AgentRegister<CorrelationId, CorrelationArrayId, BunchSetId, RuleId> getAgentRegister(AgentId agentId);
    AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> getAgentDetails(AgentId agentId);
    ImmutableList<DisplayableItem<AcceptationId>> readBunchSetAcceptationsAndTexts(BunchSetId bunchSet, AlphabetId preferredAlphabet);
    ImmutableList<SearchResult<AcceptationId, RuleId>> findAcceptationAndRulesFromText(String queryText, int restrictionStringType, ImmutableIntRange range);
    AcceptationId getStaticAcceptationFromDynamic(AcceptationId dynamicAcceptation);
    ConceptId findRuledConcept(RuleId rule, ConceptId concept);
    ImmutableMap<ConceptId, ConceptId> findRuledConceptsByRule(RuleId rule);
    AcceptationId findRuledAcceptationByAgentAndBaseAcceptation(AgentId agentId, AcceptationId baseAcceptation);
    String readAcceptationMainText(AcceptationId acceptation);
    ImmutableSet<AgentId> findAllAgentsThatIncludedAcceptationInBunch(BunchId bunch, AcceptationId acceptation);
    ImmutableMap<AcceptationId, AcceptationId> getAgentProcessedMap(AgentId agentId);
    MorphologyReaderResult<AcceptationId, RuleId, AgentId> readMorphologiesFromAcceptation(AcceptationId acceptation, AlphabetId preferredAlphabet);
    ImmutableSet<AcceptationId> getAcceptationsInBunchByBunchAndAgent(BunchId bunch, AgentId agent);
    ImmutableSet<BunchId> getBunchSet(BunchSetId setId);
    AcceptationId findRuledAcceptationByRuleAndBaseAcceptation(RuleId rule, AcceptationId baseAcceptation);
}
