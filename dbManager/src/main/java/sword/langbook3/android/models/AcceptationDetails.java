package sword.langbook3.android.models;

import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.ImmutableCorrelation;

public interface AcceptationDetails<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId, AgentId, SentenceId> {

    interface InvolvedAgentResultFlags {
        int target = 1;
        int source = 2;
        int diff = 4;
        int rule = 8;
        int processed = 16;
    }

    ConceptId getConcept();
    IdTextPairResult<LanguageId> getLanguage();
    AcceptationId getOriginalAcceptationId();
    String getOriginalAcceptationText();
    AgentId getAppliedAgentId();
    RuleId getAppliedRuleId();
    AcceptationId getAppliedRuleAcceptationId();
    ImmutableList<CorrelationId> getCorrelationIds();
    ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> getCorrelations();
    ImmutableCorrelation<AlphabetId> getTexts();
    ImmutableMap<AcceptationId, ImmutableSet<AlphabetId>> getAcceptationsSharingTexts();
    ImmutableMap<AcceptationId, String> getAcceptationsSharingTextsDisplayableTexts();
    AcceptationId getBaseConceptAcceptationId();
    String getBaseConceptText();
    ImmutableMap<AcceptationId, String> getDefinitionComplementTexts();
    ImmutableMap<AcceptationId, String> getSubtypes();
    ImmutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> getSynonymsAndTranslations();
    ImmutableList<DynamizableResult<AcceptationId>> getBunchChildren();
    ImmutableList<DynamizableResult<AcceptationId>> getBunchesWhereAcceptationIsIncluded();
    ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> getDerivedAcceptations();
    ImmutableMap<RuleId, String> getRuleTexts();
    ImmutableIntValueMap<AgentId> getInvolvedAgents();
    ImmutableMap<AgentId, RuleId> getAgentRules();
    ImmutableMap<SentenceId, String> getSampleSentences();

    /**
     * Maps languages with the suitable representation text.
     */
    ImmutableMap<LanguageId, String> getLanguageTexts();

    String getTitle(AlphabetId preferredAlphabet);
}
