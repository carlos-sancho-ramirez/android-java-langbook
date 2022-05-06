package sword.langbook3.android.models;

import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.ImmutableCorrelation;

public final class AcceptationDetailsModel<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId, AgentId, SentenceId> implements AcceptationDetails<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId, AgentId, SentenceId> {

    private final ConceptId _concept;
    private final IdTextPairResult<LanguageId> _language;
    private final AcceptationId _originalAcceptationId;
    private final String _originalAcceptationText;
    private final AgentId _appliedAgentId;
    private final RuleId _appliedRuleId;
    private final AcceptationId _appliedRuleAcceptationId;
    private final ImmutableList<CorrelationId> _correlationIds;
    private final ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> _correlations;
    private final ImmutableCorrelation<AlphabetId> _texts;
    private final ImmutableMap<AcceptationId, ImmutableSet<AlphabetId>> _acceptationsSharingTexts;
    private final ImmutableMap<AcceptationId, String> _acceptationsSharingTextsDisplayableTexts;
    private final AcceptationId _baseConceptAcceptationId;
    private final String _baseConceptText;
    private final ImmutableMap<AcceptationId, String> _definitionComplementTexts;
    private final ImmutableMap<AcceptationId, String> _subtypes;
    private final ImmutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> _synonymsAndTranslations;
    private final ImmutableList<DynamizableResult<AcceptationId>> _bunchChildren;
    private final ImmutableList<DynamizableResult<AcceptationId>> _bunchesWhereAcceptationIsIncluded;
    private final ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> _derivedAcceptations;
    private final ImmutableMap<RuleId, String> _ruleTexts;
    private final ImmutableIntValueMap<AgentId> _involvedAgents;
    private final ImmutableMap<AgentId, RuleId> _agentRules;
    private final ImmutableMap<SentenceId, String> _sampleSentences;

    /**
     * Maps languages with the suitable representation text.
     */
    private final ImmutableMap<LanguageId, String> _languageTexts;

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
            ImmutableMap<SentenceId, String> sampleSentences
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

        _concept = concept;
        _language = language;
        _originalAcceptationId = originalAcceptationId;
        _originalAcceptationText = originalAcceptationText;
        _appliedAgentId = appliedAgentId;
        _appliedRuleId = appliedRuleId;
        _appliedRuleAcceptationId = appliedRuleAcceptationId;
        _correlationIds = correlationIds;
        _correlations = correlations;
        _texts = texts;
        _acceptationsSharingTexts = acceptationsSharingTexts;
        _acceptationsSharingTextsDisplayableTexts = acceptationsSharingTextsDisplayableTexts;
        _baseConceptAcceptationId = baseConceptAcceptationId;
        _baseConceptText = baseConceptText;
        _definitionComplementTexts = definitionComplementTexts;
        _subtypes = subtypes;
        _synonymsAndTranslations = synonymsAndTranslations;
        _bunchChildren = bunchChildren;
        _bunchesWhereAcceptationIsIncluded = bunchesWhereAcceptationIsIncluded;
        _derivedAcceptations = derivedAcceptations;
        _ruleTexts = ruleTexts;
        _involvedAgents = involvedAgents;
        _agentRules = agentRules;
        _languageTexts = languageTexts;
        _sampleSentences = sampleSentences;
    }

    @Override
    public ConceptId getConcept() {
        return _concept;
    }

    @Override
    public IdTextPairResult<LanguageId> getLanguage() {
        return _language;
    }

    @Override
    public AcceptationId getOriginalAcceptationId() {
        return _originalAcceptationId;
    }

    @Override
    public String getOriginalAcceptationText() {
        return _originalAcceptationText;
    }

    @Override
    public AgentId getAppliedAgentId() {
        return _appliedAgentId;
    }

    @Override
    public RuleId getAppliedRuleId() {
        return _appliedRuleId;
    }

    @Override
    public AcceptationId getAppliedRuleAcceptationId() {
        return _appliedRuleAcceptationId;
    }

    @Override
    public ImmutableList<CorrelationId> getCorrelationIds() {
        return _correlationIds;
    }

    @Override
    public ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> getCorrelations() {
        return _correlations;
    }

    @Override
    public ImmutableCorrelation<AlphabetId> getTexts() {
        return _texts;
    }

    @Override
    public ImmutableMap<AcceptationId, ImmutableSet<AlphabetId>> getAcceptationsSharingTexts() {
        return _acceptationsSharingTexts;
    }

    @Override
    public ImmutableMap<AcceptationId, String> getAcceptationsSharingTextsDisplayableTexts() {
        return _acceptationsSharingTextsDisplayableTexts;
    }

    @Override
    public AcceptationId getBaseConceptAcceptationId() {
        return _baseConceptAcceptationId;
    }

    @Override
    public String getBaseConceptText() {
        return _baseConceptText;
    }

    @Override
    public ImmutableMap<AcceptationId, String> getDefinitionComplementTexts() {
        return _definitionComplementTexts;
    }

    @Override
    public ImmutableMap<AcceptationId, String> getSubtypes() {
        return _subtypes;
    }

    @Override
    public ImmutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> getSynonymsAndTranslations() {
        return _synonymsAndTranslations;
    }

    @Override
    public ImmutableList<DynamizableResult<AcceptationId>> getBunchChildren() {
        return _bunchChildren;
    }

    @Override
    public ImmutableList<DynamizableResult<AcceptationId>> getBunchesWhereAcceptationIsIncluded() {
        return _bunchesWhereAcceptationIsIncluded;
    }

    @Override
    public ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> getDerivedAcceptations() {
        return _derivedAcceptations;
    }

    @Override
    public ImmutableMap<RuleId, String> getRuleTexts() {
        return _ruleTexts;
    }

    @Override
    public ImmutableIntValueMap<AgentId> getInvolvedAgents() {
        return _involvedAgents;
    }

    @Override
    public ImmutableMap<AgentId, RuleId> getAgentRules() {
        return _agentRules;
    }

    @Override
    public ImmutableMap<SentenceId, String> getSampleSentences() {
        return _sampleSentences;
    }

    @Override
    public ImmutableMap<LanguageId, String> getLanguageTexts() {
        return _languageTexts;
    }

    @Override
    public String getTitle(AlphabetId preferredAlphabet) {
        StringBuilder sb = new StringBuilder();
        for (CorrelationId correlationId : _correlationIds) {
            final ImmutableMap<AlphabetId, String> correlation = _correlations.get(correlationId);
            final String preferredText = correlation.get(preferredAlphabet, null);
            sb.append((preferredText != null)? preferredText : correlation.valueAt(0));
        }

        return sb.toString();
    }
}
