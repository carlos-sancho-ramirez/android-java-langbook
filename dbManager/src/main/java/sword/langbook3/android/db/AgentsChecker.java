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
import sword.langbook3.android.models.TableCellReference;
import sword.langbook3.android.models.TableCellValue;

public interface AgentsChecker<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> extends BunchesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> {
    ImmutableIntKeyMap<String> readAllMatchingBunches(ImmutableCorrelation<AlphabetId> texts, AlphabetId preferredAlphabet);
    MutableCorrelation<AlphabetId> readCorrelationArrayTexts(CorrelationArrayId correlationArrayId);
    ImmutableIntKeyMap<String> readAllRules(AlphabetId preferredAlphabet);
    ImmutableIntSet getAgentIds();
    ImmutableList<SearchResult<AcceptationId>> findAcceptationFromText(String queryText, int restrictionStringType, ImmutableIntRange range);
    AgentRegister<CorrelationId> getAgentRegister(int agentId);
    AgentDetails<AlphabetId> getAgentDetails(int agentId);
    ImmutableList<DisplayableItem<AcceptationId>> readBunchSetAcceptationsAndTexts(int bunchSet, AlphabetId preferredAlphabet);
    ImmutableList<SearchResult<AcceptationId>> findAcceptationAndRulesFromText(String queryText, int restrictionStringType, ImmutableIntRange range);
    ImmutableMap<TableCellReference, TableCellValue> readTableContent(int dynamicAcceptation, AlphabetId preferredAlphabet);
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
}
