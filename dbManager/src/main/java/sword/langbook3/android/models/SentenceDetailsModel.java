package sword.langbook3.android.models;

import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;

public final class SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> {

    public final ConceptId concept;
    public final String text;
    public final ImmutableSet<SentenceSpan<AcceptationId>> spans;
    public final ImmutableMap<SentenceId, String> sameMeaningSentences;

    public SentenceDetailsModel(ConceptId concept, String text, ImmutableSet<SentenceSpan<AcceptationId>> spans, ImmutableMap<SentenceId, String> sameMeaningSentences) {
        this.concept = concept;
        this.text = text;
        this.spans = spans;
        this.sameMeaningSentences = sameMeaningSentences;
    }
}
