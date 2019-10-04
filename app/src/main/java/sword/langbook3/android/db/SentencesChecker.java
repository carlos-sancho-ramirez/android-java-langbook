package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.SentenceSpan;

public interface SentencesChecker extends AcceptationsChecker {
    ImmutableIntValueMap<SentenceSpan> getSentenceSpansWithIds(int symbolArray);
    boolean isSymbolArrayMerelyASentence(int symbolArrayId);
    ImmutableSet<SentenceSpan> getSentenceSpans(int symbolArray);
    ImmutableIntSet findSentenceIdsMatchingMeaning(int symbolArrayId);
}
