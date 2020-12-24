package sword.langbook3.android.models;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.AlphabetId;

public final class AcceptationDetailsModel {

    public interface InvolvedAgentResultFlags {
        int target = 1;
        int source = 2;
        int diff = 4;
        int rule = 8;
        int processed = 16;
    }

    public final int concept;
    public final IdentifiableResult language;
    public final int originalAcceptationId;
    public final String originalAcceptationText;
    public final int appliedAgentId;
    public final int appliedRuleId;
    public final int appliedRuleAcceptationId;
    public final ImmutableIntList correlationIds;
    public final ImmutableIntKeyMap<ImmutableMap<AlphabetId, String>> correlations;
    public final ImmutableMap<AlphabetId, String> texts;
    public final ImmutableIntKeyMap<ImmutableSet<AlphabetId>> acceptationsSharingTexts;
    public final ImmutableIntKeyMap<String> acceptationsSharingTextsDisplayableTexts;
    public final int baseConceptAcceptationId;
    public final String baseConceptText;
    public final ImmutableIntKeyMap<String> definitionComplementTexts;
    public final ImmutableIntKeyMap<String> subtypes;
    public final ImmutableIntKeyMap<SynonymTranslationResult> synonymsAndTranslations;
    public final ImmutableList<DynamizableResult> bunchChildren;
    public final ImmutableList<DynamizableResult> bunchesWhereAcceptationIsIncluded;
    public final ImmutableIntKeyMap<DerivedAcceptationResult> derivedAcceptations;
    public final ImmutableIntKeyMap<String> ruleTexts;
    public final ImmutableIntPairMap involvedAgents;
    public final ImmutableIntPairMap agentRules;
    public final ImmutableIntKeyMap<String> sampleSentences;

    /**
     * Maps languages (concepts) with the suitable representation text.
     */
    public final ImmutableIntKeyMap<String> languageTexts;

    public AcceptationDetailsModel(
            int concept,
            IdentifiableResult language,
            int originalAcceptationId,
            String originalAcceptationText,
            int appliedAgentId,
            int appliedRuleId,
            int appliedRuleAcceptationId,
            ImmutableIntList correlationIds,
            ImmutableIntKeyMap<ImmutableMap<AlphabetId, String>> correlations,
            ImmutableMap<AlphabetId, String> texts,
            ImmutableIntKeyMap<ImmutableSet<AlphabetId>> acceptationsSharingTexts,
            ImmutableIntKeyMap<String> acceptationsSharingTextsDisplayableTexts,
            int baseConceptAcceptationId,
            String baseConceptText,
            ImmutableIntKeyMap<String> definitionComplementTexts,
            ImmutableIntKeyMap<String> subtypes,
            ImmutableIntKeyMap<SynonymTranslationResult> synonymsAndTranslations,
            ImmutableList<DynamizableResult> bunchChildren,
            ImmutableList<DynamizableResult> bunchesWhereAcceptationIsIncluded,
            ImmutableIntKeyMap<DerivedAcceptationResult> derivedAcceptations,
            ImmutableIntKeyMap<String> ruleTexts,
            ImmutableIntPairMap involvedAgents,
            ImmutableIntPairMap agentRules,
            ImmutableIntKeyMap<String> languageTexts,
            ImmutableIntKeyMap<String> sampleSentences
    ) {
        if (language == null || originalAcceptationId != 0 && (originalAcceptationText == null ||
                appliedAgentId == 0 || appliedRuleId == 0 || appliedRuleAcceptationId == 0) ||
                correlationIds == null || correlations == null ||
                texts == null || acceptationsSharingTexts == null ||
                acceptationsSharingTextsDisplayableTexts == null || definitionComplementTexts == null ||
                subtypes == null || synonymsAndTranslations == null ||
                bunchChildren == null || bunchesWhereAcceptationIsIncluded == null ||
                derivedAcceptations == null || ruleTexts == null ||
                involvedAgents == null || agentRules == null || languageTexts == null || sampleSentences == null) {
            throw new IllegalArgumentException();
        }

        if (appliedRuleId != 0 && ruleTexts.get(appliedRuleId, null) == null) {
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
    }

    public String getTitle(AlphabetId preferredAlphabet) {
        StringBuilder sb = new StringBuilder();
        for (int correlationId : correlationIds) {
            final ImmutableMap<AlphabetId, String> correlation = correlations.get(correlationId);
            final String preferredText = correlation.get(preferredAlphabet, null);
            sb.append((preferredText != null)? preferredText : correlation.valueAt(0));
        }

        return sb.toString();
    }
}
