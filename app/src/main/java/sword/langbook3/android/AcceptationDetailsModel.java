package sword.langbook3.android;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableList;
import sword.langbook3.android.LangbookReadableDatabase.DynamizableResult;
import sword.langbook3.android.LangbookReadableDatabase.IdentifiableResult;
import sword.langbook3.android.LangbookReadableDatabase.MorphologyResult;
import sword.langbook3.android.LangbookReadableDatabase.SynonymTranslationResult;

public final class AcceptationDetailsModel {
    public final int concept;
    public final IdentifiableResult language;
    public final ImmutableIntList correlationIds;
    public final ImmutableIntKeyMap<ImmutableIntKeyMap<String>> correlations;
    public final ImmutableIntKeyMap<String> supertypes;
    public final ImmutableIntKeyMap<String> subtypes;
    public final ImmutableIntKeyMap<SynonymTranslationResult> synonymsAndTranslations;
    public final ImmutableList<DynamizableResult> bunchChildren;
    public final ImmutableList<DynamizableResult> bunchesWhereAcceptationIsIncluded;
    public final ImmutableList<MorphologyResult> morphologies;
    public final ImmutableIntPairMap involvedAgents;
    public final ImmutableIntKeyMap<String> sampleSentences;

    /**
     * Maps languages (concepts) with the suitable representation text.
     */
    public final ImmutableIntKeyMap<String> languageTexts;

    public AcceptationDetailsModel(
            int concept,
            IdentifiableResult language,
            ImmutableIntList correlationIds,
            ImmutableIntKeyMap<ImmutableIntKeyMap<String>> correlations,
            ImmutableIntKeyMap<String> supertypes,
            ImmutableIntKeyMap<String> subtypes,
            ImmutableIntKeyMap<SynonymTranslationResult> synonymsAndTranslations,
            ImmutableList<DynamizableResult> bunchChildren,
            ImmutableList<DynamizableResult> bunchesWhereAcceptationIsIncluded,
            ImmutableList<MorphologyResult> morphologies,
            ImmutableIntPairMap involvedAgents,
            ImmutableIntKeyMap<String> languageTexts,
            ImmutableIntKeyMap<String> sampleSentences
    ) {
        if (language == null || correlationIds == null || correlations == null ||
                supertypes == null || subtypes == null || synonymsAndTranslations == null ||
                bunchChildren == null || bunchesWhereAcceptationIsIncluded == null ||
                morphologies == null || involvedAgents == null || languageTexts == null ||
                sampleSentences == null) {
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
        this.correlationIds = correlationIds;
        this.correlations = correlations;
        this.supertypes = supertypes;
        this.subtypes = subtypes;
        this.synonymsAndTranslations = synonymsAndTranslations;
        this.bunchChildren = bunchChildren;
        this.bunchesWhereAcceptationIsIncluded = bunchesWhereAcceptationIsIncluded;
        this.morphologies = morphologies;
        this.involvedAgents = involvedAgents;
        this.languageTexts = languageTexts;
        this.sampleSentences = sampleSentences;
    }

    public String getTitle(int preferredAlphabet) {
        StringBuilder sb = new StringBuilder();
        for (int correlationId : correlationIds) {
            final ImmutableIntKeyMap<String> correlation = correlations.get(correlationId);
            final String preferredText = correlation.get(preferredAlphabet, null);
            sb.append((preferredText != null)? preferredText : correlation.valueAt(0));
        }

        return sb.toString();
    }
}
