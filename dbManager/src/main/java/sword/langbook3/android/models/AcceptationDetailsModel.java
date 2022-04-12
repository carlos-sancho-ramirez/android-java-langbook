package sword.langbook3.android.models;

import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.ImmutableCorrelation;

public final class AcceptationDetailsModel<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId, AgentId, SentenceId> {

    public interface InvolvedAgentResultFlags {
        int target = 1;
        int source = 2;
        int diff = 4;
        int rule = 8;
        int processed = 16;
    }

    public final ConceptId concept;
    public final IdTextPairResult<LanguageId> language;
    public final AcceptationId originalAcceptationId;
    public final String originalAcceptationText;
    public final AgentId appliedAgentId;
    public final RuleId appliedRuleId;
    public final AcceptationId appliedRuleAcceptationId;
    public final ImmutableList<CorrelationId> correlationIds;
    public final ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlations;
    public final ImmutableCorrelation<AlphabetId> texts;
    public final ImmutableMap<AcceptationId, ImmutableSet<AlphabetId>> acceptationsSharingTexts;
    public final ImmutableMap<AcceptationId, String> acceptationsSharingTextsDisplayableTexts;
    public final AcceptationId baseConceptAcceptationId;
    public final String baseConceptText;
    public final ImmutableMap<AcceptationId, String> definitionComplementTexts;
    public final ImmutableMap<AcceptationId, String> subtypes;
    public final ImmutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> synonymsAndTranslations;
    public final ImmutableList<DynamizableResult<AcceptationId>> bunchChildren;
    public final ImmutableList<DynamizableResult<AcceptationId>> bunchesWhereAcceptationIsIncluded;
    public final ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> derivedAcceptations;
    public final ImmutableMap<RuleId, String> ruleTexts;
    public final ImmutableIntValueMap<AgentId> involvedAgents;
    public final ImmutableMap<AgentId, RuleId> agentRules;
    public final ImmutableMap<SentenceId, String> sampleSentences;
    public final CharacterCompositionDefinitionRegister characterCompositionDefinitionRegister;

    /**
     * Maps languages with the suitable representation text.
     */
    public final ImmutableMap<LanguageId, String> languageTexts;

    public AcceptationDetailsModel(
            ConceptId concept,
            IdTextPairResult<LanguageId> language,
            AcceptationId originalAcceptationId,
            String originalAcceptationText,
            AgentId appliedAgentId,
            RuleId appliedRuleId,
            AcceptationId appliedRuleAcceptationId,
            ImmutableList<CorrelationId> correlationIds,
            ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlations,
            ImmutableCorrelation<AlphabetId> texts,
            ImmutableMap<AcceptationId, ImmutableSet<AlphabetId>> acceptationsSharingTexts,
            ImmutableMap<AcceptationId, String> acceptationsSharingTextsDisplayableTexts,
            AcceptationId baseConceptAcceptationId,
            String baseConceptText,
            ImmutableMap<AcceptationId, String> definitionComplementTexts,
            ImmutableMap<AcceptationId, String> subtypes,
            ImmutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> synonymsAndTranslations,
            ImmutableList<DynamizableResult<AcceptationId>> bunchChildren,
            ImmutableList<DynamizableResult<AcceptationId>> bunchesWhereAcceptationIsIncluded,
            ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> derivedAcceptations,
            ImmutableMap<RuleId, String> ruleTexts,
            ImmutableIntValueMap<AgentId> involvedAgents,
            ImmutableMap<AgentId, RuleId> agentRules,
            ImmutableMap<LanguageId, String> languageTexts,
            ImmutableMap<SentenceId, String> sampleSentences,
            CharacterCompositionDefinitionRegister characterCompositionDefinitionRegister
    ) {
        if (concept == null || language == null || originalAcceptationId != null && (originalAcceptationText == null ||
                appliedAgentId == null || appliedRuleId == null || appliedRuleAcceptationId == null) ||
                correlationIds == null || correlations == null ||
                texts == null || acceptationsSharingTexts == null ||
                acceptationsSharingTextsDisplayableTexts == null || definitionComplementTexts == null ||
                subtypes == null || synonymsAndTranslations == null ||
                bunchChildren == null || bunchesWhereAcceptationIsIncluded == null ||
                derivedAcceptations == null || ruleTexts == null ||
                involvedAgents == null || agentRules == null || languageTexts == null || sampleSentences == null) {
            throw new IllegalArgumentException();
        }

        if (appliedRuleId != null && ruleTexts.get(appliedRuleId, null) == null) {
            throw new IllegalArgumentException();
        }

        if (correlationIds.anyMatch(id -> !correlations.keySet().contains(id))) {
            throw new IllegalArgumentException();
        }

        if (synonymsAndTranslations.anyMatch(value -> !languageTexts.keySet().contains(value.language))) {
            throw new IllegalArgumentException();
        }

        this.concept = concept;
        this.language = language;
        this.originalAcceptationId = originalAcceptationId;
        this.originalAcceptationText = originalAcceptationText;
        this.appliedAgentId = appliedAgentId;
        this.appliedRuleId = appliedRuleId;
        this.appliedRuleAcceptationId = appliedRuleAcceptationId;
        this.correlationIds = correlationIds;
        this.correlations = correlations;
        this.texts = texts;
        this.acceptationsSharingTexts = acceptationsSharingTexts;
        this.acceptationsSharingTextsDisplayableTexts = acceptationsSharingTextsDisplayableTexts;
        this.baseConceptAcceptationId = baseConceptAcceptationId;
        this.baseConceptText = baseConceptText;
        this.definitionComplementTexts = definitionComplementTexts;
        this.subtypes = subtypes;
        this.synonymsAndTranslations = synonymsAndTranslations;
        this.bunchChildren = bunchChildren;
        this.bunchesWhereAcceptationIsIncluded = bunchesWhereAcceptationIsIncluded;
        this.derivedAcceptations = derivedAcceptations;
        this.ruleTexts = ruleTexts;
        this.involvedAgents = involvedAgents;
        this.agentRules = agentRules;
        this.languageTexts = languageTexts;
        this.sampleSentences = sampleSentences;
        this.characterCompositionDefinitionRegister = characterCompositionDefinitionRegister;
    }

    public String getTitle(AlphabetId preferredAlphabet) {
        StringBuilder sb = new StringBuilder();
        for (CorrelationId correlationId : correlationIds) {
            final ImmutableMap<AlphabetId, String> correlation = correlations.get(correlationId);
            final String preferredText = correlation.get(preferredAlphabet, null);
            sb.append((preferredText != null)? preferredText : correlation.valueAt(0));
        }

        return sb.toString();
    }
}
