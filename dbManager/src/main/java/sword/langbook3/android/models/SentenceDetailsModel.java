package sword.langbook3.android.models;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableSet;

public final class SentenceDetailsModel<AcceptationId> {

    public final int concept;
    public final String text;
    public final ImmutableSet<SentenceSpan<AcceptationId>> spans;
    public final ImmutableIntKeyMap<String> sameMeaningSentences;

    public SentenceDetailsModel(int concept, String text, ImmutableSet<SentenceSpan<AcceptationId>> spans, ImmutableIntKeyMap<String> sameMeaningSentences) {
        this.concept = concept;
        this.text = text;
        this.spans = spans;
        this.sameMeaningSentences = sameMeaningSentences;
    }
}
