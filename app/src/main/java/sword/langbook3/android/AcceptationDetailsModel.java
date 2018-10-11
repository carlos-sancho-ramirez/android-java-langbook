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

    /**
     * Maps languages (concepts) with the suitable representation text.
     */
    public final ImmutableIntKeyMap<String> languageTexts;

    public AcceptationDetailsModel(
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
            ImmutableIntKeyMap<String> languageTexts
    ) {
        if (language == null || correlationIds == null || correlations == null ||
                supertypes == null || subtypes == null || synonymsAndTranslations == null ||
                bunchChildren == null || bunchesWhereAcceptationIsIncluded == null ||
                morphologies == null || involvedAgents == null || languageTexts == null) {
            throw new IllegalArgumentException();
        }

        if (correlationIds.anyMatch(id -> !correlations.keySet().contains(id))) {
            throw new IllegalArgumentException();
        }

        if (synonymsAndTranslations.anyMatch(value -> !languageTexts.keySet().contains(value.language))) {
            throw new IllegalArgumentException();
        }

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
    }
}
