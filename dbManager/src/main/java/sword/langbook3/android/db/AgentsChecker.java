package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.MutableIntKeyMap;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.DisplayableItem;
import sword.langbook3.android.models.MorphologyReaderResult;
import sword.langbook3.android.models.SearchResult;
import sword.langbook3.android.models.TableCellReference;
import sword.langbook3.android.models.TableCellValue;

public interface AgentsChecker extends BunchesChecker {
    ImmutableIntKeyMap<String> readAllMatchingBunches(ImmutableIntKeyMap<String> texts, int preferredAlphabet);
    MutableIntKeyMap<String> readCorrelationArrayTexts(int correlationArrayId);
    ImmutableIntKeyMap<String> readAllRules(int preferredAlphabet);
    ImmutableIntSet getAgentIds();
    ImmutableList<SearchResult> findAcceptationFromText(String queryText, int restrictionStringType);
    AgentRegister getAgentRegister(int agentId);
    AgentDetails getAgentDetails(int agentId);
    ImmutableList<DisplayableItem> readBunchSetAcceptationsAndTexts(int bunchSet, int preferredAlphabet);
    ImmutableList<SearchResult> findAcceptationAndRulesFromText(String queryText, int restrictionStringType);
    ImmutableMap<TableCellReference, TableCellValue> readTableContent(int dynamicAcceptation, int preferredAlphabet);
    Integer getStaticAcceptationFromDynamic(int dynamicAcceptation);
    Integer findRuledConcept(int rule, int concept);
    ImmutableIntPairMap findRuledConceptsByRule(int rule);
    Integer findRuledAcceptationByAgentAndBaseAcceptation(int agentId, int baseAcceptation);
    String readAcceptationMainText(int acceptation);
    ImmutableIntSet findAllAgentsThatIncludedAcceptationInBunch(int bunch, int acceptation);
    ImmutableIntPairMap getAgentProcessedMap(int agentId);
    MorphologyReaderResult readMorphologiesFromAcceptation(int acceptation, int preferredAlphabet);
    ImmutableIntSet getAcceptationsInBunchByBunchAndAgent(int bunch, int agent);
    ImmutableIntSet getBunchSet(int setId);
}
