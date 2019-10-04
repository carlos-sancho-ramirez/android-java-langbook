package sword.langbook3.android.models;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableSet;

public class SentenceDetailsModel {

    public final String text;
    public final ImmutableSet<SentenceSpan> spans;
    public final ImmutableIntKeyMap<String> sameMeaningSentences;

    public SentenceDetailsModel(String text, ImmutableSet<SentenceSpan> spans, ImmutableIntKeyMap<String> sameMeaningSentences) {
        this.text = text;
        this.spans = spans;
        this.sameMeaningSentences = sameMeaningSentences;
    }
}
