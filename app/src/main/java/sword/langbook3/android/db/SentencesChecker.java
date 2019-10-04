package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;

public interface SentencesChecker extends AcceptationsChecker {
    boolean isSymbolArrayMerelyASentence(int symbolArrayId);
    ImmutableSet<SentenceSpan> getSentenceSpans(int symbolArray);

    ImmutableIntKeyMap<String> getSampleSentences(int staticAcceptation);
    SentenceDetailsModel getSentenceDetails(int sentenceId);
}
